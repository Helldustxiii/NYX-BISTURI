package com.nyx.bridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);
        text.setText(
                "NYX Bridge 0.1\n\n"
                        + "Android ↔ Termux local bridge activo.\n\n"
                        + "Prueba desde Termux:\n"
                        + "content call --uri content://"
                        + BridgeProvider.AUTHORITY
                        + " --method ping\n\n"
                        + "Información del dispositivo:\n"
                        + "content call --uri content://"
                        + BridgeProvider.AUTHORITY
                        + " --method device_info");
        text.setTextSize(18);
        text.setPadding(32, 32, 32, 32);

        setContentView(text);
    }
}
