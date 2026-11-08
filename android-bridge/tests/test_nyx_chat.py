import importlib.machinery
import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "tools" / "nyx-chat"
LOADER = importlib.machinery.SourceFileLoader("nyx_chat", str(SCRIPT))
SPEC = importlib.util.spec_from_loader(LOADER.name, LOADER)
NYX_CHAT = importlib.util.module_from_spec(SPEC)
LOADER.exec_module(NYX_CHAT)


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


if __name__ == "__main__":
    unittest.main()
