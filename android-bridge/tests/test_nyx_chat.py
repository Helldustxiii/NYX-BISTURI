import importlib.machinery
import importlib.util
import pathlib
import unittest
import sys
from unittest.mock import patch

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "tools" / "nyx-chat"
LOADER = importlib.machinery.SourceFileLoader("nyx_chat", str(SCRIPT))
SPEC = importlib.util.spec_from_loader(LOADER.name, LOADER)
NYX_CHAT = importlib.util.module_from_spec(SPEC)
LOADER.exec_module(NYX_CHAT)
sys.path.insert(0, str(SCRIPT.parent))
from connectors import dropbox_local


class ProtocolParsingTests(unittest.TestCase):

    def test_parse_result_packet_preserves_literal_pipe(self):
        line = '770|REQ-1|BISTURI|TERMUX|RESULT|{"text":"a|b"}|OK'
        packet = NYX_CHAT.parse_packet(line)
        self.assertIsNotNone(packet)
        self.assertEqual(packet["id"], "REQ-1")
        ok, text = NYX_CHAT.decode_response(packet)
        self.assertTrue(ok)
        self.assertEqual(text, "a|b")

    def test_decode_success_response(self):
        packet = {
            "id": "REQ-2", "type": "RESULT",
            "data": '{"text":"Respuesta local"}', "status": "OK",
        }
        ok, text = NYX_CHAT.decode_response(packet)
        self.assertTrue(ok)
        self.assertEqual(text, "Respuesta local")

    def test_decode_error_packet(self):
        packet = {
            "id": "REQ-3", "type": "ERROR",
            "data": '{"text":"runtime_not_ready"}', "status": "FAIL",
        }
        ok, text = NYX_CHAT.decode_response(packet)
        self.assertFalse(ok)
        self.assertEqual(text, "runtime_not_ready")

    def test_reject_non_protocol_line(self):
        self.assertIsNone(NYX_CHAT.parse_packet("not a 770 packet"))


class DropboxCommandTests(unittest.TestCase):

    def test_dropbox_list_is_an_allowlisted_fixed_command(self):
        with patch.object(NYX_CHAT, "emit") as emit, patch.object(
            dropbox_local, "call", return_value='{"entries":[]}'
        ) as call:
            result = NYX_CHAT.main(["--dropbox-list", "/Nyx Ia/Recursos"])
        self.assertEqual(result, 0)
        call.assert_called_once_with("list", {"path": "/Nyx Ia/Recursos"})
        payload = emit.call_args.args[0]
        self.assertTrue(payload["ok"])
        self.assertEqual(payload["kind"], "dropbox")
        self.assertEqual(payload["data"]["entries"], [])

    def test_dropbox_more_uses_the_pagination_cursor(self):
        with patch.object(NYX_CHAT, "emit") as emit, patch.object(
            dropbox_local, "call",
            return_value='{"entries":[{"name":"next.png"}],"has_more":false,"cursor":null}',
        ) as call:
            result = NYX_CHAT.main(["--dropbox-more", "cursor-token-123"])
        self.assertEqual(result, 0)
        call.assert_called_once_with("list", {"cursor": "cursor-token-123"})
        payload = emit.call_args.args[0]
        self.assertTrue(payload["ok"])
        self.assertEqual(payload["action"], "list")
        self.assertEqual(payload["args"]["cursor"], "cursor-token-123")

    def test_dropbox_download_is_an_allowlisted_fixed_command(self):
        with patch.object(NYX_CHAT, "emit") as emit, patch.object(
            dropbox_local, "call",
            return_value='{"downloaded":true,"name":"image.png","path":"/tmp/image.png"}',
        ) as call:
            result = NYX_CHAT.main(["--dropbox-download", "/Nyx Ia/image.png"])
        self.assertEqual(result, 0)
        call.assert_called_once_with("download", {"path": "/Nyx Ia/image.png"})
        self.assertTrue(emit.call_args.args[0]["data"]["downloaded"])

    def test_dropbox_status_returns_authorization_error_without_leaking_tokens(self):
        with patch.object(NYX_CHAT, "emit") as emit, patch.object(
            dropbox_local, "call", side_effect=RuntimeError("dropbox_not_authorized")
        ):
            result = NYX_CHAT.main(["--dropbox-status"])
        self.assertEqual(result, 1)
        payload = emit.call_args.args[0]
        self.assertFalse(payload["ok"])
        self.assertEqual(payload["error"], "dropbox_not_authorized")

    def test_dropbox_flags_reject_missing_or_extra_arguments(self):
        with patch.object(NYX_CHAT, "emit") as emit, patch.object(
            dropbox_local, "call"
        ) as call:
            self.assertEqual(NYX_CHAT.main(["--dropbox-list"]), 2)
            self.assertEqual(NYX_CHAT.main(["--dropbox-more"]), 2)
            self.assertEqual(NYX_CHAT.main(["--dropbox-download", "/file.png", "extra"]), 2)
        self.assertEqual(call.call_count, 0)
        self.assertEqual(emit.call_count, 3)


if __name__ == "__main__":
    unittest.main()
