package com.nyx.bridge;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Receives Android share intents and imports images into the local NYX inbox. */
public final class ImageReceiverActivity extends Activity {

    private static final int MAX_IMAGES_PER_SHARE = 5;
    private static final String STATE_IMPORT_STARTED = "nyx.import_started";
    private static final String STATE_STATUS = "nyx.status";
    private static final ExecutorService IMPORT_EXECUTOR =
            Executors.newSingleThreadExecutor();

    private TextView statusView;
    private boolean importStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        statusView = new TextView(this);
        statusView.setTextSize(17);
        statusView.setPadding(32, 32, 32, 32);
        statusView.setText("NYX Bridge\n\nRecibiendo imagen…");

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(statusView);
        setContentView(scrollView);

        if (savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_IMPORT_STARTED, false)) {
            importStarted = true;
            showStatus(savedInstanceState.getString(
                    STATE_STATUS,
                    "La recepción ya se había iniciado. Consulta nyx-bridge inbox en Termux."
            ));
        } else {
            handleIncomingIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_IMPORT_STARTED, importStarted);
        if (statusView != null) {
            outState.putString(STATE_STATUS, statusView.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) {
            showStatus("No se recibió ninguna imagen.");
            return;
        }

        String action = intent.getAction();
        if (!Intent.ACTION_SEND.equals(action)
                && !Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            showStatus("Acción no compatible. Comparte una imagen con NYX Bridge.");
            return;
        }

        List<Uri> sources = collectUris(intent);
        if (sources.isEmpty()) {
            showStatus("El envío no contiene una imagen accesible.");
            return;
        }
        if (sources.size() > MAX_IMAGES_PER_SHARE) {
            showStatus("Límite: " + MAX_IMAGES_PER_SHARE
                    + " imágenes por envío. No se ha importado ninguna.");
            return;
        }

        importStarted = true;
        showStatus("NYX Bridge\n\nValidando y guardando "
                + sources.size() + " imagen(es)…");

        Context applicationContext = getApplicationContext();
        IMPORT_EXECUTOR.execute(() -> {
            StringBuilder report = new StringBuilder("NYX Bridge\n\n");
            int imported = 0;

            for (Uri source : sources) {
                try {
                    ImageInboxImporter.ImportResult result =
                            ImageInboxImporter.importImage(applicationContext, source);
                    imported++;
                    report.append("✓ ").append(result.fileName).append('\n')
                            .append("  ").append(result.width).append(" × ")
                            .append(result.height).append(" · ")
                            .append(formatBytes(result.byteCount)).append('\n');
                } catch (Exception error) {
                    report.append("✗ Imagen rechazada: ")
                            .append(safeError(error)).append('\n');
                }
            }

            report.append("\nGuardadas: ").append(imported)
                    .append(" de ").append(sources.size())
                    .append("\n\nDestino: Download/NYX-Inbox/")
                    .append("\n\nTermux: nyx-bridge inbox");

            final String finalReport = report.toString();
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    showStatus(finalReport);
                }
            });
        });
    }

    private List<Uri> collectUris(Intent intent) {
        Set<Uri> unique = new LinkedHashSet<>();
        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            Parcelable stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (stream instanceof Uri) {
                unique.add((Uri) stream);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Parcelable> streams =
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (streams != null) {
                for (Parcelable stream : streams) {
                    if (stream instanceof Uri) {
                        unique.add((Uri) stream);
                    }
                }
            }
        }

        ClipData clipData = intent.getClipData();
        if (clipData != null) {
            for (int index = 0; index < clipData.getItemCount(); index++) {
                Uri uri = clipData.getItemAt(index).getUri();
                if (uri != null) {
                    unique.add(uri);
                }
            }
        }

        return new ArrayList<>(unique);
    }

    private void showStatus(String message) {
        statusView.setText(message);
    }

    private String safeError(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return error.getClass().getSimpleName();
        }
        return message.replace('\n', ' ').replace('\r', ' ');
    }

    private String formatBytes(long bytes) {
        return String.format(java.util.Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
