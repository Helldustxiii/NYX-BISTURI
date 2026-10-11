import importlib.util
import json
import unittest
from pathlib import Path
from unittest.mock import Mock

RUNTIME_PATH = Path(__file__).resolve().parents[1] / "tools" / "nyx-770-runtime.py"
SPEC = importlib.util.spec_from_file_location("nyx_770_runtime", RUNTIME_PATH)
runtime = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(runtime)


class FakeProtocol:
    def __init__(self):
        self.sent = []
        self.delegated = []

    def parse_message(self, message):
        fields = message.rstrip("\n").split("|", 6)
        if len(fields) != 7 or fields[0] != "770":
            return None
        return {
            "id": fields[1],
            "from": fields[2],
            "to": fields[3],
            "type": fields[4],
            "data": fields[5],
            "status": fields[6],
        }

    def build_metadata(self, source, message_id, msg_type):
        return {"source": source, "id": message_id, "type": msg_type}

    def send(self, message):
        self.sent.append(message)

    def process(self, message):
        self.delegated.append(message)


def make_message(message_id, msg_type, payload, source="TERMUX"):
    data = payload if isinstance(payload, str) else json.dumps(payload)
    return f"770|{message_id}|{source}|BISTURI|{msg_type}|{data}|OK\n"


def decode_reply(message):
    fields = message.rstrip("\n").split("|", 6)
    return fields, json.loads(fields[5])


class RuntimeRoutingTests(unittest.TestCase):
    def setUp(self):
        self.protocol = FakeProtocol()
        self.android_call = Mock(side_effect=lambda method: f"android:{method}:ok")
        self.connectors = {
            "android": {"transport": "local", "status": "active", "actions": ("ping", "device_info")},
            "github": {"transport": "mcp", "status": "available_via_chatgpt", "actions": ("search", "read")},
        }
        self.get_connector = self.connectors.get

    def send(self, message):
        runtime.process_message(message, self.protocol, self.android_call, self.get_connector)

    def test_android_message_returns_result_with_same_id(self):
        self.send(make_message("A1", "ANDROID", {"method": "ping"}))
        self.assertEqual(len(self.protocol.sent), 1)
        fields, payload = decode_reply(self.protocol.sent[0])
        self.assertEqual(fields[1], "A1")
        self.assertEqual(fields[4], "RESULT")
        self.assertEqual(fields[6], "OK")
        self.assertEqual(payload["text"], "android:ping:ok")
        self.android_call.assert_called_once_with("ping")

    def test_android_connector_uses_local_driver(self):
        self.send(make_message("A2", "CONNECTOR", {"service": "android", "action": "device_info", "args": {}}))
        fields, payload = decode_reply(self.protocol.sent[0])
        self.assertEqual(fields[1], "A2")
        self.assertEqual(fields[4], "RESULT")
        self.assertEqual(payload["text"], "android:device_info:ok")

    def test_mcp_connector_fails_closed_without_relay(self):
        self.send(make_message("A3", "CONNECTOR", {"service": "github", "action": "search", "args": {"query": "x"}}))
        fields, payload = decode_reply(self.protocol.sent[0])
        error = json.loads(payload["text"])
        self.assertEqual(fields[4], "ERROR")
        self.assertEqual(fields[6], "FAIL")
        self.assertEqual(error["error"], "mcp_relay_required")

    def test_unknown_service_and_action_fail_closed(self):
        self.send(make_message("A4", "CONNECTOR", {"service": "unknown", "action": "ping", "args": {}}))
        self.send(make_message("A5", "CONNECTOR", {"service": "android", "action": "shell", "args": {}}))
        replies = [decode_reply(item) for item in self.protocol.sent]
        self.assertEqual([item[0][6] for item in replies], ["FAIL", "FAIL"])
        self.assertIn("connector_not_registered", replies[0][1]["text"])
        self.assertIn("action_not_allowed", replies[1][1]["text"])

    def test_invalid_payload_is_reported(self):
        self.send(make_message("A6", "ANDROID", ["not", "an", "object"]))
        fields, payload = decode_reply(self.protocol.sent[0])
        self.assertEqual(fields[4], "ERROR")
        self.assertEqual(fields[6], "FAIL")
        self.assertEqual(payload["text"], "payload_must_be_object")

    def test_unhandled_message_type_is_delegated_once(self):
        message = make_message("A7", "PING", {})
        self.send(message)
        self.assertEqual(self.protocol.delegated, [message])
        self.assertEqual(self.protocol.sent, [])

    def test_malformed_message_is_ignored(self):
        self.send("not-a-770-message\n")
        self.assertEqual(self.protocol.sent, [])
        self.assertEqual(self.protocol.delegated, [])


if __name__ == "__main__":
    unittest.main()
