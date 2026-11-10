import sys
import unittest
from pathlib import Path
from unittest.mock import Mock

TOOLS = Path(__file__).resolve().parents[1] / "tools"
sys.path.insert(0, str(TOOLS))

from nyx_orchestration import (
    ExecutionGrant,
    ExecutionPermissions,
    ExecutionReport,
    ReviewReport,
    TaskManifest,
    TaskState,
    is_path_allowed,
    run_task,
    validate_changed_paths,
    validate_grant,
    validate_manifest,
)
from nyx_orchestration.policy import PolicyViolation


def manifest(**overrides):
    values = {
        "task_id": "task-001",
        "objective": "Add an offline orchestration contract",
        "repository": "Helldustxiii/NYX-BISTURI",
        "base_ref": "main",
        "allowed_paths": ("android-bridge/docs/", "android-bridge/tests/"),
        "permissions": ExecutionPermissions(),
    }
    values.update(overrides)
    return TaskManifest(**values)


def grant(task_id="task-001", paths=("android-bridge/docs/", "android-bridge/tests/")):
    return ExecutionGrant(task_id, paths)


class PolicyTests(unittest.TestCase):
    def test_manifest_requires_task_id_and_objective(self):
        for values in ({"task_id": " "}, {"objective": ""}):
            with self.subTest(values=values), self.assertRaises(PolicyViolation):
                validate_manifest(manifest(**values))

    def test_manifest_only_accepts_the_canonical_repository(self):
        with self.assertRaisesRegex(PolicyViolation, "INVALID_REPOSITORY"):
            validate_manifest(manifest(repository="someone/else"))

    def test_manifest_rejects_dangerous_permissions(self):
        for name in ("network", "shell", "git_push", "merge", "deploy"):
            permissions = ExecutionPermissions(**{name: True})
            with self.subTest(permission=name), self.assertRaisesRegex(
                PolicyViolation, "PERMISSION_DENIED"
            ):
                validate_manifest(manifest(permissions=permissions))

    def test_allowed_roots_are_limited_to_docs_and_tests(self):
        for root in ("JARDIN/", "android-bridge/app/", ".github/"):
            with self.subTest(root=root), self.assertRaises(PolicyViolation):
                validate_manifest(manifest(allowed_paths=(root,)))

    def test_path_policy_rejects_traversal_absolute_and_backslash_paths(self):
        invalid = (
            "../JARDIN/secret.md",
            "android-bridge/docs/../../JARDIN/secret.md",
            "/etc/passwd",
            r"android-bridge\docs\secret.md",
            "android-bridge/docs//secret.md",
            "android-bridge/docs/./secret.md",
            "android-bridge/docs/secret\x00.md",
            "",
        )
        for path in invalid:
            with self.subTest(path=path):
                self.assertFalse(is_path_allowed(path, ("android-bridge/docs/",)))

    def test_grant_must_match_task_and_not_expand_scope(self):
        item = manifest(allowed_paths=("android-bridge/docs/",))
        with self.assertRaisesRegex(PolicyViolation, "INVALID_GRANT"):
            validate_grant(item, grant(task_id="other"))
        with self.assertRaisesRegex(PolicyViolation, "INVALID_GRANT"):
            validate_grant(item, grant(paths=("android-bridge/tests/",)))

    def test_changed_paths_must_remain_inside_manifest_scope(self):
        item = manifest(allowed_paths=("android-bridge/docs/",))
        with self.assertRaisesRegex(PolicyViolation, "OUT_OF_SCOPE_CHANGE"):
            validate_changed_paths(item, ("JARDIN/secret.md",))


class FakeExecutor:
    def __init__(self, report=None, error=None):
        self.report = report or ExecutionReport(
            ("android-bridge/docs/770-ORCHESTRATION-POC.md",),
            "diff --git a/file b/file",
            ("unit-test-report: 8 passed",),
        )
        self.error = error
        self.calls = 0

    def execute(self, task):
        self.calls += 1
        if self.error:
            raise self.error
        return self.report


class FakeReviewer:
    def __init__(self, report=None, error=None):
        self.report = report or ReviewReport(True, ())
        self.error = error
        self.calls = 0

    def review(self, task, execution):
        self.calls += 1
        if self.error:
            raise self.error
        return self.report


