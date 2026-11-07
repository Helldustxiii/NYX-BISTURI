"""NYX 770 connector registry.

The registry distinguishes connectors that can execute locally in Termux from
ChatGPT/MCP connectors that require an external relay. It deliberately does not
pretend that an MCP connector is directly reachable from Termux.
"""

CONNECTORS = {
    "android": {
        "transport": "local",
        "status": "active",
        "actions": ("ping", "device_info"),
    },
    "github": {
        "transport": "mcp",
        "status": "available_via_chatgpt",
        "actions": ("search", "read", "write", "issues", "pull_requests"),
    },
    "dropbox": {
        "transport": "mcp",
        "status": "available_via_chatgpt",
        "actions": ("search", "read", "copy", "move", "upload", "download"),
    },
    "notion": {
        "transport": "mcp",
        "status": "available_via_chatgpt",
        "actions": ("search", "read", "write", "databases"),
    },
    "vercel": {
        "transport": "mcp",
        "status": "available_via_chatgpt",
        "actions": ("projects", "deployments", "logs", "domains", "environment"),
    },
    "basic_memory": {
        "transport": "mcp",
        "status": "available_via_chatgpt",
        "actions": ("search", "read", "write", "edit"),
    },
    "airtable": {
        "transport": "mcp",
        "status": "available_via_chatgpt",
        "actions": ("search", "read", "write", "schema"),
    },
    "granola": {
        "transport": "mcp",
        "status": "available_via_chatgpt",
        "actions": ("search", "read"),
    },
    "automations": {
        "transport": "mcp",
        "status": "available_via_chatgpt",
        "actions": ("create", "list", "update", "delete"),
    },
    "openai_platform": {
        "transport": "mcp",
        "status": "available_via_chatgpt",
        "actions": ("keys", "projects", "usage"),
    },
}

def get_connector(name):
    return CONNECTORS.get(name)

def list_connectors():
    return {
        name: {
            "transport": spec["transport"],
            "status": spec["status"],
            "actions": list(spec["actions"]),
        }
        for name, spec in CONNECTORS.items()
    }
