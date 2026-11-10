"""Immutable contracts for the offline NYX agent orchestration proof."""
from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
from typing import Protocol


@dataclass(frozen=True, slots=True)
class ExecutionPermissions:
    network: bool = False
    shell: bool = False
    git_push: bool = False
    merge: bool = False
    deploy: bool = False


@dataclass(frozen=True, slots=True)
class TaskManifest:
    task_id: str
    objective: str
    repository: str
    base_ref: str
    allowed_paths: tuple[str, ...]
    permissions: ExecutionPermissions = ExecutionPermissions()


@dataclass(frozen=True, slots=True)
class ExecutionGrant:
    task_id: str
    allowed_paths: tuple[str, ...]


@dataclass(frozen=True, slots=True)
class ExecutionReport:
    changed_paths: tuple[str, ...]
    patch: str
    evidence: tuple[str, ...]


@dataclass(frozen=True, slots=True)
class ReviewReport:
    accepted: bool
    findings: tuple[str, ...] = ()


class TaskState(str, Enum):
    PLANNED = "planned"
    AUTHORIZED = "authorized"
    EXECUTING = "executing"
    REVIEWING = "reviewing"
    ACCEPTED = "accepted"
    REJECTED = "rejected"
    BLOCKED = "blocked"


@dataclass(frozen=True, slots=True)
class AuditEvent:
    state: TaskState
    code: str


@dataclass(frozen=True, slots=True)
class TaskResult:
    state: TaskState
    execution: ExecutionReport | None
    review: ReviewReport | None
    error_code: str | None
    audit: tuple[AuditEvent, ...]


class Executor(Protocol):
    def execute(self, manifest: TaskManifest) -> ExecutionReport:
        """Return a declarative report. This interface does not provide tools."""


class Reviewer(Protocol):
    def review(
        self, manifest: TaskManifest, execution: ExecutionReport
    ) -> ReviewReport:
        """Review evidence without receiving a write-capable interface."""
