package com.treeidflashcards.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://www.treeidflashcards.com/";
    private WebView webView;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new NativeTTSBridge(), "NativeTTS");

        tts = new TextToSpeech(this, status -> {
            ttsReady = status == TextToSpeech.SUCCESS;
            if (ttsReady) {
                tts.setLanguage(Locale.getDefault());
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        sendStatus("start", "Speaking with installed device voice…");
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        sendStatus("done", "");
                    }

                    @Override
                    public void onError(String utteranceId) {
                        sendStatus("error", "native synthesis error");
                    }

                    @Override
                    public void onError(String utteranceId, int errorCode) {
                        sendStatus("error", "code " + errorCode);
                    }
                });
            }
            runOnUiThread(() ->
                webView.evaluateJavascript(
                    "window.dispatchEvent(new Event('nativevoiceschanged'));",
                    null
                )
            );
        });

        webView.loadUrl(APP_URL);
    }

    private void sendStatus(String status, String message) {
        final String script =
            "window.onNativeTTSStatus && window.onNativeTTSStatus(" +
            JSONObject.quote(status) + "," + JSONObject.quote(message) + ");";
        runOnUiThread(() -> webView.evaluateJavascript(script, null));
    }

    public class NativeTTSBridge {
        @JavascriptInterface
        public boolean isReady() {
            return ttsReady;
        }

        @JavascriptInterface
        public String getVoices() {
            JSONArray array = new JSONArray();
            if (!ttsReady || tts == null) return array.toString();

            try {
                Set<android.speech.tts.Voice> set = tts.getVoices();
                if (set == null) return array.toString();

                List<android.speech.tts.Voice> voices = new ArrayList<>(set);
                voices.sort(
                    Comparator.comparing((android.speech.tts.Voice v) ->
                        v.getLocale() == null ? "" : v.getLocale().toLanguageTag()
                    ).thenComparing(android.speech.tts.Voice::getName)
                );

                for (android.speech.tts.Voice voice : voices) {
                    JSONObject item = new JSONObject();
                    item.put("name", voice.getName());
                    item.put(
                        "locale",
                        voice.getLocale() == null ? "" : voice.getLocale().toLanguageTag()
                    );
                    item.put("network", voice.isNetworkConnectionRequired());
                    array.put(item);
                }
            } catch (Exception ignored) {
            }
            return array.toString();
        }

        @JavascriptInterface
        public void speak(String text, String voiceName, float rate) {
            runOnUiThread(() -> {
                if (!ttsReady || tts == null || text == null || text.trim().isEmpty()) {
                    sendStatus("error", "text-to-speech engine not ready");
                    return;
                }

                try {
                    tts.stop();
                    tts.setSpeechRate(Math.max(0.5f, Math.min(rate, 1.5f)));
                    tts.setPitch(1.0f);

                    boolean voiceApplied = false;
                    if (voiceName != null && !voiceName.isEmpty()) {
                        Set<android.speech.tts.Voice> voices = tts.getVoices();
                        if (voices != null) {
                            for (android.speech.tts.Voice voice : voices) {
                                if (voice.getName().equals(voiceName)) {
                                    voiceApplied = tts.setVoice(voice) == TextToSpeech.SUCCESS;
                                    break;
                                }
                            }
                        }
                    }

                    if (!voiceApplied) {
                        android.speech.tts.Voice defaultVoice = tts.getDefaultVoice();
                        if (defaultVoice != null) {
                            tts.setVoice(defaultVoice);
                        } else {
                            tts.setLanguage(Locale.getDefault());
                        }
                    }

                    int result = tts.speak(
                        text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "tree-id-" + System.currentTimeMillis()
                    );

                    if (result == TextToSpeech.ERROR) {
                        sendStatus("error", "Android rejected the speech request");
                    }
                } catch (Exception error) {
                    sendStatus("error", error.getMessage());
                }
            });
        }

        @JavascriptInterface
        public void stop() {
            runOnUiThread(() -> {
                if (tts != null) tts.stop();
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (webView != null) {
            webView.removeJavascriptInterface("NativeTTS");
            webView.destroy();
        }
        super.onDestroy();
    }
}
