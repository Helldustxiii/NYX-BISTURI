#!/data/data/com.termux/files/usr/bin/python
"""Deprecated entry point.

The Android adapter no longer owns channel/link. Run nyx-770-runtime.py so one
process routes both ANDROID and CONNECTOR messages without competing FIFO reads.
"""

import sys


def main() -> int:
    print(
        "Este consumidor fue sustituido por nyx-770-runtime.py; "
        "no debe leer ~/NYX/channel/link de forma independiente.",
        file=sys.stderr,
    )
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
