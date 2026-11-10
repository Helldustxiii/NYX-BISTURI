"""No-tools GitHub Copilot SDK connectivity check for an ephemeral CI runner."""
from __future__ import annotations

import asyncio
import os
import tempfile
from collections.abc import Mapping

EXPECTED_MARKER = "COPILOT_SMOKE_OK"
PROMPT = (
    "This is a connectivity smoke test only. Do not access files or perform actions. "
    f"Reply with exactly this marker and nothing else: {EXPECTED_MARKER}"
)


def require_copilot_token(environ: Mapping[str, str] | None = None) -> str:
    source = os.environ if environ is None else environ
    token = source.get("COPILOT_GITHUB_TOKEN", "")
    if not isinstance(token, str) or not token.strip():
        raise RuntimeError("COPILOT_GITHUB_TOKEN is required for this manual smoke test.")
    return token.strip()


def response_content(response: object) -> str:
    data = getattr(response, "data", None)
    content = getattr(data, "content", None)
    if not isinstance(content, str) or not content.strip():
        raise RuntimeError("Copilot returned no assistant text.")
    return content.strip()


async def run_smoke_test() -> None:
    from copilot import CopilotClient

    token = require_copilot_token()
    with tempfile.TemporaryDirectory(prefix="nyx-copilot-smoke-") as runtime_home:
        async with CopilotClient(
            mode="empty",
            base_directory=runtime_home,
            working_directory=runtime_home,
            github_token=token,
            use_logged_in_user=False,
        ) as client:
            async with await client.create_session(
                model="auto",
                available_tools=[],
                memory={"enabled": False},
                enable_session_store=False,
                working_directory=runtime_home,
            ) as session:
                response = await session.send_and_wait(PROMPT, timeout=120)
                content = response_content(response)
                if content != EXPECTED_MARKER:
                    raise RuntimeError("Copilot smoke response did not match the expected marker.")

    print(EXPECTED_MARKER)


def main() -> None:
    asyncio.run(run_smoke_test())


if __name__ == "__main__":
    main()
