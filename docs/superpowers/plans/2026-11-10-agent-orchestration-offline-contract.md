# NYX Agent Orchestration Offline Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Python standard-library-only orchestration contract that validates a scoped task, runs separate executor and read-only reviewer adapters, and records a fail-closed result without connecting to Termux, 770, Copilot, or external services.

**Architecture:** Add a small provider-neutral Python package under `android-bridge/tools/nyx_orchestration/`. The controller receives an explicit grant, validates the manifest and all reported changed paths, calls an executor, then calls a separate reviewer; acceptance is impossible without a positive reviewer verdict. The first pass uses deterministic in-process test adapters only. It proves controller semantics, not real model execution or sandbox isolation.

**Tech Stack:** Python 3.12, dataclasses, enums, typing.Protocol, pathlib/POSIX path validation, unittest. No new dependency.

**Spec:** `docs/agent-orchestration-architecture.md`, especially sections 3–6, 8–10, 12–14.

## Global Constraints

- Keep NYX as the only interaction identity; BISTURÍ remains a control capability, not a new agent identity.
- First understand and then act; distinguish confirmed results from plausible or uncertain results.
- The repository's only permitted write roots for this proof are `android-bridge/docs/` and `android-bridge/tests/`.
- Reject absolute paths, traversal, alternate separators, protected paths, and all paths outside those two roots.
- JARDIN, secrets, credentials, production configuration, Android application source, runtime 770, connector registry, and root application code are out of scope.
- Do not add dependencies or install Copilot SDK / Copilot CLI.
- Do not call external services, authenticate to an agent provider, invoke shell commands from an executor, or execute generated code.
- Do not connect the controller to the 770 FIFO. The runtime remains a single-reader transport and is not an agent execution bus.
- The executor may return a declarative patch/report only. It may not write files, push, merge, deploy, or expand its scope.
- The reviewer receives task and execution evidence through a separate interface and returns a verdict. This Python interface is not a security sandbox; real untrusted agents require process/tool isolation in a later phase.
- No push to main, merge, deployment, credential changes, permission changes, or persistent memory changes.
- This is **Phase B1: offline contract proof**. It does not claim that a real Copilot/Qwen agent has executed a task.

## Review Focus

1. A path such as `android-bridge/tests/../../JARDIN/secret.md`, an absolute path, or a Windows-style path must be rejected before any adapter is called.
2. A manifest that allows `.env`, `.git`, `JARDIN`, `android-bridge/app/`, or any root outside the two allowed roots must fail closed.
3. A task grant for a different task ID or a broader path set must not authorize execution.
4. An executor that reports a changed path outside the manifest scope must be blocked before review or acceptance.
5. A reviewer rejection or exception must never produce an accepted task; audit state must preserve the actual terminal outcome.

---

## File Structure

Create:
- `android-bridge/tools/nyx_orchestration/__init__.py` — public exports for the small offline controller contract.
- `android-bridge/tools/nyx_orchestration/contracts.py` — frozen dataclasses, enums, and executor/reviewer Protocols.
- `android-bridge/tools/nyx_orchestration/policy.py` — manifest validation, grant validation, and path-scope validation.
- `android-bridge/tools/nyx_orchestration/controller.py` — deterministic state machine, adapter orchestration, and audit events.
- `android-bridge/tests/test_nyx_orchestration.py` — standard-library unit tests for policy and lifecycle.

Modify:
- `android-bridge/docs/770-ORCHESTRATION-POC.md` — describe the offline contract, how to run tests, explicit limitations, and the next approval gate.

Do not modify:
- `android-bridge/tools/nyx-770-runtime.py`
- `android-bridge/tools/connectors/registry.py`
- existing 770 tests unless a test discovery issue is proven
- Android application source
- `JARDIN/` or any memory content
- `.github/workflows/`, package manifests, dependencies, or deployment config

## Task 1: Define the manifest, grants, and fail-closed path policy

**Files:**
- Create: `android-bridge/tools/nyx_orchestration/contracts.py`
- Create: `android-bridge/tools/nyx_orchestration/policy.py`
- Create: `android-bridge/tools/nyx_orchestration/__init__.py`
- Test: `android-bridge/tests/test_nyx_orchestration.py`

