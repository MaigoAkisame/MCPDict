package maigosoft.mcpdict;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;

public class HelpActivity extends ActivityWithOptionsMenu {
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_activity);

        WebView webview = (WebView) findViewById(R.id.web_view_help);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadUrl("file:///android_asset/help/index.htm");
    }
}
