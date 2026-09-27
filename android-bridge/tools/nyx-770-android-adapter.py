#!/data/data/com.termux/files/usr/bin/python
"""
NYX 770 -> Android Bridge adapter.
"""

import json
import os
import shutil
import subprocess
import sys

KERNEL_DIR = os.path.expanduser("~/bisturi-kernel")
TOOLS_DIR = os.path.expanduser("~/nyx-android-bridge-integrate/android-bridge/tools")
for path in (KERNEL_DIR, TOOLS_DIR):
    if path not in sys.path:
        sys.path.insert(0, path)

import protocol770
from connectors.android import call as bridge_call

IN_CHANNEL = os.path.expanduser("~/NYX/channel/link")
OUT_CHANNEL = os.path.expanduser("~/NYX/channel/out")

ALLOWED_METHODS = {
    "ping": "ping",
    "device_info": "device_info",
}


def bridge_executable():
    command = shutil.which("nyx-bridge")
    if command:
        return command

    fallback = os.path.expanduser("~/bin/nyx-bridge")
    if os.path.isfile(fallback) and os.access(fallback, os.X_OK):
        return fallback

    raise FileNotFoundError("nyx-bridge_not_found")


def bridge_call(method):
    command = ALLOWED_METHODS.get(method)
    if command is None:
        raise ValueError("method_not_allowed")

    result = subprocess.run(
        [bridge_executable(), command],
        capture_output=True,
        text=True,
        timeout=10,
        check=False,
    )

    if result.returncode != 0:
        detail = (result.stderr or result.stdout).strip()
        raise RuntimeError(detail or f"nyx-bridge_exit_{result.returncode}")

    return result.stdout.strip()


def send_android_result(packet, text, status="OK", msg_type="RESULT"):
    metadata = protocol770.build_metadata(packet["from"], packet["id"], msg_type)
    result = {
        "text": text,
        "metadata": metadata,
    }

    protocol770.send(
        f'770|{packet["id"]}|BISTURI|{packet["from"]}|{msg_type}|'
        f'{json.dumps(result, ensure_ascii=False)}|{status}'
    )


def process(message):
    packet = protocol770.parse_message(message)

    if packet is None:
        return

    if packet["type"] != "ANDROID":
        protocol770.process(message)
        return

    try:
        payload = json.loads(packet["data"] or "{}")
        if not isinstance(payload, dict):
            raise ValueError("android_payload_must_be_object")

        method = payload.get("method", "")
        output = bridge_call(method)
        send_android_result(packet, output)

    except Exception as error:
        send_android_result(
            packet,
            str(error),
            status="FAIL",
            msg_type="ERROR",
        )


def main():
    print("NYX 770 ANDROID ADAPTER ONLINE", flush=True)
    print(f"IN : {IN_CHANNEL}", flush=True)
    print(f"OUT: {OUT_CHANNEL}", flush=True)
    print("METHODS: ping, device_info", flush=True)

    with open(IN_CHANNEL, "r", encoding="utf-8") as channel:
        for message in channel:
            if message.strip():
                process(message)


if __name__ == "__main__":
    main()
