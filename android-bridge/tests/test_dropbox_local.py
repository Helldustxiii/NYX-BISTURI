import base64
import hashlib
import importlib.util
import os
import stat
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path
from urllib.parse import parse_qs, urlparse
from unittest.mock import patch

MODULE = Path(__file__).resolve().parents[1] / "tools" / "connectors" / "dropbox_local.py"
SPEC = importlib.util.spec_from_file_location("dropbox_local", MODULE)
dropbox = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(dropbox)
TOOLS = MODULE.parents[1]
sys.path.insert(0, str(TOOLS))


class DropboxLocalTests(unittest.TestCase):
    def test_pkce_pair_uses_rfc7636_s256(self):
        verifier, challenge = dropbox._pkce_pair()
        self.assertGreaterEqual(len(verifier), 43)
        self.assertLessEqual(len(verifier), 128)
        expected = base64.urlsafe_b64encode(
            hashlib.sha256(verifier.encode("ascii")).digest()
        ).rstrip(b"=").decode("ascii")
        self.assertEqual(challenge, expected)
        self.assertNotIn("=", challenge)

    def test_authorize_requests_offline_read_only_pkce_without_secret(self):
        output = StringIO()
        with patch.dict(os.environ, {"NYX_DROPBOX_APP_KEY": "public-app-key"}), \
             patch("builtins.input", return_value="one-time-code"), \
             patch.object(dropbox, "_http_json", return_value={
                 "refresh_token": "refresh-test",
                 "access_token": "access-test",
                 "expires_in": 14400,
                 "scope": "files.metadata.read files.content.read",
             }) as http, \
             patch.object(dropbox, "_secure_write") as save, \
             redirect_stdout(output):
            result = dropbox.authorize()

        self.assertIn("Dropbox autorizado", result)
        auth_url = output.getvalue().splitlines()[1]
        query = parse_qs(urlparse(auth_url).query)
        self.assertEqual(query["client_id"], ["public-app-key"])
        self.assertEqual(query["response_type"], ["code"])
        self.assertEqual(query["token_access_type"], ["offline"])
        self.assertEqual(query["code_challenge_method"], ["S256"])
        self.assertEqual(
            set(query["scope"][0].split()),
            {"files.metadata.read", "files.content.read"},
        )
        form = http.call_args.kwargs["form"]
        self.assertEqual(form["client_id"], "public-app-key")
        self.assertEqual(form["code"], "one-time-code")
        self.assertEqual(form["grant_type"], "authorization_code")
        self.assertEqual(
            query["code_challenge"][0],
            base64.urlsafe_b64encode(
                hashlib.sha256(form["code_verifier"].encode("ascii")).digest()
            ).rstrip(b"=").decode("ascii"),
        )
        self.assertNotIn("client_secret", form)
        saved = save.call_args.args[1]
        self.assertEqual(saved["refresh_token"], "refresh-test")

    def test_secure_write_uses_private_permissions(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / ".config" / "nyx" / "dropbox.json"
            with patch.object(dropbox, "token_path", return_value=path):
                dropbox._secure_write(path, {"refresh_token": "test"})
            self.assertEqual(stat.S_IMODE(path.stat().st_mode), 0o600)
            self.assertEqual(stat.S_IMODE(path.parent.stat().st_mode), 0o700)

    def test_load_config_rejects_permissive_token_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "dropbox.json"
            path.write_text('{"app_key":"key","refresh_token":"token"}', encoding="utf-8")
            path.chmod(0o644)
            with patch.object(dropbox, "token_path", return_value=path):
                with self.assertRaisesRegex(RuntimeError, "permissions_too_open"):
                    dropbox._load_config()

    def test_image_signatures(self):
        self.assertTrue(dropbox._image_signature(b"\x89PNG\r\n\x1a\nrest"))
        self.assertTrue(dropbox._image_signature(b"\xff\xd8\xffrest"))
        self.assertTrue(dropbox._image_signature(b"RIFFxxxxWEBPrest"))
        self.assertFalse(dropbox._image_signature(b"not an image"))

    def test_image_mime_matches_detected_signature(self):
        self.assertEqual(dropbox._image_mime(b"\x89PNG\r\n\x1a\nrest"), "image/png")
        self.assertEqual(dropbox._image_mime(b"\xff\xd8\xffrest"), "image/jpeg")
        self.assertEqual(dropbox._image_mime(b"RIFFxxxxWEBPrest"), "image/webp")
        self.assertIsNone(dropbox._image_mime(b"not an image"))

    def test_download_rejects_relative_path(self):
        with self.assertRaisesRegex(ValueError, "absolute_path"):
            dropbox.download_image("relative/file.png")

    def test_download_rejects_non_image_extension(self):
        with self.assertRaisesRegex(ValueError, "extension"):
            dropbox.download_image("/folder/file.pdf")

    def test_registry_action_is_read_only(self):
        from connectors.registry import get_connector
        spec = get_connector("dropbox")
        self.assertEqual(spec["transport"], "local")
        self.assertEqual(set(spec["actions"]), {"status", "list", "download"})


if __name__ == "__main__":
    unittest.main()
