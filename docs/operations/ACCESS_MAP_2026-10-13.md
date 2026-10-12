# NYX-BISTURÍ · Access map and continuity
**Audit date:** 2026-10-13  
**Status:** evidence-based snapshot; do not treat unverified capabilities as available.  
**Canonical companion record:** [NYX · Mapa de accesos y continuidad · 2026-10-13](https://app.notion.com/p/3f7d22aada0b81608f27f139b0375f44)

## Verified connection status

| System | Read | Write / execution | Current decision |
|---|---|---|---|
| Notion | Verified | Verified: audit page and connector rows were updated, then read back | Operational canonical record. AI search and some advanced operations depend on plan. |
| GitHub `Helldustxiii/NYX-BISTURI` | Verified | `admin/push/pull` reported; branch writes tested | Code/version history. Keep risky changes isolated from `main`. |
| Vercel `nyx-bisturi-bhow` | Verified | Deployment/config tools exist; no config changes made | Production deployment `READY` on `main`, commit `e2bba6a89b3aba625cc21399616a3f08bc59ecbb`. Latest observed diagnostic deployment is a preview with `target=null`, not production. API reports `live=false` while production shows `READY`; unresolved, not enough evidence to alter settings. SSO protection reports enabled for all domains except custom domains. |
| Dropbox | Metadata/listing verified | **Not verified** | Archive/restore only after a write-and-read-back test. GitHub → Dropbox automatic sync was not observed. |
| Basic Memory Cloud | Tool answers the status query | Blocked: trial ended | Do not use as the only durable memory until access is restored. |
| Termux / Android Bridge | Not directly inspected in this audit | Local access not verified | User-device read-only diagnosis required before changes. |

## Observed graph
- Notion and ChatGPT connect through the authenticated Notion connector.
- GitHub connects to Vercel. `main` is production; diagnostic branches can create previews.
- No automatic GitHub-to-Dropbox sync was observed.
- Basic Memory Cloud is a separate dependency and currently subscription-blocked.
- This session does not prove ChatGPT has persistent direct access to private files inside Android/Termux.

## Continuity fallback for plan limits
1. Record state and decisions in Notion while ordinary read/write remains available.
2. Keep code and critical machine-readable documentation in GitHub with commit history.
3. Keep a local repository copy on Termux and test offline readability. This local state remains unaudited.
4. Use Dropbox as a secondary archive only after a successful copy/upload and read-back verification.
5. Do not depend on Basic Memory Cloud until its access is restored or formally replaced.

## Persistent access / credential boundary
Never paste API keys, cookies, session tokens or passwords into chat, Notion, or the repository. Connected tools act as authenticated intermediaries; that does not make their private credentials exportable.

The safe future design is a user-controlled local broker on Android/Termux:
- secrets kept outside the repository with restrictive local permissions;
- per-service, least-privilege, revocable credentials where supported;
- a strict allowlist of permitted operations, not arbitrary shell execution;
- human approval for deletes, moves, permission changes, production edits and deployments;
- a local audit log of request, result and failure;
- read-only inventory first, then reversible write probes.

This is a design target, **not an implemented capability**. No credentials were created and no permissions were changed in this audit.

## Exact pending work
1. Dropbox write probe: a one-time copy to `/Nyx Ia/.nyx-write-test/NYX_BISTURI_Jardin_write_probe_2026-10-13.md` must be explicitly confirmed before execution, then the copied object must be read back and verified.
2. Termux: user-run, read-only diagnostics for repository status, local backups, bridge state and actual file permissions.
3. Vercel: investigate the `live=false` discrepancy only if users observe an access problem. Do not change settings based on that field alone.

**Control rule:** reading metadata is not proof of write permission; a READY preview is not production; a visible connector is not an exportable key.
