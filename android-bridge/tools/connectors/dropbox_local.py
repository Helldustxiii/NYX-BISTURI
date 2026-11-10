"""Read-only Dropbox connector for NYX/Termux.

Uses Dropbox OAuth authorization-code + PKCE with offline refresh tokens.
No app secret is used. Refresh tokens are stored in Termux's private home
directory with restrictive filesystem permissions.
"""
from __future__ import annotations

import base64
import hashlib
import json
import os
import secrets
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any

API = "https://api.dropboxapi.com/2"
CONTENT_API = "https://content.dropboxapi.com/2"
TOKEN_URL = "https://api.dropbox.com/oauth2/token"
AUTHORIZE_URL = "https://www.dropbox.com/oauth2/authorize"
MAX_IMAGE_BYTES = 20 * 1024 * 1024
ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}


def config_dir() -> Path:
    return Path.home() / ".config" / "nyx"


def token_path() -> Path:
    return config_dir() / "dropbox.json"


def _pkce_pair() -> tuple[str, str]:
    """Return an RFC 7636 verifier and its S256 URL-safe challenge."""
    verifier = secrets.token_urlsafe(64)
    challenge = base64.urlsafe_b64encode(
        hashlib.sha256(verifier.encode("ascii")).digest()
    ).rstrip(b"=").decode("ascii")
    return verifier, challenge