class ControllerTests(unittest.TestCase):
    def setUp(self):
        self.task = manifest(allowed_paths=("android-bridge/docs/",))
        self.authorization = grant(paths=("android-bridge/docs/",))

    def test_executor_is_not_called_without_a_valid_grant(self):
        executor = FakeExecutor()
        reviewer = FakeReviewer()
        result = run_task(
            self.task,
            grant(task_id="wrong", paths=("android-bridge/docs/",)),
            executor,
            reviewer,
        )
        self.assertEqual(result.state, TaskState.BLOCKED)
        self.assertEqual(result.error_code, "INVALID_GRANT")
        self.assertEqual(executor.calls, 0)
        self.assertEqual(reviewer.calls, 0)

    def test_successful_task_requires_a_separate_reviewer_acceptance(self):
        executor = FakeExecutor()
        reviewer = FakeReviewer(ReviewReport(True))
        result = run_task(self.task, self.authorization, executor, reviewer)
        self.assertEqual(result.state, TaskState.ACCEPTED)
        self.assertEqual(reviewer.calls, 1)
        self.assertEqual(
            [event.state for event in result.audit],
            [
                TaskState.PLANNED,
                TaskState.AUTHORIZED,
                TaskState.EXECUTING,
                TaskState.REVIEWING,
                TaskState.ACCEPTED,
            ],
        )

    def test_reviewer_rejection_produces_rejected_state(self):
        result = run_task(
            self.task,
            self.authorization,
            FakeExecutor(),
            FakeReviewer(ReviewReport(False, ("test failed",))),
        )
        self.assertEqual(result.state, TaskState.REJECTED)
        self.assertIsNone(result.error_code)

    def test_out_of_scope_executor_result_is_blocked_before_review(self):
        report = ExecutionReport(("JARDIN/private.md",), "patch", ("evidence",))
        reviewer = FakeReviewer()
        result = run_task(
            self.task, self.authorization, FakeExecutor(report), reviewer
        )
        self.assertEqual(result.state, TaskState.BLOCKED)
        self.assertEqual(result.error_code, "OUT_OF_SCOPE_CHANGE")
        self.assertEqual(reviewer.calls, 0)

    def test_executor_exception_is_fail_closed(self):
        result = run_task(
            self.task,
            self.authorization,
            FakeExecutor(error=RuntimeError("secret details")),
            FakeReviewer(),
        )
        self.assertEqual(result.state, TaskState.BLOCKED)
        self.assertEqual(result.error_code, "EXECUTOR_FAILED")
        self.assertNotIn("secret details", repr(result.audit))

    def test_reviewer_exception_is_fail_closed(self):
        result = run_task(
            self.task,
            self.authorization,
            FakeExecutor(),
            FakeReviewer(error=RuntimeError("secret details")),
        )
        self.assertEqual(result.state, TaskState.BLOCKED)
        self.assertEqual(result.error_code, "REVIEWER_FAILED")

    def test_empty_execution_report_is_blocked_before_review(self):
        reports = (
            ExecutionReport((), "patch", ("evidence",)),
            ExecutionReport(("android-bridge/docs/a.md",), " ", ("evidence",)),
            ExecutionReport(("android-bridge/docs/a.md",), "patch", ()),
        )
        for report in reports:
            reviewer = FakeReviewer()
            result = run_task(
                self.task, self.authorization, FakeExecutor(report), reviewer
            )
            with self.subTest(report=report):
                self.assertEqual(result.state, TaskState.BLOCKED)
                self.assertEqual(result.error_code, "EMPTY_EXECUTION_REPORT")
                self.assertEqual(reviewer.calls, 0)

    def test_audit_events_follow_the_actual_state_transitions(self):
        result = run_task(
            self.task,
            self.authorization,
            FakeExecutor(),
            FakeReviewer(ReviewReport(False)),
        )
        self.assertEqual(result.audit[-1].state, TaskState.REJECTED)
        self.assertEqual(result.audit[-1].code, "REVIEW_REJECTED")

    def test_invalid_executor_and_reviewer_reports_fail_closed(self):
        bad_executor = Mock()
        bad_executor.execute.return_value = {"patch": "no"}
        result = run_task(self.task, self.authorization, bad_executor, FakeReviewer())
        self.assertEqual(result.error_code, "INVALID_EXECUTION_REPORT")
        bad_reviewer = Mock()
        bad_reviewer.review.return_value = {"accepted": True}
        result = run_task(
            self.task, self.authorization, FakeExecutor(), bad_reviewer
        )
        self.assertEqual(result.error_code, "INVALID_REVIEW_REPORT")

    def test_malformed_execution_report_fields_fail_closed(self):
        malformed = ExecutionReport(
            ("android-bridge/docs/a.md",), None, ("evidence",)
        )
        result = run_task(
            self.task, self.authorization, FakeExecutor(malformed), FakeReviewer()
        )
        self.assertEqual(result.state, TaskState.BLOCKED)
        self.assertEqual(result.error_code, "EMPTY_EXECUTION_REPORT")

    def test_non_boolean_reviewer_verdict_fails_closed(self):
        malformed = ReviewReport("yes")
        result = run_task(
            self.task, self.authorization, FakeExecutor(), FakeReviewer(malformed)
        )
        self.assertEqual(result.state, TaskState.BLOCKED)
        self.assertEqual(result.error_code, "INVALID_REVIEW_REPORT")


if __name__ == "__main__":
    unittest.main()
