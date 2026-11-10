package com.nyx.bridge;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.concurrent.atomic.AtomicInteger;

final class TermuxCommandClient {

    static final String TERMUX_PACKAGE = "com.termux";
    static final String RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND";
    static final String TERMUX_HOME = "/data/data/com.termux/files/home";
    static final String DEFAULT_COMMAND_PATH =
            TERMUX_HOME + "/nyx-android-bridge-integrate/android-bridge/tools/nyx-chat";

    private static final String PREFS = "nyx_app_settings";
    private static final String KEY_COMMAND_PATH = "termux_command_path";
    private static final AtomicInteger EXECUTION_IDS =
            new AtomicInteger((int) (System.currentTimeMillis() & 0x3fffffff));

    private TermuxCommandClient() {
    }

    static boolean isTermuxInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(TERMUX_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException error) {
            return false;
        }
    }

    static boolean hasRunCommandPermission(Context context) {
        return context.checkSelfPermission(RUN_COMMAND_PERMISSION)
                == PackageManager.PERMISSION_GRANTED;
    }

    static String getCommandPath(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String path = preferences.getString(KEY_COMMAND_PATH, DEFAULT_COMMAND_PATH);
        return path == null || path.trim().isEmpty() ? DEFAULT_COMMAND_PATH : path.trim();
    }

    static void setCommandPath(Context context, String path) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_COMMAND_PATH, path.trim())
                .apply();
    }

    static void sendChat(Context context, String message, String replyId) {
        execute(context, new String[]{message}, replyId, "chat");
    }

    static void requestStatus(Context context) {
        execute(context, new String[]{"--status"}, "system-status", "status");
    }

    static void requestDropboxStatus(Context context) {
        execute(context, new String[]{"--dropbox-status"}, "dropbox-result", "dropbox-status");
    }

    static void requestDropboxList(Context context, String path) {
        execute(context, new String[]{"--dropbox-list", path}, "dropbox-result", "dropbox-list");
    }

    static void requestDropboxMore(Context context, String cursor) {
        execute(context, new String[]{"--dropbox-more", cursor}, "dropbox-result", "dropbox-more");
    }

    static void requestDropboxDownload(Context context, String path) {
        execute(context, new String[]{"--dropbox-download", path}, "dropbox-result", "dropbox-download");
    }

    private static void execute(
            Context context,
            String[] arguments,
            String resultId,
            String mode) {
        int executionId = EXECUTION_IDS.incrementAndGet();
        Intent resultIntent = new Intent(context, PluginResultsService.class);
        resultIntent.putExtra(PluginResultsService.EXTRA_EXECUTION_ID, executionId);
        resultIntent.putExtra(PluginResultsService.EXTRA_RESULT_ID, resultId);
        resultIntent.putExtra(PluginResultsService.EXTRA_MODE, mode);

        int pendingFlags = PendingIntent.FLAG_ONE_SHOT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Termux adds its result Bundle to this PendingIntent.
            pendingFlags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent resultPendingIntent = PendingIntent.getService(
                context,
                executionId,
                resultIntent,
                pendingFlags
        );

        Intent command = new Intent();
        command.setComponent(new ComponentName(
                TERMUX_PACKAGE,
                "com.termux.app.RunCommandService"
        ));
        command.setAction("com.termux.RUN_COMMAND");
        command.putExtra("com.termux.RUN_COMMAND_PATH", getCommandPath(context));
        command.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arguments);
        command.putExtra("com.termux.RUN_COMMAND_WORKDIR", TERMUX_HOME);
        command.putExtra("com.termux.RUN_COMMAND_RUNNER", "app-shell");
        command.putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL", "NYX · BISTURÍ");
        command.putExtra(
                "com.termux.RUN_COMMAND_COMMAND_DESCRIPTION",
                "Procesa una petición local mediante el protocolo 770 de NYX."
        );
        command.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", resultPendingIntent);

        context.startService(command);
    }
}
