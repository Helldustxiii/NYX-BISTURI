import base64
import hashlib
import importlib.util
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

MODULE = Path(__file__).resolve().parents[1] / "tools" / "connectors" / "dropbox_local.py"
SPEC = importlib.util.spec_from_file_location("dropbox_local", MODULE)
dropbox = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(dropbox)


class DropboxLocalTests(unittest.TestCase):
    def test_image_signatures(self):
        self.assertTrue(dropbox._image_signature(b"\x89PNG\r\n\x1a\nrest"))
        self.assertTrue(dropbox._image_signature(b"\xff\xd8\xffrest"))
        self.assertTrue(dropbox._image_signature(b"RIFFxxxxWEBPrest"))
        self.assertFalse(dropbox._image_signature(b"not an image"))

    def test_secure_write_uses_private_permissions(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / ".config" / "nyx" / "dropbox.json"
            with patch.object(dropbox, "token_path", return_value=path):
                dropbox._secure_write(path, {"refresh_token": "test"})
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)
            self.assertEqual(path.parent.stat().st_mode & 0o777, 0o700)

    def test_download_rejects_relative_path(self):
        with self.assertRaisesRegex(ValueError, "absolute_path"):
            dropbox.download_image("relative/file.png")

    def test_download_rejects_non_image_extension(self):
        with self.assertRaisesRegex(ValueError, "extension"):
            dropbox.download_image("/folder/file.pdf")

    def test_registry_action_is_read_only(self):
        from connectors.registry import get_connector
        spec = get_connector("dropbox")
        self.assertEqual(spec["transport"], "local")
        self.assertEqual(set(spec["actions"]), {"status", "list", "download"})


if __name__ == "__main__":
    unittest.main()
