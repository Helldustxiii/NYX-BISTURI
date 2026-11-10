"""Fail-closed manifest, grant, and repository path policy."""
from __future__ import annotations

from .contracts import ExecutionGrant, ExecutionPermissions, TaskManifest

CANONICAL_REPOSITORY = "Helldustxiii/NYX-BISTURI"
ALLOWED_ROOTS = ("android-bridge/docs/", "android-bridge/tests/")


class PolicyViolation(ValueError):
    """Policy error with a stable, non-sensitive machine-readable code."""

    def __init__(self, code: str) -> None:
        self.code = code
        super().__init__(code)


def _validate_relative_path(path: str) -> None:
    if not isinstance(path, str) or not path or "\x00" in path:
        raise PolicyViolation("INVALID_PATH")
    if path.startswith("/") or "\\" in path:
        raise PolicyViolation("INVALID_PATH")
    parts = path.split("/")
    if any(part in ("", ".", "..") for part in parts):
        raise PolicyViolation("INVALID_PATH")
    if parts[0] in (".git", ".github") or any(
        part.startswith(".env") for part in parts
    ):
        raise PolicyViolation("PROTECTED_PATH")


def _is_within_root(path: str, root: str) -> bool:
    return path.startswith(root) and len(path) > len(root)


def is_path_allowed(path: str, allowed_paths: tuple[str, ...]) -> bool:
    try:
        _validate_relative_path(path)
    except PolicyViolation:
        return False
    if not isinstance(allowed_paths, tuple) or not allowed_paths:
        return False
    if any(root not in ALLOWED_ROOTS for root in allowed_paths):
        return False
    return any(_is_within_root(path, root) for root in allowed_paths)


def validate_manifest(manifest: TaskManifest) -> None:
    if not isinstance(manifest, TaskManifest):
        raise PolicyViolation("INVALID_MANIFEST")
    if not isinstance(manifest.task_id, str) or not manifest.task_id.strip():
        raise PolicyViolation("INVALID_MANIFEST")
    if not isinstance(manifest.objective, str) or not manifest.objective.strip():
        raise PolicyViolation("INVALID_MANIFEST")
    if manifest.repository != CANONICAL_REPOSITORY:
        raise PolicyViolation("INVALID_REPOSITORY")
    if not isinstance(manifest.base_ref, str) or not manifest.base_ref.strip():
        raise PolicyViolation("INVALID_MANIFEST")
    if not isinstance(manifest.allowed_paths, tuple) or not manifest.allowed_paths:
        raise PolicyViolation("INVALID_SCOPE")
    if any(root not in ALLOWED_ROOTS for root in manifest.allowed_paths):
        raise PolicyViolation("INVALID_SCOPE")
    if len(set(manifest.allowed_paths)) != len(manifest.allowed_paths):
        raise PolicyViolation("INVALID_SCOPE")
    permissions = manifest.permissions
    if not isinstance(permissions, ExecutionPermissions):
        raise PolicyViolation("INVALID_PERMISSIONS")
    if any(
        getattr(permissions, name, True) is not False
        for name in ("network", "shell", "git_push", "merge", "deploy")
    ):
        raise PolicyViolation("PERMISSION_DENIED")


def validate_grant(manifest: TaskManifest, grant: ExecutionGrant) -> None:
    validate_manifest(manifest)
    if not isinstance(grant, ExecutionGrant):
        raise PolicyViolation("INVALID_GRANT")
    if grant.task_id != manifest.task_id:
        raise PolicyViolation("INVALID_GRANT")
    if not isinstance(grant.allowed_paths, tuple) or not grant.allowed_paths:
        raise PolicyViolation("INVALID_GRANT")
    if len(set(grant.allowed_paths)) != len(grant.allowed_paths):
        raise PolicyViolation("INVALID_GRANT")
    if any(root not in manifest.allowed_paths for root in grant.allowed_paths):
        raise PolicyViolation("INVALID_GRANT")


def validate_changed_paths(
    manifest: TaskManifest, changed_paths: tuple[str, ...]
) -> None:
    validate_manifest(manifest)
    if not isinstance(changed_paths, tuple) or not changed_paths:
        raise PolicyViolation("EMPTY_EXECUTION_REPORT")
    if len(set(changed_paths)) != len(changed_paths):
        raise PolicyViolation("INVALID_PATH")
    if any(not is_path_allowed(path, manifest.allowed_paths) for path in changed_paths):
        raise PolicyViolation("OUT_OF_SCOPE_CHANGE")
