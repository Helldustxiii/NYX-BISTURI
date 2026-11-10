# NYX 770: Offline Agent Orchestration Proof (Phase B1)

## Purpose

This document describes the first provider-neutral orchestration contract for NYX/BISTURÍ. It proves the controller's authorization and review lifecycle using deterministic in-process test doubles. It is not a live agent session, a process sandbox, or a production executor.

## What is implemented

The package lives in \`android-bridge/tools/nyx_orchestration/\` and uses only the Python standard library.

- **Manifest:** identifies the task, objective, repository, base ref, and allowed roots.
- **Grant:** binds authorization to one task ID and a subset of the manifest's roots.
- **Policy:** permits only \`android-bridge/docs/\` and \`android-bridge/tests/\`; rejects traversal, absolute paths, Windows separators, protected paths, and scope expansion.
- **Executor protocol:** returns a declarative execution report. It has no shell, network, write, push, merge, or deploy method.
- **Reviewer protocol:** receives the manifest and report separately and must return an explicit Boolean verdict.
- **Controller:** moves through PLANNED → AUTHORIZED → EXECUTING → REVIEWING → ACCEPTED/REJECTED. Policy violations and adapter failures terminate in BLOCKED.
- **Audit:** records deterministic state and stable error codes without copying exception strings or environment data.

The controller never applies a patch. It validates the paths reported by the executor, but this is a contract check, not a filesystem sandbox.

## Run the tests

From the repository root:

\`\`\`bash
python -m unittest discover -s android-bridge/tests -p 'test_nyx_orchestration.py' -v
\`\`\`

Run the full Android bridge Python regression suite:

\`\`\`bash
python -m unittest discover -s android-bridge/tests -v
\`\`\`

## Explicit limitations

1. The executor and reviewer in the tests are local fake adapters. No model has been dispatched.
2. Separate Python protocols do not create process isolation. An untrusted model or tool must later run behind OS-level process/container isolation and explicit tool authorization.
3. The implementation does not connect to protocol 770 or its FIFO. The 770 runtime remains a single-reader transport and is not an agent execution bus.
4. No Copilot SDK/CLI is installed, and no GitHub/Copilot authentication or provider usage is performed.
5. No Termux, Android application, connector registry, credential, secret, deployment configuration, or persistent memory is changed.
6. No patch is written to disk, no Git push is performed by the executor, and no merge/deployment is initiated.

## Next approval gate: live provider execution

Before adding a real Copilot adapter, explicitly decide:

- **Host:** desktop, controlled server, or CI. Python SDK support alone does not establish compatibility with Android/Termux or its bundled runtime.
- **Runtime installation:** whether to install and run the Copilot SDK and its CLI/runtime components.
- **Authentication and cost:** whether to authenticate with the GitHub/Copilot account and accept any applicable plan limits or usage charges.
- **Tool boundary:** the exact workspace, read/write paths, network policy, and reviewer independence.

Until that approval, Phase B1 remains offline and provider-neutral.
