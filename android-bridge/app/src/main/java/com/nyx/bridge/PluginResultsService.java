package com.nyx.bridge;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONArray;
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
                JSONObject dropboxResult = "dropbox-more".equals(mode)
                        ? mergeDropboxPage(ChatMessageStore.getDropboxResult(this), payload)
                        : payload;
                ChatMessageStore.saveDropboxResult(this, dropboxResult.toString());
            } else if ("dropbox-more".equals(mode)) {
                JSONObject previous = ChatMessageStore.getDropboxResult(this);
                try {
                    previous.put("pagination_error",
                            errorText(payload, stderr, internalError, exitCode));
                    ChatMessageStore.saveDropboxResult(this, previous.toString());
                } catch (JSONException ignored) {
                    ChatMessageStore.saveDropboxResult(this, payload == null ? "{}" : payload.toString());
                }
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

    private JSONObject mergeDropboxPage(JSONObject previous, JSONObject next) {
        if (previous == null || !"list".equals(previous.optString("action"))) {
            return next;
        }
        JSONObject oldData = previous.optJSONObject("data");
        JSONObject newData = next.optJSONObject("data");
        JSONObject oldArgs = previous.optJSONObject("args");
        JSONObject nextArgs = next.optJSONObject("args");
        if (oldData == null || newData == null || oldArgs == null || nextArgs == null) {
            return withPaginationError(previous,
                    "No se pudo validar la carpeta del siguiente bloque. Vuelve a listar.");
        }

        String currentPath = oldArgs.optString("path", "");
        String expectedPath = nextArgs.optString("expected_path", "");
        String currentCursor = oldData.optString("cursor", "");
        String requestedCursor = nextArgs.optString("cursor", "");
        if (!currentPath.equals(expectedPath) || currentCursor.isEmpty()
                || !currentCursor.equals(requestedCursor)) {
            return withPaginationError(previous,
                    "La carpeta o el cursor cambió mientras se cargaba. Vuelve a listar la carpeta.");
        }

        JSONArray oldEntries = oldData.optJSONArray("entries");
        JSONArray newEntries = newData.optJSONArray("entries");
        JSONArray merged = new JSONArray();
        if (oldEntries != null) {
            for (int i = 0; i < oldEntries.length(); i++) {
                JSONObject entry = oldEntries.optJSONObject(i);
                if (entry != null) {
                    merged.put(entry);
                }
            }
        }
        if (newEntries != null) {
            for (int i = 0; i < newEntries.length(); i++) {
                JSONObject entry = newEntries.optJSONObject(i);
                if (entry != null) {
                    merged.put(entry);
                }
            }
        }
        try {
            newData.put("entries", merged);
            JSONObject combined = new JSONObject(next.toString());
            combined.put("data", newData);
            combined.put("action", "list");
            combined.put("args", oldArgs);
            combined.remove("pagination_error");
            return combined;
        } catch (JSONException error) {
            return withPaginationError(previous,
                    "No se pudo combinar la página de Dropbox. Vuelve a listar la carpeta.");
        }
    }

    private JSONObject withPaginationError(JSONObject source, String message) {
        try {
            JSONObject result = new JSONObject(source.toString());
            result.put("pagination_error", message);
            return result;
        } catch (JSONException error) {
            return source;
        }
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
        if ("dropbox-more".equals(mode)) {
            JSONObject previous = ChatMessageStore.getDropboxResult(this);
            try {
                previous.put("pagination_error", message);
                ChatMessageStore.saveDropboxResult(this, previous.toString());
            } catch (JSONException ignored) {
                ChatMessageStore.saveDropboxResult(this, "{}");
            }
        } else if (mode != null && mode.startsWith("dropbox")) {
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
