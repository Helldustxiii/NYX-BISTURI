#!/data/data/com.termux/files/usr/bin/python
"""NYX 770 connector gateway.

Protocol:
  770|ID|FROM|BISTURI|CONNECTOR|{"service":"...","action":"...","args":{...}}|OK

The gateway currently executes only local connectors. MCP-backed connectors are
registered and reported as requiring an authenticated relay.
"""

import json
import os
import sys

KERNEL_DIR = os.path.expanduser("~/bisturi-kernel")
TOOLS_DIR = os.path.expanduser("~/nyx-android-bridge-integrate/android-bridge/tools")
for path in (KERNEL_DIR, TOOLS_DIR):
    if path not in sys.path:
        sys.path.insert(0, path)

import protocol770
from connectors.registry import get_connector, list_connectors

IN_CHANNEL = os.path.expanduser("~/NYX/channel/link")


def send(packet, msg_type, payload, status="OK"):
    result = {
        "text": payload if isinstance(payload, str) else json.dumps(payload, ensure_ascii=False),
        "metadata": protocol770.build_metadata(packet["from"], packet["id"], msg_type),
    }
    protocol770.send(
        f'770|{packet["id"]}|BISTURI|{packet["from"]}|{msg_type}|'
        f'{json.dumps(result, ensure_ascii=False)}|{status}'
    )


def process(message):
    packet = protocol770.parse_message(message)
    if packet is None:
        return

    if packet["type"] != "CONNECTOR":
        protocol770.process(message)
        return

    try:
        request = json.loads(packet["data"] or "{}")
        if not isinstance(request, dict):
            raise ValueError("connector_payload_must_be_object")

        service = request.get("service", "")
        action = request.get("action", "")
        connector = get_connector(service)

        if connector is None:
            raise ValueError("connector_not_registered")

        if action not in connector["actions"]:
            raise ValueError("action_not_allowed")

        if connector["transport"] != "local":
            send(
                packet,
                "ERROR",
                {
                    "error": "mcp_relay_required",
                    "service": service,
                    "action": action,
                    "transport": connector["transport"],
                    "status": connector["status"],
                },
                "FAIL",
            )
            return

        if service == "android":
            from connectors.android import call
            output = call(action)
            send(packet, output)
            return

        raise ValueError("local_connector_not_implemented")

    except Exception as error:
        send(packet, "ERROR", str(error), "FAIL")


def main():
    print("NYX 770 CONNECTOR GATEWAY ONLINE", flush=True)
    print(f"IN : {IN_CHANNEL}", flush=True)
    print("CONNECTORS:", flush=True)
    for name, spec in list_connectors().items():
        print(f"  - {name}: {spec['transport']}/{spec['status']}", flush=True)

    with open(IN_CHANNEL, "r", encoding="utf-8") as channel:
        for message in channel:
            if message.strip():
                process(message)


if __name__ == "__main__":
    main()
