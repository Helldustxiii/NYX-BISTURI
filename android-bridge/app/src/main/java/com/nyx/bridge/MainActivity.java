package com.nyx.bridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private static final int REQUEST_TERMUX_PERMISSION = 770;
    private static final int BG = Color.rgb(12, 10, 16);
    private static final int PANEL = Color.rgb(27, 23, 34);
    private static final int PANEL_LIGHT = Color.rgb(39, 31, 49);
    private static final int TEXT = Color.rgb(241, 235, 248);
    private static final int MUTED = Color.rgb(166, 153, 180);
    private static final int ACCENT = Color.rgb(155, 123, 193);
    private static final int ACCENT_DARK = Color.rgb(61, 43, 80);
    private static final int GREEN = Color.rgb(119, 202, 149);
    private static final int RED = Color.rgb(230, 126, 126);

    private LocalBridgeServer bridgeServer;
    private LinearLayout root;
    private FrameLayout content;
    private String currentTab = "chat";
    private EditText composer;
    private EditText dropboxPathInput;
    private String dropboxPath = "/Nyx Ia";
    private boolean receiverRegistered;

    private final BroadcastReceiver updates = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && PluginResultsService.ACTION_UPDATED.equals(intent.getAction())) {
                renderCurrentTab();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        bridgeServer = new LocalBridgeServer();
        bridgeServer.start();

        buildShell();
        renderCurrentTab();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter(PluginResultsService.ACTION_UPDATED);
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(updates, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(updates, filter);
            }
            receiverRegistered = true;
        }
        // Termux may finish while NYX is backgrounded; reload persisted results on return.
        renderCurrentTab();
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            unregisterReceiver(updates);
            receiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (bridgeServer != null) {
            bridgeServer.stop();
        }
        super.onDestroy();
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(16), dp(20), dp(13));
        header.setBackgroundColor(Color.rgb(17, 14, 22));

        TextView sigil = new TextView(this);
        sigil.setText("🜏");
        sigil.setTextSize(26);
        sigil.setTextColor(ACCENT);
        header.addView(sigil, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = label("NYX", 20, TEXT, true);
        titleBox.addView(title);
        TextView subtitle = label("BISTURÍ · NÚCLEO LOCAL", 10, MUTED, true);
        titleBox.addView(subtitle);
        header.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));

        TextView version = label("770", 12, ACCENT, true);
        version.setPadding(dp(10), dp(6), dp(10), dp(6));
        version.setBackground(round(ACCENT_DARK, dp(12)));
        header.addView(version);

        root.addView(header);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1
        ));

        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setPadding(dp(8), dp(8), dp(8), dp(8));
        navigation.setBackgroundColor(Color.rgb(17, 14, 22));

        navigation.addView(navButton("Chat", "chat"), weighted());
        navigation.addView(navButton("Dropbox", "dropbox"), weighted());
        navigation.addView(navButton("Sistema", "system"), weighted());
        navigation.addView(navButton("Ajustes", "settings"), weighted());
        root.addView(navigation);
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, dp(48), 1);
    }

    private TextView navButton(String text, String tab) {
        TextView button = label(text, 12, currentTab.equals(tab) ? TEXT : MUTED, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(round(
                currentTab.equals(tab) ? ACCENT_DARK : Color.TRANSPARENT,
                dp(14)
        ));
        button.setOnClickListener(view -> {
            currentTab = tab;
            buildShell();
            renderCurrentTab();
        });
        return button;
    }

    private void renderCurrentTab() {
        if (content == null) {
            return;
        }
        content.removeAllViews();
        if ("dropbox".equals(currentTab)) {
            content.addView(createDropboxScreen());
        } else if ("system".equals(currentTab)) {
            content.addView(createSystemScreen());
        } else if ("settings".equals(currentTab)) {
            content.addView(createSettingsScreen());
        } else {
            content.addView(createChatScreen());
        }
    }

    private View createChatScreen() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), dp(10));

        TextView heading = label("Conversación local", 17, TEXT, true);
        page.addView(heading);
        TextView hint = label("Tus mensajes viajan al Runtime 770 y regresan desde Qwen.", 11, MUTED, false);
        page.addView(hint);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(0, dp(14), 0, dp(14));

        JSONArray history = ChatMessageStore.getMessages(this);
        if (history.length() == 0) {
            messages.addView(welcomeCard());
        } else {
            for (int i = 0; i < history.length(); i++) {
                JSONObject item = history.optJSONObject(i);
                if (item != null) {
                    messages.addView(messageRow(item));
                }
            }
        }
        scroll.addView(messages);
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1
        ));

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setGravity(Gravity.BOTTOM);
        inputRow.setPadding(dp(4), dp(8), dp(4), dp(4));

        composer = new EditText(this);
        composer.setTextColor(TEXT);
        composer.setHintTextColor(MUTED);
        composer.setHint("Escribe a BISTURÍ…");
        composer.setTextSize(15);
        composer.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        composer.setMinLines(1);
        composer.setMaxLines(4);
        composer.setPadding(dp(15), dp(12), dp(15), dp(12));
        composer.setBackground(round(PANEL, dp(18)));
        inputRow.addView(composer, new LinearLayout.LayoutParams(0, -2, 1));

        Button send = actionButton("Enviar");
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(
                dp(84), dp(48)
        );
        sendParams.leftMargin = dp(8);
        inputRow.addView(send, sendParams);
        send.setOnClickListener(view -> sendMessage());

        page.addView(inputRow);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        return page;
    }

    private View welcomeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(round(PANEL, dp(20)));

        TextView title = label("BISTURÍ está preparado.", 16, TEXT, true);
        card.addView(title);
        TextView description = label(
                "Esta conversación se procesa en tu teléfono con el modelo local. "
                        + "El historial queda guardado en esta aplicación.",
                13,
                MUTED,
                false
        );
        description.setPadding(0, dp(8), 0, 0);
        card.addView(description);
        return card;
    }

    private View messageRow(JSONObject item) {
        boolean user = "user".equals(item.optString("role"));
        String state = item.optString("state", "done");
        LinearLayout row = new LinearLayout(this);
        row.setGravity(user ? Gravity.END : Gravity.START);
        row.setPadding(dp(2), dp(5), dp(2), dp(5));

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setBackground(round(
                user ? ACCENT_DARK : PANEL_LIGHT,
                dp(17)
        ));

        TextView who = label(user ? "TÚ" : "BISTURÍ", 9, user ? Color.rgb(220, 200, 245) : ACCENT, true);
        bubble.addView(who);

        String contentText = item.optString("text", "");
        if ("pending".equals(state)) {
            contentText = "Procesando en Qwen…";
        }
        TextView body = label(contentText, 14, TEXT, false);
        body.setPadding(0, dp(5), 0, 0);
        bubble.addView(body);

        if ("error".equals(state)) {
            TextView error = label("No se pudo completar", 9, RED, true);
            error.setPadding(0, dp(7), 0, 0);
            bubble.addView(error);
        }

        row.addView(bubble, new LinearLayout.LayoutParams(
                Math.min(getResources().getDisplayMetrics().widthPixels - dp(70), dp(360)),
                -2
        ));
        return row;
    }

    private void sendMessage() {
        if (composer == null) {
            return;
        }
        String message = composer.getText().toString().trim();
        if (message.isEmpty()) {
            return;
        }
        if (!ensureTermuxReady()) {
            return;
        }

        String replyId = "assistant-" + java.util.UUID.randomUUID();
        ChatMessageStore.addMessage(this, "user-" + java.util.UUID.randomUUID(),
                "user", message, "done");
        ChatMessageStore.addMessage(this, replyId, "assistant",
                "Procesando en Qwen…", "pending");
        composer.setText("");

        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(composer.getWindowToken(), 0);
        }
        renderCurrentTab();

        try {
            TermuxCommandClient.sendChat(this, message, replyId);
        } catch (Exception error) {
            ChatMessageStore.completeMessage(this, replyId,
                    "No se pudo iniciar Termux: " + error.getMessage(), "error");
            renderCurrentTab();
        }
    }

    private View createDropboxScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(14), dp(16), dp(24));

        page.addView(label("Dropbox · biblioteca local", 18, TEXT, true));
        page.addView(infoCard("Acceso de solo lectura",
                "NYX puede listar carpetas y descargar imágenes PNG, JPEG y WebP. "
                        + "No puede subir, borrar, mover ni compartir archivos. "
                        + "Las imágenes descargadas se guardan en ~/NYX/Dropbox-Inbox/."));

        Button status = secondaryButton("Comprobar autorización");
        LinearLayout.LayoutParams statusParams = fullWidthButton();
        statusParams.topMargin = dp(10);
        page.addView(status, statusParams);
        status.setOnClickListener(view -> requestDropboxStatus());

        page.addView(label("Carpeta de Dropbox", 13, TEXT, true));
        dropboxPathInput = new EditText(this);
        dropboxPathInput.setSingleLine(true);
        dropboxPathInput.setTextColor(TEXT);
        dropboxPathInput.setHintTextColor(MUTED);
        dropboxPathInput.setTextSize(13);
        dropboxPathInput.setHint("/Nyx Ia/Recursos Visuales");
        dropboxPathInput.setText(dropboxPath);
        dropboxPathInput.setPadding(dp(12), dp(11), dp(12), dp(11));
        dropboxPathInput.setBackground(round(PANEL, dp(12)));
        LinearLayout.LayoutParams pathParams = fullWidthButton();
        pathParams.topMargin = dp(7);
        page.addView(dropboxPathInput, pathParams);

        Button list = actionButton("Listar carpeta");
        LinearLayout.LayoutParams listParams = fullWidthButton();
        listParams.topMargin = dp(8);
        page.addView(list, listParams);
        list.setOnClickListener(view -> {
            String value = dropboxPathInput.getText().toString().trim();
            dropboxPath = value;
            requestDropboxList(value);
        });

        Button auth = secondaryButton("Copiar comando de autorización");
        LinearLayout.LayoutParams authParams = fullWidthButton();
        authParams.topMargin = dp(8);
        page.addView(auth, authParams);
        auth.setOnClickListener(view -> copyDropboxAuthorizationCommand());

        page.addView(infoCard("Primera conexión",
                "Crea una aplicación Dropbox con Full Dropbox, habilita únicamente "
                        + "files.metadata.read y files.content.read y activa Allow public clients. "
                        + "Después completa una sola vez el flujo OAuth PKCE desde Termux. "
                        + "No pegues aquí ni compartas el refresh token."));

        JSONObject result = ChatMessageStore.getDropboxResult(this);
        if (result.length() == 0 || result.optString("kind").isEmpty()) {
            page.addView(infoCard("Sin consultas todavía",
                    "Comprueba la autorización y después lista /Nyx Ia para navegar por tu biblioteca."));
        } else if (!result.optBoolean("ok", false)) {
            page.addView(infoCard("Dropbox no disponible",
                    result.optString("error", "No se pudo completar la operación.")));
        } else {
            String action = result.optString("action", "");
            JSONObject data = result.optJSONObject("data");
            if ("status".equals(action) && data != null) {
                page.addView(infoCard("Autorización correcta",
                        "Credenciales locales encontradas. Permisos: "
                                + data.optString("scope", "no indicados")
                                + ". El token se renueva en Termux cuando caduca."));
            } else if ("download".equals(action) && data != null) {
                page.addView(infoCard("Imagen descargada",
                        data.optString("name", "Archivo")
                                + "\n" + data.optString("path", "")
                                + "\n" + data.optLong("bytes", 0) + " bytes"));
            } else if ("list".equals(action) && data != null) {
                JSONArray entries = data.optJSONArray("entries");
                page.addView(label(
                        "Contenido · " + (entries == null ? 0 : entries.length()) + " elementos",
                        14, TEXT, true
                ));
                if (entries != null && entries.length() > 0) {
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject entry = entries.optJSONObject(i);
                        if (entry != null) {
                            page.addView(dropboxEntryRow(entry));
                        }
                    }
                } else {
                    page.addView(infoCard("Carpeta vacía", "Dropbox no devolvió elementos en esta carpeta."));
                }
                String nextCursor = data.optString("cursor", "");
                if (data.optBoolean("has_more", false) && !nextCursor.isEmpty()) {
                    Button more = actionButton("Cargar siguientes 100");
                    LinearLayout.LayoutParams moreParams = fullWidthButton();
                    moreParams.topMargin = dp(9);
                    page.addView(more, moreParams);
                    more.setOnClickListener(view -> requestDropboxMore(nextCursor));
                }
                String pageError = result.optString("pagination_error", "");
                if (!pageError.isEmpty()) {
                    page.addView(infoCard("No se pudo cargar la página siguiente", pageError));
                }
            }
        }

        scroll.addView(page);
        return scroll;
    }

    private View dropboxEntryRow(JSONObject entry) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(9), dp(9), dp(9));
        row.setBackground(round(PANEL, dp(13)));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, -2
        );
        rowParams.topMargin = dp(7);
        row.setLayoutParams(rowParams);

        String name = entry.optString("name", "Elemento");
        String type = entry.optString("type", "");
        String path = entry.optString("path", "");
        long size = entry.optLong("size", 0);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.addView(label(name, 12, TEXT, true));
        details.addView(label(
                "folder".equals(type) ? "Carpeta" : (size > 0 ? size + " bytes" : "Archivo"),
                10, MUTED, false
        ));
        row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

        if ("folder".equals(type)) {
            Button open = secondaryButton("Abrir");
            row.addView(open, new LinearLayout.LayoutParams(dp(76), dp(40)));
            open.setOnClickListener(view -> {
                dropboxPath = path;
                if (dropboxPathInput != null) {
                    dropboxPathInput.setText(path);
                }
                requestDropboxList(path);
            });
        } else if (isDropboxImage(path)) {
            Button download = actionButton("Descargar");
            row.addView(download, new LinearLayout.LayoutParams(dp(96), dp(40)));
            download.setOnClickListener(view -> requestDropboxDownload(path));
        }
        return row;
    }

    private boolean isDropboxImage(String path) {
        String lower = path == null ? "" : path.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg") || lower.endsWith(".webp");
    }

    private void requestDropboxStatus() {
        if (!ensureTermuxReady()) {
            return;
        }
        try {
            TermuxCommandClient.requestDropboxStatus(this);
            Toast.makeText(this, "Comprobando autorización de Dropbox…", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo consultar Dropbox: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void requestDropboxList(String path) {
        if (!ensureTermuxReady()) {
            return;
        }
        if (path == null || (!path.isEmpty() && !path.startsWith("/"))) {
            Toast.makeText(this, "La ruta debe ser absoluta en Dropbox, por ejemplo /Nyx Ia.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        try {
            TermuxCommandClient.requestDropboxList(this, path);
            Toast.makeText(this, "Consultando carpeta de Dropbox…", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo listar la carpeta: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void requestDropboxMore(String cursor) {
        if (!ensureTermuxReady()) {
            return;
        }
        if (cursor == null || cursor.isEmpty()) {
            Toast.makeText(this, "Dropbox no devolvió un cursor de paginación.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        try {
            TermuxCommandClient.requestDropboxMore(this, cursor);
            Toast.makeText(this, "Cargando los siguientes 100 elementos…",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo continuar el listado: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void requestDropboxDownload(String path) {
        if (!ensureTermuxReady()) {
            return;
        }
        if (path == null || !path.startsWith("/") || !isDropboxImage(path)) {
            Toast.makeText(this, "Solo se pueden descargar imágenes PNG, JPEG o WebP.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        try {
            TermuxCommandClient.requestDropboxDownload(this, path);
            Toast.makeText(this, "Descargando imagen en NYX/Dropbox-Inbox…", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo descargar la imagen: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void copyDropboxAuthorizationCommand() {
        String command = "export NYX_DROPBOX_APP_KEY=\"APP_KEY_DE_DROPBOX\"\n"
                + "python \"$HOME/nyx-android-bridge-integrate/android-bridge/tools/nyx-dropbox\" auth";
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("NYX Dropbox OAuth PKCE", command));
            Toast.makeText(this,
                    "Comando copiado. Sustituye APP_KEY_DE_DROPBOX y ejecútalo en Termux.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private View createSystemScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(16), dp(16), dp(24));

        page.addView(label("Estado del sistema", 18, TEXT, true));
        page.addView(label("Lectura ejecutada desde Termux, no estimaciones de la interfaz.", 11, MUTED, false));

        JSONObject status = ChatMessageStore.getStatus(this);
        JSONObject checks = status.optJSONObject("checks");
        if (checks == null) {
            checks = new JSONObject();
        }

        page.addView(statusCard("Runtime 770", checks.optBoolean("runtime", false),
                "Proceso lector único del canal de entrada."));
        page.addView(statusCard("Qwen · servidor local", checks.optBoolean("qwen", false),
                "Comprobación HTTP en 127.0.0.1:8080/health."));
        page.addView(statusCard("Canales FIFO", checks.optBoolean("input_fifo", false)
                        && checks.optBoolean("output_fifo", false),
                "Entrada y salida existen y son tuberías FIFO."));
        page.addView(statusCard("Termux: acceso externo", checks.optBoolean("allow_external_apps", false),
                "La propiedad allow-external-apps debe estar activada."));

        String error = status.optString("error", "");
        if (!error.isEmpty()) {
            page.addView(infoCard("Diagnóstico", error));
        }

        Button refresh = actionButton("Comprobar sistema");
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        );
        refreshParams.topMargin = dp(12);
        page.addView(refresh, refreshParams);
        refresh.setOnClickListener(view -> refreshSystemStatus());

        page.addView(infoCard("Importante",
                "Verde significa que la comprobación respondió correctamente en el último diagnóstico. "
                        + "No garantiza que una petición futura nunca falle."));
        scroll.addView(page);
        return scroll;
    }

    private View statusCard(String title, boolean ok, String description) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(13), dp(15), dp(13));
        card.setBackground(round(PANEL, dp(16)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, -2
        );
        params.topMargin = dp(10);
        card.setLayoutParams(params);

        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = label(title, 14, TEXT, true);
        line.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView state = label(ok ? "● ACTIVO" : "● SIN CONFIRMAR", 10,
                ok ? GREEN : MUTED, true);
        line.addView(state);
        card.addView(line);

        TextView detail = label(description, 11, MUTED, false);
        detail.setPadding(0, dp(6), 0, 0);
        card.addView(detail);
        return card;
    }

    private View createSettingsScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(16), dp(16), dp(24));

        page.addView(label("Configuración", 18, TEXT, true));
        page.addView(infoCard("Conexión requerida",
                "NYX utiliza la API oficial RUN_COMMAND de Termux. No necesita ADB para enviar cada mensaje. "
                        + "Termux ejecuta un único script autorizado y devuelve su resultado."));
        page.addView(infoCard("1 · Permiso Android",
                "Concede a NYX el permiso «Run commands in Termux environment». "
                        + "Android puede mostrarlo dentro de los permisos adicionales de la aplicación."));
        Button permissions = actionButton("Abrir permisos de NYX");
        page.addView(permissions, fullWidthButton());
        permissions.setOnClickListener(view -> openAppSettings());

        page.addView(infoCard("2 · Permitir aplicaciones externas en Termux",
                "En Termux, abre ~/.termux/termux.properties y añade o cambia esta propiedad:\n\n"
                        + "allow-external-apps=true\n\n"
                        + "Guarda el archivo. Esta opción es obligatoria por seguridad."));
        Button openTermux = secondaryButton("Abrir Termux");
        LinearLayout.LayoutParams termuxParams = fullWidthButton();
        termuxParams.topMargin = dp(8);
        page.addView(openTermux, termuxParams);
        openTermux.setOnClickListener(view -> launchTermux());

        page.addView(label("Ruta del comando local", 13, TEXT, true));
        EditText path = new EditText(this);
        path.setSingleLine(true);
        path.setTextColor(TEXT);
        path.setHintTextColor(MUTED);
        path.setTextSize(12);
        path.setPadding(dp(12), dp(12), dp(12), dp(12));
        path.setBackground(round(PANEL, dp(12)));
        path.setText(TermuxCommandClient.getCommandPath(this));
        LinearLayout.LayoutParams pathParams = fullWidthButton();
        pathParams.topMargin = dp(8);
        page.addView(path, pathParams);

        Button savePath = secondaryButton("Guardar ruta");
        LinearLayout.LayoutParams saveParams = fullWidthButton();
        saveParams.topMargin = dp(8);
        page.addView(savePath, saveParams);
        savePath.setOnClickListener(view -> {
            String value = path.getText().toString().trim();
            if (!value.startsWith("/data/")) {
                Toast.makeText(this, "RUN_COMMAND necesita una ruta absoluta de Termux (/data/…).", Toast.LENGTH_LONG).show();
                return;
            }
            TermuxCommandClient.setCommandPath(this, value);
            Toast.makeText(this, "Ruta guardada.", Toast.LENGTH_SHORT).show();
        });

        Button copySetup = secondaryButton("Copiar ajuste de Termux");
        LinearLayout.LayoutParams copyParams = fullWidthButton();
        copyParams.topMargin = dp(8);
        page.addView(copySetup, copyParams);
        copySetup.setOnClickListener(view -> {
            String command = "mkdir -p ~/.termux && (grep -q '^[[:space:]]*allow-external-apps[[:space:]]*=' ~/.termux/termux.properties 2>/dev/null && sed -i 's/^[[:space:]]*allow-external-apps[[:space:]]*=.*/allow-external-apps=true/' ~/.termux/termux.properties || printf '\\nallow-external-apps=true\\n' >> ~/.termux/termux.properties)";
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("NYX Termux setup", command));
                Toast.makeText(this, "Comando copiado. Pégalo en Termux.", Toast.LENGTH_LONG).show();
            }
        });

        page.addView(infoCard("Privacidad y seguridad",
                "NYX solo ejecuta el script nyx-chat mediante RUN_COMMAND. El mensaje y las rutas de Dropbox "
                        + "se entregan como argumentos, no como código shell. El script acepta conversación, "
                        + "diagnóstico y tres acciones Dropbox de solo lectura; no ofrece ejecución arbitraria."));
        Button clear = secondaryButton("Borrar historial local");
        LinearLayout.LayoutParams clearParams = fullWidthButton();
        clearParams.topMargin = dp(10);
        page.addView(clear, clearParams);
        clear.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle("Borrar historial")
                .setMessage("Se eliminarán los mensajes guardados en esta aplicación. No afecta a los logs ni a la memoria de BISTURÍ.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Borrar", (dialog, which) -> {
                    ChatMessageStore.clearMessages(this);
                    Toast.makeText(this, "Historial eliminado.", Toast.LENGTH_SHORT).show();
                })
                .show());

        scroll.addView(page);
        return scroll;
    }

    private void refreshSystemStatus() {
        if (!ensureTermuxReady()) {
            return;
        }
        try {
            TermuxCommandClient.requestStatus(this);
            Toast.makeText(this, "Consultando Qwen y Runtime 770…", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo iniciar el diagnóstico: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean ensureTermuxReady() {
        if (!TermuxCommandClient.isTermuxInstalled(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Termux no encontrado")
                    .setMessage("Instala Termux y completa su configuración antes de iniciar el chat local.")
                    .setPositiveButton("Entendido", null)
                    .show();
            return false;
        }
        if (!TermuxCommandClient.hasRunCommandPermission(this)) {
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(
                        new String[]{TermuxCommandClient.RUN_COMMAND_PERMISSION},
                        REQUEST_TERMUX_PERMISSION
                );
            } else {
                openAppSettings();
            }
            Toast.makeText(this, "Concede el permiso y vuelve a enviar el mensaje.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_TERMUX_PERMISSION) {
            String message = grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ? "Permiso concedido. Ya puedes enviar el mensaje."
                    : "El permiso sigue pendiente. Actívalo desde los ajustes de NYX.";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void launchTermux() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(TermuxCommandClient.TERMUX_PACKAGE);
        if (launch == null) {
            Toast.makeText(this, "No se encontró Termux.", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(launch);
    }

    private View infoCard(String title, String body) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(13), dp(15), dp(13));
        card.setBackground(round(PANEL, dp(16)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, -2
        );
        params.topMargin = dp(10);
        card.setLayoutParams(params);

        card.addView(label(title, 13, TEXT, true));
        TextView detail = label(body, 12, MUTED, false);
        detail.setPadding(0, dp(7), 0, 0);
        card.addView(detail);
        return card;
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(TEXT);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(round(ACCENT_DARK, dp(16)));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(TEXT);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setBackground(round(PANEL_LIGHT, dp(14)));
        return button;
    }

    private LinearLayout.LayoutParams fullWidthButton() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46)
        );
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
