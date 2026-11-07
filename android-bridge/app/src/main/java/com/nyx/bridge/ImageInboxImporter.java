package com.nyx.bridge;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Copies supported shared images into the public Downloads/NYX-Inbox directory.
 * The app does not upload, publish, or send the image over a network.
 */
final class ImageInboxImporter {

    static final long MAX_IMAGE_BYTES = 20L * 1024L * 1024L;
    static final int MAX_IMAGE_DIMENSION = 20000;
    static final long MAX_IMAGE_PIXELS = 120_000_000L;

    private static final int BUFFER_SIZE = 16 * 1024;
    private static final String RELATIVE_PATH =
            Environment.DIRECTORY_DOWNLOADS + "/NYX-Inbox/";

    private ImageInboxImporter() {
    }

    static ImportResult importImage(Context context, Uri source) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw new IOException("requires_android_10_or_newer");
        }
        if (source == null
                || !ContentResolver.SCHEME_CONTENT.equals(source.getScheme())) {
            throw new IOException("unsupported_image_uri");
        }

        ContentResolver resolver = context.getContentResolver();
        File stagingDirectory = new File(context.getCacheDir(), "nyx-image-staging");
        if (!stagingDirectory.isDirectory() && !stagingDirectory.mkdirs()) {
            throw new IOException("cannot_create_staging_directory");
        }

        File stagedFile = File.createTempFile("nyx-image-", ".tmp", stagingDirectory);
        try {
            InputStream incoming = resolver.openInputStream(source);
            if (incoming == null) {
                throw new IOException("image_source_unavailable");
            }

            long byteCount;
            try (InputStream input = incoming;
                 OutputStream output = new FileOutputStream(stagedFile)) {
                byteCount = copyLimited(input, output, MAX_IMAGE_BYTES);
            }

            if (byteCount == 0) {
                throw new IOException("empty_image");
            }

            ImageFormat.Kind format = detectFormat(stagedFile);
            if (format == null) {
                throw new IOException("unsupported_image_format");
            }

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(stagedFile.getAbsolutePath(), bounds);

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw new IOException("invalid_image_dimensions");
            }
            if (bounds.outWidth > MAX_IMAGE_DIMENSION
                    || bounds.outHeight > MAX_IMAGE_DIMENSION
                    || ((long) bounds.outWidth * (long) bounds.outHeight) > MAX_IMAGE_PIXELS) {
                throw new IOException("image_dimensions_exceed_limit");
            }

            String fileName = createFileName(format);
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri destination = resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
            );
            if (destination == null) {
                throw new IOException("cannot_create_inbox_entry");
            }

            boolean published = false;
            try {
                OutputStream destinationStream = resolver.openOutputStream(destination, "w");
                if (destinationStream == null) {
                    throw new IOException("cannot_open_inbox_entry");
                }

                try (InputStream input = new FileInputStream(stagedFile);
                     OutputStream output = destinationStream) {
                    copy(input, output);
                }

                ContentValues ready = new ContentValues();
                ready.put(MediaStore.MediaColumns.IS_PENDING, 0);
                if (resolver.update(destination, ready, null, null) < 1) {
                    throw new IOException("cannot_publish_inbox_entry");
                }
                published = true;
            } finally {
                if (!published) {
                    resolver.delete(destination, null, null);
                }
            }

            return new ImportResult(
                    fileName,
                    format.mimeType,
                    byteCount,
                    bounds.outWidth,
                    bounds.outHeight
            );
        } finally {
            // The only staged copy is temporary. The published copy lives in Downloads.
            //noinspection ResultOfMethodCallIgnored
            stagedFile.delete();
        }
    }

    private static ImageFormat.Kind detectFormat(File file) throws IOException {
        byte[] header = new byte[12];
        int length = 0;
        try (InputStream input = new FileInputStream(file)) {
            while (length < header.length) {
                int read = input.read(header, length, header.length - length);
                if (read < 0) {
                    break;
                }
                length += read;
            }
        }
        return ImageFormat.detect(header, length);
    }

    private static long copyLimited(InputStream input, OutputStream output, long limit)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (total + read > limit) {
                throw new IOException("image_exceeds_20_mb_limit");
            }
            output.write(buffer, 0, read);
            total += read;
        }
        output.flush();
        return total;
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        output.flush();
    }

    private static String createFileName(ImageFormat.Kind format) {
        String timestamp = new SimpleDateFormat(
                "yyyyMMdd-HHmmss",
                Locale.US
        ).format(new Date());
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "nyx-" + timestamp + "-" + suffix + "." + format.extension;
    }

    static final class ImportResult {
        final String fileName;
        final String mimeType;
        final long byteCount;
        final int width;
        final int height;

        ImportResult(String fileName, String mimeType, long byteCount, int width, int height) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.byteCount = byteCount;
            this.width = width;
            this.height = height;
        }
    }
}