**Interfaces:**
- `TaskManifest(task_id: str, objective: str, repository: str, base_ref: str, allowed_paths: tuple[str, ...], permissions: ExecutionPermissions = ExecutionPermissions())`
- `ExecutionGrant(task_id: str, allowed_paths: tuple[str, ...])`
- `ExecutionPermissions(network: bool = False, shell: bool = False, git_push: bool = False, merge: bool = False, deploy: bool = False)`
- `validate_manifest(manifest: TaskManifest) -> None`
- `validate_grant(manifest: TaskManifest, grant: ExecutionGrant) -> None`
- `is_path_allowed(path: str, allowed_paths: tuple[str, ...]) -> bool` — each allowed root must be exactly `android-bridge/docs/` or `android-bridge/tests/` (trailing slash significant); no caller-defined glob or sub-root.
- `validate_changed_paths(manifest: TaskManifest, changed_paths: tuple[str, ...]) -> None`
- Policy violations raise one documented `PolicyViolation(ValueError)` subtype with stable, non-sensitive error codes.

- [ ] **Step 1: Write failing policy tests**
  - `test_manifest_requires_task_id_and_objective`: reject blank task IDs and objectives.
  - `test_manifest_only_accepts_the_canonical_repository`: reject any repository other than `Helldustxiii/NYX-BISTURI`.
  - `test_manifest_rejects_dangerous_permissions`: any enabled network, shell, push, merge, or deploy permission is rejected.
  - `test_allowed_roots_are_limited_to_docs_and_tests`: reject roots outside `android-bridge/docs/` and `android-bridge/tests/`.
  - `test_path_policy_rejects_traversal_absolute_and_backslash_paths`: reject traversal, absolute paths, backslashes, NULs, and empty paths.
  - `test_grant_must_match_task_and_not_expand_scope`: reject wrong task ID and any grant that expands the manifest scope.
  - `test_changed_paths_must_remain_inside_manifest_scope`: reject an out-of-scope executor result.

- [ ] **Step 2: Run the focused tests and verify RED**
  - Run: `python -m unittest discover -s android-bridge/tests -p 'test_nyx_orchestration.py' -v`.
  - Expected: FAIL because the new contract and policy functions do not yet exist. A syntax/import error is not an acceptable RED result.

- [ ] **Step 3: Implement the minimal contracts and policy**
  - Use immutable dataclasses for manifests, permissions, grants, and reports.
  - Normalize only safe POSIX-relative paths; reject rather than repair suspicious input.
  - Require `allowed_paths` to be a non-empty subset of the two fixed roots; compare grants as a strict subset of those roots, never as arbitrary prefixes.
  - Define allowed roots as fixed policy constants, not caller-controlled broadening.
  - Require every manifest path and every changed path to pass the same canonical path validator.
  - Make the default permissions all false and reject a manifest that turns any dangerous permission on.

- [ ] **Step 4: Run the focused tests and verify GREEN**
  - Run: `python -m unittest discover -s android-bridge/tests -p 'test_nyx_orchestration.py' -v`.
  - Expected: all Task 1 policy tests pass.

- [ ] **Step 5: Commit the task**
  - Commit message: `feat: add scoped orchestration task contract`.

## Task 2: Implement the executor → reviewer state machine

**Files:**
- Create: `android-bridge/tools/nyx_orchestration/controller.py`
- Modify: `android-bridge/tools/nyx_orchestration/contracts.py`
- Test: `android-bridge/tests/test_nyx_orchestration.py`

**Interfaces:**
- `Executor.execute(manifest: TaskManifest) -> ExecutionReport` — executor Protocol; no shell or network method.
- `Reviewer.review(manifest: TaskManifest, execution: ExecutionReport) -> ReviewReport` — separate read-only reviewer Protocol.
- `ExecutionReport(changed_paths: tuple[str, ...], patch: str, evidence: tuple[str, ...])`.
- `ReviewReport(accepted: bool, findings: tuple[str, ...])`.
- `TaskState`: `PLANNED`, `AUTHORIZED`, `EXECUTING`, `REVIEWING`, `ACCEPTED`, `REJECTED`, `BLOCKED`.
- `run_task(manifest, grant, executor, reviewer) -> TaskResult`.
- `TaskResult` contains final state, optional execution/review reports, stable error code when blocked, and immutable audit events.

- [ ] **Step 1: Write failing lifecycle tests**
  - `test_executor_is_not_called_without_a_valid_grant`: invalid authorization returns BLOCKED and executor call count remains zero.
  - `test_successful_task_requires_a_separate_reviewer_acceptance`: executor result alone cannot produce ACCEPTED; reviewer must explicitly accept.
  - `test_reviewer_rejection_produces_rejected_state`: a false reviewer verdict produces REJECTED.
  - `test_out_of_scope_executor_result_is_blocked_before_review`: invalid changed path prevents reviewer invocation.
  - `test_executor_exception_is_fail_closed`: executor exceptions produce BLOCKED with a sanitized error code.
  - `test_reviewer_exception_is_fail_closed`: reviewer exceptions never produce ACCEPTED.
  - `test_audit_events_follow_the_actual_state_transitions`: event sequence is deterministic and ends in the real terminal state.

