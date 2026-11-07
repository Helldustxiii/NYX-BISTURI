package com.nyx.bridge;

/** Lightweight file-signature detection. Does not trust the sender's MIME type. */
final class ImageFormat {

    enum Kind {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp");

        final String mimeType;
        final String extension;

        Kind(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }
    }

    private ImageFormat() {
    }

    static Kind detect(byte[] header, int length) {
        if (header == null || length < 0 || length > header.length) {
            return null;
        }

        if (length >= 3
                && unsigned(header[0]) == 0xFF
                && unsigned(header[1]) == 0xD8
                && unsigned(header[2]) == 0xFF) {
            return Kind.JPEG;
        }

        if (length >= 8
                && unsigned(header[0]) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A) {
            return Kind.PNG;
        }

        if (length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P') {
            return Kind.WEBP;
        }

        return null;
    }

    private static int unsigned(byte value) {
        return value & 0xFF;
    }
}
