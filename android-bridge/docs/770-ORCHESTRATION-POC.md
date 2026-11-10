# NYX 770: Offline Agent Orchestration Proof (Phase B1)

## Purpose

This document describes the first provider-neutral orchestration contract for NYX/BISTURÍ. It proves the controller's authorization and review lifecycle using deterministic in-process test doubles. It is not a live agent session, a process sandbox, or a production executor.

## What is implemented

The package lives in `android-bridge/tools/nyx_orchestration/` and uses only the Python standard library.

- **Manifest:** identifies the task, objective, repository, base ref, and allowed roots.
- **Grant:** binds authorization to one task ID and a subset of the manifest's roots.
- **Policy:** permits only `android-bridge/docs/` and `android-bridge/tests/`; rejects traversal, absolute paths, Windows separators, protected paths, and scope expansion.
- **Executor protocol:** returns a declarative execution report. It has no shell, network, write, push, merge, or deploy method.
- **Reviewer protocol:** receives the manifest and report separately and must return an explicit Boolean verdict.
- **Controller:** moves through PLANNED → AUTHORIZED → EXECUTING → REVIEWING → ACCEPTED/REJECTED. Policy violations and adapter failures terminate in BLOCKED.
- **Audit:** records deterministic state and stable error codes without copying exception strings or environment data.

The controller never applies a patch. It validates both the executor's reported paths and the paths encoded in each `diff --git` header, requires the two sets to match, rejects renames in this first phase, and caps the patch at 1 MiB. This catches a report that claims to modify an allowed file while embedding an out-of-scope file in its patch. It remains a contract check, not a filesystem sandbox.

## Run the tests

From the repository root:

```bash
python -m unittest discover -s android-bridge/tests -p 'test_nyx_orchestration.py' -v
```

Run the full Android bridge Python regression suite:

```bash
python -m unittest discover -s android-bridge/tests -v
```

## Explicit limitations

1. The executor and reviewer in the tests are local fake adapters. No model has been dispatched.
2. Separate Python protocols do not create process isolation. An untrusted model or tool must later run behind OS-level process/container isolation and explicit tool authorization.
3. The implementation does not connect to protocol 770 or its FIFO. The 770 runtime remains a single-reader transport and is not an agent execution bus.
4. No Copilot SDK/CLI is installed, and no GitHub/Copilot authentication or provider usage is performed.
5. No Termux, Android application, connector registry, credential, secret, deployment configuration, or persistent memory is changed.
6. No patch is written to disk, no Git push is performed by the executor, and no merge/deployment is initiated.

## Security requirements for a future Copilot adapter

The official Copilot SDK supports custom agents with per-agent tool allowlists. The future adapter must:

- Start from `mode="empty"` rather than inheriting ambient CLI defaults.
- Expose only explicitly registered application-owned tools through a session allowlist.
- Give the executor only the task workspace and its effective grant; give the reviewer read-only tools and a separate context.
- Use a custom permission handler and pre-tool policy checks. Never use `approve_all` as the default for this orchestrator.
- Keep shell, network, Git push, merge, deployment, and credential access denied unless a later task-specific authorization explicitly grants a narrowly defined operation.

References: [Copilot SDK custom agents and tool scoping](https://docs.github.com/en/copilot/how-tos/copilot-sdk/features/custom-agents), [multi-tenancy and the empty mode](https://docs.github.com/en/copilot/how-tos/copilot-sdk/setup/multi-tenancy), and [pre-tool use permission hooks](https://docs.github.com/en/copilot/how-tos/copilot-sdk/features/hooks).

## Next approval gate: live provider execution

Before adding a real Copilot adapter, explicitly decide:

- **Host:** desktop, controlled server, or CI. Python SDK support alone does not establish compatibility with Android/Termux or its bundled runtime. The official Python SDK README currently lists Linux, macOS, and Windows runtime cache paths, but no Android platform entry; therefore Android/Termux support must not be assumed ([official Python SDK README](https://github.com/github/copilot-sdk/blob/main/python/README.md#runtime)).
- **Runtime installation:** whether to install and run the Copilot SDK and its CLI/runtime components.
- **Authentication and cost:** whether to authenticate with the GitHub/Copilot account and accept any applicable plan limits or usage charges.
- **Tool boundary:** the exact workspace, read/write paths, network policy, and reviewer independence.

Until that approval, Phase B1 remains offline and provider-neutral.
