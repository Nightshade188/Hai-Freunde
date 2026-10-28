package de.hai.kind;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.WindowManager;

import java.util.Locale;

/**
 * Hai-Freunde: Ein Hai erzählt einem Kleinkind (2-3 Jahre) etwas über Haie.
 * Bedienung: nur Antippen. Hai antippen = nächste Geschichte (andere Haiart, anderer Hintergrund, Bewegung, gesprochener Satz),
 * dazu viele Dinge zum Antippen (Blasen, Fische, Quallen, Seesterne, Krabbe, Muschel, Schildkröte, Delfin).
 * Im Hintergrund läuft ruhige Meeresmusik. Das Menü (Symbol oben rechts) schaltet Musik und Erzählmodus (Text und Stimme) und beendet die App.
 * Kein Lesen nötig, kein Netz, keine Berechtigungen.
 */
public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private boolean ttsBereit = false;
    private String wartenderText = null;
    private SeaView meer;
    private HaiMusik musik;
    private SharedPreferences einstellungen;
    private boolean musikAn = true, erzaehlenAn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        einstellungen = getSharedPreferences("hai", MODE_PRIVATE);
        musikAn = einstellungen.getBoolean("musik", true);
        erzaehlenAn = einstellungen.getBoolean("erzaehlen", true);
        musik = new HaiMusik();
        meer = new SeaView(this);
        setContentView(meer);
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) return;
        int r = tts.setLanguage(Locale.GERMANY);
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return;
        tts.setSpeechRate(0.8f);   // langsam und deutlich
        tts.setPitch(1.25f);       // freundlich und hell
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {   // Musik leiser, solange der Hai spricht
            @Override public void onStart(String utteranceId) { musik.leiser(true); }
            @Override public void onDone(String utteranceId) { musik.leiser(false); }
            @Override public void onError(String utteranceId) { musik.leiser(false); }
        });
        ttsBereit = true;
        if (wartenderText != null) { sprich(wartenderText); wartenderText = null; }
    }

    /** Spricht den Text, wenn der Erzählmodus an ist. */
    void sprich(String text) {
        if (!erzaehlenAn) return;
        if (!ttsBereit) { wartenderText = text; return; }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hai");
    }

    // ------------------------------------------------------------------------------------------------ Einstellungen (Menü)

    boolean istMusikAn() { return musikAn; }
    boolean istErzaehlenAn() { return erzaehlenAn; }

    void setzeMusik(boolean an) {
        musikAn = an;
        einstellungen.edit().putBoolean("musik", an).apply();
        if (an) musik.start(); else musik.stop();
    }

    void setzeErzaehlen(boolean an) {
        erzaehlenAn = an;
        einstellungen.edit().putBoolean("erzaehlen", an).apply();
        if (!an) { wartenderText = null; if (tts != null) tts.stop(); musik.leiser(false); }
    }

    /** Beendet die App ganz (auch aus der Liste der zuletzt benutzten Apps). */
    void beenden() { finishAndRemoveTask(); }

    @Override
    public void onBackPressed() {
        if (meer != null) meer.menueUmschalten();   // Zurück-Taste öffnet und schließt das Menü; beendet wird nur im Menü
        else super.onBackPressed();
    }

    // ------------------------------------------------------------------------------------------------ Lebenszyklus

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {   // Vollbild, damit das Kind nicht versehentlich Leisten antippt
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (musikAn) musik.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        musik.stop();
        if (tts != null) tts.stop();
    }

    @Override
    protected void onDestroy() {
        musik.stop();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
