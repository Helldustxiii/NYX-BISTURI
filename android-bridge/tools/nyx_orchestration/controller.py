"""Offline executor/reviewer orchestration state machine."""
from __future__ import annotations

from .contracts import (
    AuditEvent,
    ExecutionGrant,
    ExecutionReport,
    Executor,
    Reviewer,
    ReviewReport,
    TaskManifest,
    TaskResult,
    TaskState,
)
from .policy import PolicyViolation, validate_changed_paths, validate_grant


def run_task(
    manifest: TaskManifest,
    grant: ExecutionGrant,
    executor: Executor,
    reviewer: Reviewer,
) -> TaskResult:
    """Run an offline task contract; never apply a patch or invoke external tools."""
    events: list[AuditEvent] = [AuditEvent(TaskState.PLANNED, "TASK_PLANNED")]

    def blocked(code: str, execution=None, review=None) -> TaskResult:
        events.append(AuditEvent(TaskState.BLOCKED, code))
        return TaskResult(TaskState.BLOCKED, execution, review, code, tuple(events))

    try:
        validate_grant(manifest, grant)
    except PolicyViolation as error:
        return blocked(error.code)

    events.append(AuditEvent(TaskState.AUTHORIZED, "GRANT_VALIDATED"))
    events.append(AuditEvent(TaskState.EXECUTING, "EXECUTION_STARTED"))
    try:
        execution = executor.execute(manifest)
    except Exception:
        return blocked("EXECUTOR_FAILED")

    if not isinstance(execution, ExecutionReport):
        return blocked("INVALID_EXECUTION_REPORT")
    try:
        validate_changed_paths(manifest, execution.changed_paths)
    except PolicyViolation as error:
        return blocked(error.code, execution=execution)
    if (
        not isinstance(execution.patch, str)
        or not execution.patch.strip()
        or not isinstance(execution.evidence, tuple)
        or not execution.evidence
        or any(not isinstance(item, str) or not item.strip() for item in execution.evidence)
    ):
        return blocked("EMPTY_EXECUTION_REPORT", execution=execution)

    events.append(AuditEvent(TaskState.REVIEWING, "REVIEW_STARTED"))
    try:
        review = reviewer.review(manifest, execution)
    except Exception:
        return blocked("REVIEWER_FAILED", execution=execution)
    if (
        not isinstance(review, ReviewReport)
        or type(review.accepted) is not bool
        or not isinstance(review.findings, tuple)
        or any(not isinstance(item, str) for item in review.findings)
    ):
        return blocked("INVALID_REVIEW_REPORT", execution=execution)
    if not review.accepted:
        events.append(AuditEvent(TaskState.REJECTED, "REVIEW_REJECTED"))
        return TaskResult(
            TaskState.REJECTED, execution, review, None, tuple(events)
        )
    events.append(AuditEvent(TaskState.ACCEPTED, "REVIEW_ACCEPTED"))
    return TaskResult(TaskState.ACCEPTED, execution, review, None, tuple(events))
