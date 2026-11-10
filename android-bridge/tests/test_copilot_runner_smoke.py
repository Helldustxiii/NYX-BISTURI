import sys
import unittest
from pathlib import Path
from types import SimpleNamespace

TOOLS = Path(__file__).resolve().parents[1] / "tools"
sys.path.insert(0, str(TOOLS))

from nyx_copilot_runner_smoke import require_copilot_token, response_content


class CopilotRunnerSmokeTests(unittest.TestCase):
    def test_requires_explicit_copilot_token(self):
        with self.assertRaisesRegex(RuntimeError, "COPILOT_GITHUB_TOKEN is required"):
            require_copilot_token({})

    def test_rejects_blank_copilot_token(self):
        with self.assertRaises(RuntimeError):
            require_copilot_token({"COPILOT_GITHUB_TOKEN": "  "})

    def test_reads_token_without_logging_or_mutating_it(self):
        self.assertEqual(
            require_copilot_token({"COPILOT_GITHUB_TOKEN": " token-value "}),
            "token-value",
        )

    def test_extracts_assistant_content(self):
        response = SimpleNamespace(data=SimpleNamespace(content=" COPILOT_SMOKE_OK "))
        self.assertEqual(response_content(response), "COPILOT_SMOKE_OK")

    def test_rejects_missing_assistant_content(self):
        with self.assertRaisesRegex(RuntimeError, "no assistant text"):
            response_content(SimpleNamespace(data=None))


if __name__ == "__main__":
    unittest.main()