def _secure_write(path: Path, data: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    try:
        os.chmod(path.parent, 0o700)
    except OSError:
        pass
    temp = path.with_name(f".{path.name}.{secrets.token_hex(8)}.tmp")
    flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL
    flags |= getattr(os, "O_NOFOLLOW", 0)
    fd = os.open(str(temp), flags, 0o600)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as stream:
            json.dump(data, stream, ensure_ascii=False)
            stream.write("\n")
        os.replace(temp, path)
        os.chmod(path, 0o600)
    finally:
        try:
            temp.unlink()
        except FileNotFoundError:
            pass


def _load_config() -> dict[str, Any]:
    path = token_path()
    if not path.is_file():
        raise RuntimeError("dropbox_not_authorized; run: nyx-dropbox auth")
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        raise RuntimeError("dropbox_credentials_unreadable") from None
    if not isinstance(data, dict) or not data.get("app_key") or not data.get("refresh_token"):
        raise RuntimeError("dropbox_credentials_incomplete")
    if path.stat().st_mode & 0o077:
        raise RuntimeError("dropbox_credentials_permissions_too_open; expected_0600")
    return data


def _http_json(url: str, *, data: dict[str, Any] | None = None,
               headers: dict[str, str] | None = None,
               form: dict[str, str] | None = None) -> dict[str, Any]:
    body = None
    request_headers = dict(headers or {})
    if form is not None:
        body = urllib.parse.urlencode(form).encode("utf-8")
        request_headers["Content-Type"] = "application/x-www-form-urlencoded"
    elif data is not None:
        body = json.dumps(data).encode("utf-8")
        request_headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=body, headers=request_headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            raw = response.read(2 * 1024 * 1024)
    except urllib.error.HTTPError as error:
        detail = error.read(4096).decode("utf-8", "replace")
        try:
            parsed = json.loads(detail)
            if isinstance(parsed, dict):
                tag = parsed.get("error_summary") or parsed.get("error") or "http_error"
            else:
                tag = "http_error"
        except json.JSONDecodeError:
            tag = "http_error"
        raise RuntimeError(f"dropbox_http_{error.code}:{tag}") from None
    except (urllib.error.URLError, TimeoutError, OSError) as error:
        raise RuntimeError(f"dropbox_network_error:{type(error).__name__}") from None
    try:
        result = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        raise RuntimeError("dropbox_invalid_json_response") from None
    if not isinstance(result, dict):
        raise RuntimeError("dropbox_unexpected_response")
    return result


def _exchange_code(app_key: str, code: str, verifier: str) -> dict[str, Any]:
    return _http_json(TOKEN_URL, form={
        "code": code.strip(),
        "grant_type": "authorization_code",
        "client_id": app_key,
        "code_verifier": verifier,
    })


def authorize() -> str:
    """Interactive PKCE authorization. User copies the code shown by Dropbox."""
    app_key = os.environ.get("NYX_DROPBOX_APP_KEY", "").strip()
    if not app_key:
        raise RuntimeError("set_NYX_DROPBOX_APP_KEY_from_Dropbox_App_Console_first")
    verifier, challenge = _pkce_pair()
    params = {
        "client_id": app_key,
        "response_type": "code",
        "token_access_type": "offline",
        "code_challenge": challenge,
        "code_challenge_method": "S256",
        "scope": "files.metadata.read files.content.read",
    }
    url = AUTHORIZE_URL + "?" + urllib.parse.urlencode(params)
    print("Abre esta dirección y autoriza NYX con permisos de SOLO LECTURA:")
    print(url)
    print("\nCopia el código que Dropbox muestre al finalizar.")
    code = input("Código de autorización: ").strip()
    if not code:
        raise RuntimeError("authorization_code_empty")
    tokens = _exchange_code(app_key, code, verifier)
    refresh = tokens.get("refresh_token")
    if not isinstance(refresh, str) or not refresh:
        raise RuntimeError("dropbox_did_not_return_refresh_token; check_offline_access")
    saved = {
        "app_key": app_key,
        "refresh_token": refresh,
        "access_token": tokens.get("access_token", ""),
        "expires_at": int(time.time()) + int(tokens.get("expires_in", 14400)),
        "scope": tokens.get("scope", ""),
        "token_type": "Bearer",
    }
    _secure_write(token_path(), saved)
    return "Dropbox autorizado. Credenciales guardadas en el almacenamiento privado de Termux."


def _access_token(config: dict[str, Any], force_refresh: bool = False) -> str:
    if not force_refresh and config.get("access_token") and int(config.get("expires_at", 0)) > time.time() + 60:
        return str(config["access_token"])
    tokens = _http_json(TOKEN_URL, form={
        "grant_type": "refresh_token",
        "refresh_token": str(config["refresh_token"]),
        "client_id": str(config["app_key"]),
    })
    access = tokens.get("access_token")
    if not isinstance(access, str) or not access:
        raise RuntimeError("dropbox_refresh_failed")
    config["access_token"] = access
    config["expires_at"] = int(time.time()) + int(tokens.get("expires_in", 14400))
    _secure_write(token_path(), config)
    return access


def _rpc(endpoint: str, payload: dict[str, Any]) -> dict[str, Any]:
    config = _load_config()
    token = _access_token(config)
    try:
        return _http_json(f"{API}/{endpoint}", data=payload,
                          headers={"Authorization": f"Bearer {token}"})
    except RuntimeError as error:
        if not str(error).startswith("dropbox_http_401:"):
            raise
    token = _access_token(config, force_refresh=True)
    return _http_json(f"{API}/{endpoint}", data=payload,
                      headers={"Authorization": f"Bearer {token}"})


def list_folder(path: str = "", cursor: str | None = None) -> dict[str, Any]:
    if cursor:
        result = _rpc("files/list_folder/continue", {"cursor": cursor})
    else:
        normalized = path.strip()
        if normalized and not normalized.startswith("/"):
            normalized = "/" + normalized
        result = _rpc("files/list_folder", {
            "path": normalized,
            "recursive": False,
            "include_deleted": False,
            "limit": 100,
        })
    entries = []
    for item in result.get("entries", []):
        if not isinstance(item, dict):
            continue
        entries.append({
            "type": item.get(".tag"),
            "name": item.get("name"),
            "path": item.get("path_display"),
            "size": item.get("size"),
            "modified": item.get("server_modified"),
        })
    return {"entries": entries, "has_more": bool(result.get("has_more")),
            "cursor": result.get("cursor")}


def _image_signature(data: bytes) -> bool:
    return (
        data.startswith(b"\x89PNG\r\n\x1a\n")
        or data.startswith(b"\xff\xd8\xff")
        or (len(data) >= 12 and data[:4] == b"RIFF" and data[8:12] == b"WEBP")
    )


def download_image(path: str) -> dict[str, Any]:
    remote_path = path.strip()
    if not remote_path.startswith("/") or "\x00" in remote_path:
        raise ValueError("dropbox_path_must_be_an_absolute_path")
    suffix = Path(remote_path).suffix.lower()
    if suffix not in ALLOWED_EXTENSIONS:
        raise ValueError("unsupported_image_extension")
    config = _load_config()
    token = _access_token(config)
    headers = {
        "Authorization": f"Bearer {token}",
        "Dropbox-API-Arg": json.dumps({"path": remote_path}),
    }
    request = urllib.request.Request(
        f"{CONTENT_API}/files/download", headers=headers, method="POST"
    )
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            data = response.read(MAX_IMAGE_BYTES + 1)
    except urllib.error.HTTPError as error:
        if error.code == 401:
            token = _access_token(config, force_refresh=True)
            headers["Authorization"] = f"Bearer {token}"
            request = urllib.request.Request(
                f"{CONTENT_API}/files/download", headers=headers, method="POST"
            )
            with urllib.request.urlopen(request, timeout=60) as response:
                data = response.read(MAX_IMAGE_BYTES + 1)
        else:
            raise RuntimeError(f"dropbox_download_http_{error.code}") from None
    except (urllib.error.URLError, TimeoutError, OSError) as error:
        raise RuntimeError(f"dropbox_download_network_error:{type(error).__name__}") from None
    if len(data) > MAX_IMAGE_BYTES:
        raise ValueError("image_exceeds_20_mb_limit")
    if not _image_signature(data):
        raise ValueError("downloaded_file_is_not_a_supported_image")
    inbox = Path.home() / "NYX" / "Dropbox-Inbox"
    inbox.mkdir(parents=True, exist_ok=True, mode=0o700)
    safe_name = Path(remote_path).name.replace("/", "_").replace("\\", "_")
    target = inbox / safe_name
    if target.exists():
        target = inbox / f"{target.stem}-{int(time.time())}{target.suffix}"
    temp = target.with_suffix(target.suffix + ".tmp")
    fd = os.open(str(temp), os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    try:
        with os.fdopen(fd, "wb") as stream:
            stream.write(data)
        os.replace(temp, target)
        os.chmod(target, 0o600)
    finally:
        try:
            temp.unlink()
        except FileNotFoundError:
            pass
    return {"downloaded": True, "name": target.name, "path": str(target),
            "bytes": len(data), "mime": {
                ".png": "image/png", ".jpg": "image/jpeg",
                ".jpeg": "image/jpeg", ".webp": "image/webp"
            }[suffix]}


def call(action: str, args: dict[str, Any] | None = None) -> str:
    args = args or {}
    if action == "list":
        result = list_folder(str(args.get("path", "")), args.get("cursor"))
    elif action == "download":
        result = download_image(str(args.get("path", "")))
    elif action == "status":
        config = _load_config()
        result = {"credentials_present": True, "token_cached": bool(config.get("access_token")),
                  "token_expires_at": config.get("expires_at"),
                  "scope": config.get("scope", "")}
    else:
        raise ValueError("action_not_allowed")
    return json.dumps(result, ensure_ascii=False)
