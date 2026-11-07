package com.nyx.bridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    private LocalBridgeServer bridgeServer;
    private TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bridgeServer = new LocalBridgeServer();
        bridgeServer.start();

        text = new TextView(this);
        text.setText(
                "NYX Bridge 0.2\n\n"
                        + "Android ↔ Termux local HTTP bridge.\n"
                        + "Estado: " + bridgeServer.getStatus() + "\n\n"
                        + "Pruebas desde Termux:\n"
                        + "curl -i http://127.0.0.1:" + LocalBridgeServer.PORT + "/ping\n"
                        + "curl -i http://127.0.0.1:" + LocalBridgeServer.PORT + "/device_info");
        text.setTextSize(18);
        text.setPadding(32, 32, 32, 32);

        setContentView(text);
    }

    @Override
    protected void onDestroy() {
        if (bridgeServer != null) {
            bridgeServer.stop();
        }
        super.onDestroy();
    }
}
