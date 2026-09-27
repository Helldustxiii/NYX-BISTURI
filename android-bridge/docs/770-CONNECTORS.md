# NYX 770 Connector Gateway

Fecha: 2026-09-27

## Arquitectura

~~~text
770 -> connector gateway -> connector registry
                       ├-> Android (local execution)
                       ├-> GitHub (ChatGPT/MCP)
                       ├-> Dropbox (ChatGPT/MCP)
                       ├-> Notion (ChatGPT/MCP)
                       ├-> Vercel (ChatGPT/MCP)
                       ├-> Basic Memory (ChatGPT/MCP)
                       ├-> Airtable (ChatGPT/MCP)
                       ├-> Granola (ChatGPT/MCP)
                       ├-> Automations (ChatGPT/MCP)
                       └-> OpenAI Platform (ChatGPT/MCP)
~~~

## Contrato

~~~text
770|ID|FROM|BISTURI|CONNECTOR|{"service":"github","action":"search","args":{...}}|OK
~~~

The registry validates service and action names. Local connectors execute in
Termux. MCP connectors are explicitly marked as requiring a relay.

## Importante

This does not claim that Termux can directly invoke ChatGPT's MCP servers.
There is currently no observable ChatGPT -> Termux live transport in the
environment. The gateway therefore returns mcp_relay_required for MCP-backed
services instead of pretending they executed.

## Seguridad

- No arbitrary shell execution.
- Explicit service allowlist.
- Explicit action allowlist.
- Unknown services/actions fail closed.
- Existing Android allowlist remains ping and device_info.
- Jardín is untouched.

## Examples

~~~text
770|TEST005|TERMUX|BISTURI|CONNECTOR|{"service":"android","action":"ping","args":{}}|OK
~~~

~~~text
770|TEST006|TERMUX|BISTURI|CONNECTOR|{"service":"github","action":"search","args":{"query":"NYX-BISTURI"}}|OK
~~~
