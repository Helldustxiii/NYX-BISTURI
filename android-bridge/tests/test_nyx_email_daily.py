import datetime as dt
import importlib.machinery
import importlib.util
import json
import os
import pathlib
import tempfile
import unittest
import urllib.request
from unittest.mock import patch

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "tools" / "nyx-email-daily"
LOADER = importlib.machinery.SourceFileLoader("nyx_email_daily", str(SCRIPT))
SPEC = importlib.util.spec_from_loader(LOADER.name, LOADER)
EMAIL = importlib.util.module_from_spec(SPEC)
LOADER.exec_module(EMAIL)


class PrivateFileTests(unittest.TestCase):
    def test_private_file_is_created_with_restricted_permissions(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = pathlib.Path(tmp) / "private" / "secret.txt"
            EMAIL.write_private_file(target, "secret\n")
            self.assertEqual(target.read_text(), "secret\n")
            if os.name == "posix":
                self.assertEqual(target.stat().st_mode & 0o777, 0o600)
                self.assertEqual(target.parent.stat().st_mode & 0o777, 0o700)

    def test_open_permissions_are_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = pathlib.Path(tmp) / "config.json"
            target.write_text('{"sender":"a@example.com"}')
            os.chmod(target, 0o644)
            with self.assertRaisesRegex(EMAIL.EmailJobError, "permissions_too_open"):
                EMAIL.read_json_private(target)

    def test_job_lock_rejects_a_second_concurrent_run(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / "job.lock"
            first = EMAIL.acquire_job_lock(path)
            try:
                with self.assertRaisesRegex(EMAIL.EmailJobError, "already_running"):
                    EMAIL.acquire_job_lock(path)
            finally:
                first.close()

    def test_existing_private_file_is_not_silently_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = pathlib.Path(tmp) / "secret.txt"
            EMAIL.write_private_file(target, "first")
            with self.assertRaisesRegex(EMAIL.EmailJobError, "file_already_exists"):
                EMAIL.write_private_file(target, "second")
            self.assertEqual(target.read_text(), "first")


class ConfigurationTests(unittest.TestCase):
    def test_only_exact_loopback_qwen_endpoint_is_allowed(self):
        self.assertEqual(
            EMAIL.validate_local_base_url("http://127.0.0.1:8080/v1"),
            "http://127.0.0.1:8080/v1",
        )
        for url in (
            "https://api.example.com/v1",
            "http://192.168.1.5:8080/v1",
            "http://localhost:8080/v1",
            "http://[::1]:8080/v1",
            "http://127.0.0.1:8080/v1?token=x",
            "http://user:pass@127.0.0.1:8080/v1",
        ):
            with self.subTest(url=url):
                with self.assertRaisesRegex(EMAIL.EmailJobError, "qwen_url_must_be_local"):
                    EMAIL.validate_local_base_url(url)

    def test_external_redirect_from_local_qwen_is_rejected(self):
        handler = EMAIL.LoopbackRedirectHandler()
        request = urllib.request.Request("http://127.0.0.1:8080/v1/models")
        with self.assertRaisesRegex(EMAIL.EmailJobError, "qwen_redirect_not_local"):
            handler.redirect_request(
                request, None, 302, "Found", {"Location": "https://example.com/collect"},
                "https://example.com/collect",
            )

    def test_local_redirect_is_allowed(self):
        handler = EMAIL.LoopbackRedirectHandler()
        request = urllib.request.Request("http://127.0.0.1:8080/health")
        redirected = handler.redirect_request(
            request, None, 302, "Found", {"Location": "http://127.0.0.1:8080/status"},
            "http://127.0.0.1:8080/status",
        )
        self.assertEqual(redirected.full_url, "http://127.0.0.1:8080/status")

    def test_config_rejects_unapproved_smtp_target(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / "config.json"
            EMAIL.write_private_file(path, json.dumps({
                "sender": "nyx@example.com",
                "recipient": "user@example.com",
                "qwen_base_url": EMAIL.DEFAULT_BASE_URL,
                "smtp_host": "mail.example.com",
                "smtp_port": 465,
                "profile_file": str(pathlib.Path(tmp) / "profile.md"),
            }))
            with self.assertRaisesRegex(EMAIL.EmailJobError, "smtp_target_not_allowlisted"):
                EMAIL.load_config(path)

    def test_config_loads_only_private_file_and_normalizes_defaults(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / "config.json"
            profile = pathlib.Path(tmp) / "profile.md"
            EMAIL.write_private_file(path, json.dumps({
                "sender": "nyx@example.com",
                "recipient": "user@example.com",
                "profile_file": str(profile),
            }))
            config = EMAIL.load_config(path)
            self.assertEqual(config["qwen_base_url"], EMAIL.DEFAULT_BASE_URL)
            self.assertEqual(config["smtp_host"], "smtp.gmail.com")
            self.assertEqual(config["smtp_port"], 465)
            self.assertEqual(config["sender"], "nyx@example.com")


class DraftTests(unittest.TestCase):
    def test_parse_draft_extracts_subject_and_body(self):
        subject, body = EMAIL.parse_draft(
            "Asunto: Una idea distinta\n\nPrimera línea.\nSegunda línea.",
            dt.date(2026, 11, 11),
        )
        self.assertEqual(subject, "P.D. privada · 2026-11-11")
        self.assertEqual(body, "Primera línea.\nSegunda línea.")

    def test_parse_draft_uses_nonrevealing_default_subject(self):
        subject, body = EMAIL.parse_draft(
            "Una reflexión sin asunto.", dt.date(2026, 11, 11)
        )
        self.assertEqual(subject, "P.D. privada · 2026-11-11")
        self.assertEqual(body, "Una reflexión sin asunto.")

    def test_qwen_request_uses_only_local_model_and_private_profile(self):
        responses = [
            {"status": "ok"},
            {"data": [{"id": "local-qwen"}]},
            {"choices": [{"message": {"content": "Texto original."}}]},
        ]
        calls = []

        def fake_http(url, payload=None, timeout=5.0):
            calls.append((url, payload, timeout))
            return responses.pop(0)

        config = {
            "qwen_base_url": EMAIL.DEFAULT_BASE_URL,
            "timezone": "Europe/Madrid",
        }
        subject, body, digest = EMAIL.generate_draft(
            config, "Perfil confidencial local", dt.date(2026, 11, 11),
            http=fake_http,
        )
        self.assertEqual(subject, "P.D. privada · 2026-11-11")
        self.assertEqual(body, "Texto original.")
        self.assertEqual(len(digest), 64)
        request = calls[2][1]
        self.assertEqual(request["model"], "local-qwen")
        self.assertEqual(request["messages"][0]["content"], "Perfil confidencial local")
        self.assertEqual(
            request["chat_template_kwargs"], {"enable_thinking": False}
        )

    def test_duplicate_fingerprint_is_regenerated_and_never_accepted(self):
        responses = [
            {"status": "ok"}, {"data": [{"id": "qwen"}]},
            {"choices": [{"message": {"content": "Mismo texto."}}]},
            {"choices": [{"message": {"content": "Mismo texto."}}]},
            {"choices": [{"message": {"content": "Texto distinto."}}]},
        ]

        def fake_http(url, payload=None, timeout=5.0):
            return responses.pop(0)

        old_hash = EMAIL.fingerprint(
            "P.D. privada · 2026-11-11", "Mismo texto."
        )
        result = EMAIL.generate_draft(
            {"qwen_base_url": EMAIL.DEFAULT_BASE_URL}, "Perfil",
            dt.date(2026, 11, 11), http=fake_http,
            previous_hashes=[old_hash],
        )
        self.assertEqual(result[1], "Texto distinto.")

    def test_fingerprint_ignores_the_date_in_the_subject(self):
        first = EMAIL.fingerprint(
            "P.D. privada · 2026-11-11", "La misma reflexión."
        )
        next_day = EMAIL.fingerprint(
            "P.D. privada · 2026-11-12", "La misma reflexión."
        )
        self.assertEqual(first, next_day)

    def test_sent_state_persists_only_dates_and_hashes(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / "state.json"
            previous = {"sent_dates": [], "fingerprints": []}
            EMAIL.save_sent_state(
                path, dt.date(2026, 11, 11), "a" * 64, previous
            )
            state = EMAIL.load_state(path)
            self.assertEqual(state["sent_dates"], ["2026-11-11"])
            self.assertEqual(state["fingerprints"], ["a" * 64])
            self.assertNotIn("body", path.read_text())
            if os.name == "posix":
                self.assertEqual(path.stat().st_mode & 0o777, 0o600)


class SecretTests(unittest.TestCase):
    def test_secret_is_loaded_from_a_private_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / "smtp.password"
            EMAIL.write_private_file(path, "local-test-secret\\n")
            self.assertEqual(EMAIL.load_secret(path), "local-test-secret")

    def test_secret_file_with_open_permissions_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / "smtp.password"
            path.write_text("local-test-secret")
            os.chmod(path, 0o644)
            with self.assertRaisesRegex(EMAIL.EmailJobError, "permissions_too_open"):
                EMAIL.load_secret(path)


class SmtpTests(unittest.TestCase):
    def test_send_uses_tls_and_does_not_return_or_log_message_body(self):
        captured = {}

        class FakeSMTP:
            def __init__(self, host, port, timeout, context):
                captured["connection"] = (host, port, timeout, context)

            def __enter__(self):
                return self

            def __exit__(self, *args):
                return False

            def login(self, username, password):
                captured["login"] = (username, password)

            def send_message(self, message, from_addr, to_addrs):
                captured["message"] = message
                captured["route"] = (from_addr, to_addrs)
                return {}

        config = {
            "sender": "nyx@example.com",
            "recipient": "user@example.com",
            "smtp_host": "smtp.gmail.com",
            "smtp_port": 465,
        }
        EMAIL.send_email(
            config, "not-a-real-secret", "Test", "Private body",
            smtp_factory=FakeSMTP,
        )
        self.assertEqual(
            captured["connection"][:2], ("smtp.gmail.com", 465)
        )
        self.assertEqual(
            captured["login"], ("nyx@example.com", "not-a-real-secret")
        )
        self.assertEqual(
            captured["message"].get_content().strip(), "Private body"
        )
        self.assertEqual(
            captured["route"], ("nyx@example.com", ["user@example.com"])
        )


if __name__ == "__main__":
    unittest.main(verbosity=2)
