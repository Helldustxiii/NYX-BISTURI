package com.nyx.bridge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

public class BridgeProvider extends ContentProvider {

    public static final String AUTHORITY = "com.nyx.bridge.provider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Bundle result = new Bundle();

        if ("ping".equals(method)) {
            result.putString("status", "ok");
            result.putString("response", "pong");
            result.putString("bridge_version", "0.1");
            result.putString("transport", "content_provider");
            return result;
        }

        if ("device_info".equals(method)) {
            result.putString("status", "ok");
            result.putString("manufacturer", Build.MANUFACTURER);
            result.putString("model", Build.MODEL);
            result.putInt("sdk_int", Build.VERSION.SDK_INT);
            result.putString("release", Build.VERSION.RELEASE);
            result.putString("package", requireContext().getPackageName());
            result.putString("bridge_version", "0.1");
            return result;
        }

        result.putString("status", "error");
        result.putString("error", "unknown_method");
        result.putString("method", method);
        return result;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.nyx.bridge";
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
        return 0;
    }
}