- [ ] **Step 2: Run the focused tests and verify RED**
  - Run: `python -m unittest discover -s android-bridge/tests -p 'test_nyx_orchestration.py' -v`.
  - Expected: FAIL because the controller and lifecycle types do not yet exist.

- [ ] **Step 3: Implement the minimum controller**
  - Validate manifest and grant before invoking an adapter.
  - Record state transitions in order: PLANNED → AUTHORIZED → EXECUTING → REVIEWING → ACCEPTED/REJECTED.
  - On policy or adapter failure, stop immediately in BLOCKED; do not run later stages.
  - Validate all reported changed paths before the reviewer sees the report.
  - Require a non-empty patch and evidence before entering REVIEWING.
  - Never infer acceptance from executor success, absence of findings, or an exception.
  - Do not store raw environment data, credentials, arbitrary adapter payloads, or full exception strings in the audit log.

- [ ] **Step 4: Run focused tests and verify GREEN**
  - Run: `python -m unittest discover -s android-bridge/tests -p 'test_nyx_orchestration.py' -v`.
  - Expected: all Task 1 and Task 2 tests pass.

- [ ] **Step 5: Commit the task**
  - Commit message: `feat: add fail-closed executor reviewer lifecycle`.

## Task 3: Document the offline proof and run the full regression suite

**Files:**
- Create: `android-bridge/docs/770-ORCHESTRATION-POC.md`
- Test: `android-bridge/tests/test_nyx_orchestration.py` (existing coverage; no extra code unless a documentation claim is testable and incorrect)

- [ ] **Step 1: Write the documentation**
  - State explicitly that this is an in-process offline contract test, not a live agent session or sandbox.
  - Document the allowed roots, denied capabilities, task states, adapter interfaces, and test command.
  - Explain that reviewer separation is an architectural interface only; running an untrusted model requires OS/process-level isolation and tool authorization.
  - Note that no Copilot SDK is installed and no Copilot authentication, network use, 770 routing, Termux modifications, or production changes occur.
  - Record the separate future approval gate: select host (desktop/server/CI), confirm Copilot SDK runtime compatibility and plan/cost, and authorize authentication/provider access before a live run.

- [ ] **Step 2: Run the full Python regression suite**
  - Run: `python -m unittest discover -s android-bridge/tests -v`.
  - Expected: all new orchestration tests plus all existing runtime and chat tests pass.

- [ ] **Step 3: Inspect the final diff**
  - Confirm only the five planned new files changed.
  - Confirm no JARDIN, runtime, Android app, connector registry, secrets, dependencies, workflow, or deployment files changed.
  - Confirm no credentials, tokens, or personal data appear in the diff.

- [ ] **Step 4: Commit the documentation and regression-tested contract**
  - Commit message: `docs: describe offline agent orchestration proof`.

---

## Acceptance Criteria

- The task manifest and explicit grant are validated before an executor is called.
- All path checks fail closed and cannot be expanded by the caller.
- A separate reviewer verdict is mandatory for ACCEPTED.
- Executor/reviewer exceptions and out-of-scope changes fail closed.
- Audit events show the true state transitions without storing secrets.
- The new package uses only Python standard library.
- Existing 770 and chat tests remain green.
- The only changed files are the three package files, one test file, and one documentation file.
- No live agent provider, shell, network, Android bridge, Termux, deployment, or persistent memory is touched.

## Explicit Limitations

This plan deliberately proves only the orchestration contract. It does **not** prove real agent dispatch, workspace isolation, process sandboxing, Copilot SDK compatibility on Android, or model quality. An actual Copilot SDK adapter must be designed and tested on a selected host. GitHub's current SDK documentation lists Python support, but that does not establish Termux/Android runtime support. Do not infer Android compatibility from Python API support.

## Approval Gate After This Plan

Before the live-agent phase, obtain explicit approval for:
1. the host environment (desktop, controlled server, or CI);
2. installing/running the Copilot SDK and its bundled CLI runtime;
3. authenticating with the GitHub/Copilot account and any associated usage/cost;
4. granting the executor/reviewer their exact read/write tools and workspace paths.

The offline contract can be implemented without those approvals. A real Copilot session cannot.
