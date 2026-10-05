"""Local Android connector driver for the 770 gateway."""

import os
import shutil
import subprocess

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


def call(action):
    command = ALLOWED_METHODS.get(action)
    if command is None:
        raise ValueError("action_not_allowed")

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
