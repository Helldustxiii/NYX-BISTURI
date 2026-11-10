package com.nyx.bridge;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class ChatMessageStore {

    private static final String PREFS = "nyx_chat_store";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_STATUS = "system_status";
    private static final String KEY_DROPBOX_RESULT = "dropbox_result";

    private ChatMessageStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static synchronized JSONArray getMessages(Context context) {
        String raw = prefs(context).getString(KEY_MESSAGES, "[]");
        try {
            return new JSONArray(raw == null ? "[]" : raw);
        } catch (JSONException error) {
            return new JSONArray();
        }
    }

    static synchronized void addMessage(
            Context context,
            String id,
            String role,
            String text,
            String state) {
        JSONArray messages = getMessages(context);
        JSONObject message = new JSONObject();
        try {
            message.put("id", id);
            message.put("role", role);
            message.put("text", text);
            message.put("state", state);
            message.put("created_at", System.currentTimeMillis());
            messages.put(message);
            prefs(context).edit().putString(KEY_MESSAGES, messages.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    static synchronized void completeMessage(
            Context context,
            String id,
            String text,
            String state) {
        JSONArray current = getMessages(context);
        JSONArray updated = new JSONArray();
        try {
            for (int i = 0; i < current.length(); i++) {
                JSONObject item = current.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                if (id.equals(item.optString("id"))) {
                    JSONObject replacement = new JSONObject(item.toString());
                    replacement.put("text", text);
                    replacement.put("state", state);
                    updated.put(replacement);
                } else {
                    updated.put(item);
                }
            }
            prefs(context).edit().putString(KEY_MESSAGES, updated.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    static synchronized void clearMessages(Context context) {
        prefs(context).edit().putString(KEY_MESSAGES, "[]").apply();
    }

    static synchronized void saveStatus(Context context, String json) {
        prefs(context).edit().putString(KEY_STATUS, json).apply();
    }

    static synchronized JSONObject getStatus(Context context) {
        String raw = prefs(context).getString(KEY_STATUS, "{}");
        try {
            return new JSONObject(raw == null ? "{}" : raw);
        } catch (JSONException error) {
            return new JSONObject();
        }
    }

    static synchronized void saveDropboxResult(Context context, String json) {
        prefs(context).edit().putString(KEY_DROPBOX_RESULT, json).apply();
    }

    static synchronized JSONObject getDropboxResult(Context context) {
        String raw = prefs(context).getString(KEY_DROPBOX_RESULT, "{}");
        try {
            return new JSONObject(raw == null ? "{}" : raw);
        } catch (JSONException error) {
            return new JSONObject();
        }
    }
}
