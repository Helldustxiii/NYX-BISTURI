package com.nyx.bridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);
        text.setText("NYX Bridge");
        text.setTextSize(24);
        text.setPadding(32, 32, 32, 32);

        setContentView(text);
    }
}
