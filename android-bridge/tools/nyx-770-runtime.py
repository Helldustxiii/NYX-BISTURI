#!/data/data/com.termux/files/usr/bin/python
"""Single-reader runtime for NYX's protocol 770 Android and connector requests.

Only this process should consume ~/NYX/channel/link. The legacy adapter and
connector scripts are retained as explicit deprecation notices to avoid
competing FIFO readers.
"""

from __future__ import annotations

import fcntl
import json
import os
import stat
import sys
from pathlib import Path
from typing import Any, Callable


def _reply(
    protocol: Any,
    packet: dict[str, Any],
    msg_type: str,
    payload: Any,
    status: str = "OK",
) -> None:
    text = payload if isinstance(payload, str) else json.dumps(payload, ensure_ascii=False)
    result = {
        "text": text,
        "metadata": protocol.build_metadata(packet["from"], packet["id"], msg_type),
    }
    wire_message = (
        f'770|{packet["id"]}|BISTURI|{packet["from"]}|{msg_type}|'
        f'{json.dumps(result, ensure_ascii=False)}|{status}'
    )
    protocol.send(wire_message)


def _handle_android(
    packet: dict[str, Any],
    payload: dict[str, Any],
    protocol: Any,
    android_call: Callable[[str], str],
) -> None:
    method = payload.get("method", "")
    if not isinstance(method, str):
        raise ValueError("android_method_must_be_string")
    _reply(protocol, packet, "RESULT", android_call(method))


def _handle_connector(
    packet: dict[str, Any],
    request: dict[str, Any],
    protocol: Any,
    android_call: Callable[[str], str],
    get_connector: Callable[[str], dict[str, Any] | None],
) -> None:
    service = request.get("service", "")
    action = request.get("action", "")
    args = request.get("args", {})
    if not isinstance(service, str) or not isinstance(action, str):
        raise ValueError("connector_service_and_action_must_be_strings")
    if not isinstance(args, dict):
        raise ValueError("connector_args_must_be_object")

    connector = get_connector(service)
    if connector is None:
        raise ValueError("connector_not_registered")
    if action not in connector["actions"]:
        raise ValueError("action_not_allowed")

    if connector["transport"] != "local":
        _reply(
            protocol,
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
        _reply(protocol, packet, "RESULT", android_call(action))
        return

    raise ValueError("local_connector_not_implemented")


def process_message(
    message: str,
    protocol: Any,
    android_call: Callable[[str], str],
    get_connector: Callable[[str], dict[str, Any] | None],
) -> None:
    """Route one protocol message without opening or consuming any FIFO."""
    packet = protocol.parse_message(message)
    if packet is None:
        return

    message_type = packet.get("type")
    if message_type not in ("ANDROID", "CONNECTOR"):
        protocol.process(message)
        return

    try:
        payload = json.loads(packet.get("data") or "{}")
        if not isinstance(payload, dict):
            raise ValueError("payload_must_be_object")

        if message_type == "ANDROID":
            _handle_android(packet, payload, protocol, android_call)
        else:
            _handle_connector(packet, payload, protocol, android_call, get_connector)
    except Exception as error:  # convert handler failures into protocol replies
        _reply(protocol, packet, "ERROR", str(error), "FAIL")


def acquire_runtime_lock(lock_path: Path):
    """Acquire an exclusive process lock so two copies of this runtime cannot race."""
    lock_path.parent.mkdir(parents=True, exist_ok=True)
    handle = lock_path.open("a", encoding="utf-8")
    try:
        fcntl.flock(handle.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
        handle.close()
        raise RuntimeError("runtime_already_running") from None
    return handle


def _require_fifo(path: Path) -> None:
    try:
        mode = path.stat().st_mode
    except FileNotFoundError as error:
        raise RuntimeError(f"fifo_not_found:{path}") from error
    if not stat.S_ISFIFO(mode):
        raise RuntimeError(f"input_channel_not_fifo:{path}")


def main() -> int:
    tools_dir = Path(__file__).resolve().parent
    kernel_dir = Path.home() / "bisturi-kernel"
    for path in (str(kernel_dir), str(tools_dir)):
        if path not in sys.path:
            sys.path.insert(0, path)

    try:
        import protocol770
        from connectors.android import call as android_call
        from connectors.registry import get_connector, list_connectors
    except ImportError as error:
        print(f"No se pueden cargar las dependencias de NYX 770: {error}", file=sys.stderr)
        return 2

    channel = Path.home() / "NYX" / "channel" / "link"
    lock_path = Path.home() / "NYX" / "channel" / "runtime.lock"
    try:
        _require_fifo(channel)
        lock_handle = acquire_runtime_lock(lock_path)
    except RuntimeError as error:
        print(f"NYX 770 runtime no iniciado: {error}", file=sys.stderr)
        return 2

    print("NYX 770 SINGLE-READER RUNTIME ONLINE", flush=True)
    print(f"IN : {channel}", flush=True)
    print("OUT: protocol770.send()", flush=True)
    print("ROUTES: ANDROID, CONNECTOR; other types -> protocol770.process()", flush=True)
    print("CONNECTORS:", flush=True)
    for name, spec in list_connectors().items():
        print(f"  - {name}: {spec['transport']}/{spec['status']}", flush=True)

    try:
        # A FIFO reader sees EOF when each writer closes. Reopen it to wait
        # for the next sender; the exclusive lock remains held across reopen.
        while True:
            with channel.open("r", encoding="utf-8") as input_channel:
                for message in input_channel:
                    if message.strip():
                        process_message(message, protocol770, android_call, get_connector)
    except KeyboardInterrupt:
        print("NYX 770 runtime detenido por el usuario.", flush=True)
        return 0
    finally:
        lock_handle.close()


if __name__ == "__main__":
    raise SystemExit(main())
