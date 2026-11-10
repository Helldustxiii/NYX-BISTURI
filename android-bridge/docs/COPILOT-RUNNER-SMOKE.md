# Isolated GitHub Copilot runner smoke test

## Purpose

This is the first live-provider gate for NYX/BISTURÍ. It checks that the GitHub Copilot SDK can authenticate and return one response from a disposable GitHub-hosted Linux runner.

It is deliberately **not** a coding agent test.

## Safety boundary

- Trigger: manual `workflow_dispatch` only. It does not run on pull requests, pushes, schedules, or deployments.
- Host: a fresh GitHub-hosted `ubuntu-latest` runner for each run.
- Branch: the job refuses to run unless the selected ref is `main`.
- SDK: Python package `github-copilot-sdk==1.0.29`, with its bundled runtime downloaded and checksum-verified by the SDK.
- Authentication: the workflow reads the repository secret `COPILOT_GITHUB_TOKEN`; it never prints or writes the token.
- Client mode: `empty`.
- Tools: explicit empty allowlist (`available_tools=[]`); no shell, file, network, Git, MCP, or custom tools are exposed to the agent.
- State: session memory and session store are disabled; runtime data is placed in a temporary directory.
- Workspace: the SDK runtime working directory is the temporary directory, not the checked-out repository.
- Result: only the fixed success marker is printed. No repository files are changed and no artifact is uploaded.

The runner itself needs network access to download the SDK runtime and contact Copilot. The model session receives no tools.

## Credential setup

After this workflow is merged into `main`, add a repository Actions secret:

1. Open the repository's **Settings**.
2. Open **Secrets and variables → Actions**.
3. Select **New repository secret**.
4. Name it exactly `COPILOT_GITHUB_TOKEN`.
5. Paste a GitHub Copilot-compatible token accepted by the SDK and save it.

Never put the token in source code, workflow YAML, an issue, a pull-request comment, or ChatGPT chat. Do not use a classic `ghp_` token; GitHub's SDK authentication documentation lists supported token types and environment-variable authentication.

## Run

After the workflow exists on `main` and the secret is configured:

1. Open **Actions → Copilot runner smoke test**.
2. Select **Run workflow** and choose `main`.
3. Inspect the job result.

A successful run prints `COPILOT_SMOKE_OK`. A failure does not authorize enabling tools or broadening permissions; diagnose authentication, plan eligibility, SDK/runtime compatibility, and usage allowance first.

## Cost and limits

The SDK uses Copilot usage allowance per prompt. This workflow sends one short prompt per manual run. Do not dispatch it repeatedly as a polling mechanism.

## Official references

- [GitHub Copilot SDK Python README](https://github.com/github/copilot-sdk/blob/main/python/README.md)
- [SDK authentication](https://docs.github.com/en/copilot/how-tos/copilot-sdk/auth/authenticate)
- [SDK multi-tenancy and empty mode](https://docs.github.com/en/copilot/how-tos/copilot-sdk/setup/multi-tenancy)
