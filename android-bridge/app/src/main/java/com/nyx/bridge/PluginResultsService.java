package com.nyx.bridge;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

public final class PluginResultsService extends IntentService {

    static final String EXTRA_EXECUTION_ID = "nyx.execution_id";
    static final String EXTRA_RESULT_ID = "nyx.result_id";
    static final String EXTRA_MODE = "nyx.mode";
    static final String ACTION_UPDATED = "com.nyx.bridge.CHAT_UPDATED";

    private static final String TERMUX_RESULT_BUNDLE = "result";
    private static final String TERMUX_STDOUT = "stdout";
    private static final String TERMUX_STDERR = "stderr";
    private static final String TERMUX_EXIT_CODE = "exitCode";
    private static final String TERMUX_ERROR = "err";
    private static final String TERMUX_ERROR_MESSAGE = "errmsg";

    public PluginResultsService() {
        super("NYX-Termux-Results");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String resultId = intent.getStringExtra(EXTRA_RESULT_ID);
        String mode = intent.getStringExtra(EXTRA_MODE);
        Bundle result = intent.getBundleExtra(TERMUX_RESULT_BUNDLE);
        if (result == null) {
            storeFailure(resultId, mode, "Termux no devolvió el paquete de resultados.");
            notifyUi();
            return;
        }

        String stdout = result.getString(TERMUX_STDOUT, "");
        String stderr = result.getString(TERMUX_STDERR, "");
        String internalError = result.getString(TERMUX_ERROR_MESSAGE, "");
        int exitCode = result.getInt(TERMUX_EXIT_CODE, 1);
        int errorCode = result.getInt(TERMUX_ERROR, Activity.RESULT_OK);

        if (errorCode != Activity.RESULT_OK) {
            String message = internalError.isEmpty()
                    ? "Termux no pudo iniciar el comando (código " + errorCode + ")."
                    : internalError;
            storeFailure(resultId, mode, message);
            notifyUi();
            return;
        }

        JSONObject payload = parse(stdout);
        boolean commandOk = exitCode == 0 && payload != null
                && payload.optBoolean("ok", false);

        if (mode != null && mode.startsWith("dropbox")) {
            if (commandOk) {
                ChatMessageStore.saveDropboxResult(this, payload.toString());
            } else {
                JSONObject failed = new JSONObject();
                try {
                    failed.put("ok", false);
                    failed.put("kind", "dropbox");
                    failed.put("error", errorText(payload, stderr, internalError, exitCode));
                } catch (JSONException ignored) {
                }
                ChatMessageStore.saveDropboxResult(this, failed.toString());
            }
        } else if ("status".equals(mode)) {
            if (commandOk) {
                ChatMessageStore.saveStatus(this, payload.toString());
            } else {
                String message = errorText(payload, stderr, internalError, exitCode);
                JSONObject failed = new JSONObject();
                try {
                    failed.put("ok", false);
                    failed.put("error", message);
                } catch (JSONException ignored) {
                }
                ChatMessageStore.saveStatus(this, failed.toString());
            }
        } else if (resultId != null) {
            if (commandOk) {
                ChatMessageStore.completeMessage(
                        this,
                        resultId,
                        payload.optString("text", "BISTURÍ devolvió una respuesta vacía."),
                        "done"
                );
            } else {
                ChatMessageStore.completeMessage(
                        this,
                        resultId,
                        errorText(payload, stderr, internalError, exitCode),
                        "error"
                );
            }
        }

        notifyUi();
    }

    private JSONObject parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(raw.trim());
        } catch (JSONException error) {
            return null;
        }
    }

    private String errorText(
            JSONObject payload,
            String stderr,
            String internalError,
            int exitCode) {
        if (payload != null) {
            String value = payload.optString("error", "");
            if (!value.isEmpty()) {
                return value;
            }
        }
        if (internalError != null && !internalError.isEmpty()) {
            return internalError;
        }
        if (stderr != null && !stderr.trim().isEmpty()) {
            return stderr.trim();
        }
        return "La petición terminó con error (código " + exitCode + ").";
    }

    private void storeFailure(String resultId, String mode, String message) {
        if (mode != null && mode.startsWith("dropbox")) {
            JSONObject failed = new JSONObject();
            try {
                failed.put("ok", false);
                failed.put("kind", "dropbox");
                failed.put("error", message);
            } catch (JSONException ignored) {
            }
            ChatMessageStore.saveDropboxResult(this, failed.toString());
        } else if ("status".equals(mode)) {
            JSONObject failed = new JSONObject();
            try {
                failed.put("ok", false);
                failed.put("error", message);
            } catch (JSONException ignored) {
            }
            ChatMessageStore.saveStatus(this, failed.toString());
        } else if (resultId != null) {
            ChatMessageStore.completeMessage(this, resultId, message, "error");
        }
    }

    private void notifyUi() {
        Intent updated = new Intent(ACTION_UPDATED);
        updated.setPackage(getPackageName());
        sendBroadcast(updated);
    }
}
