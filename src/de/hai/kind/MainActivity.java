package de.hai.kind;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Hai-Freunde: Ein Hai erzählt einem Kleinkind (2-3 Jahre) etwas über Haie.
 * Bedienung: nur Antippen. Hai antippen = nächste Geschichte (Bild, Bewegung und gesprochener Satz),
 * Blasen antippen = platzen, kleine Fische antippen = sie flitzen davon. Kein Lesen, kein Netz, keine Berechtigungen.
 */
public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean ttsBereit = false;
    private String wartenderText = null;
    private SeaView meer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        ttsBereit = true;
        if (wartenderText != null) { sprich(wartenderText); wartenderText = null; }
    }

    void sprich(String text) {
        if (!ttsBereit) { wartenderText = text; return; }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hai");
    }

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
    protected void onPause() {
        super.onPause();
        if (tts != null) tts.stop();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }

    // ------------------------------------------------------------------------------------------------------------

    private static final int M_RUHIG = 0, M_ZAEHNE = 1, M_SCHWANZ = 2, M_NASE = 3, M_GROSS = 4, M_KIEMEN = 5;

    private static final int[] FAKT_MODUS = { M_ZAEHNE, M_SCHWANZ, M_NASE, M_GROSS, M_KIEMEN };
    private static final String[] FAKT_TEXT = {
            "Der Hai hat ganz viele Zähne. Wenn ein Zahn ausfällt, wächst ein neuer!",
            "Mit seinem großen Schwanz schwimmt der Hai ganz schnell. Swusch!",
            "Der Hai kann super riechen. Schnuff, schnuff, schnuff!",
            "Es gibt kleine Haie und riesengroße Haie. Der Walhai ist so groß wie ein Bus!",
            "Der Hai ist ein Fisch. Im Wasser atmet er mit Kiemen. Blubb, blubb!"
    };
    private static final String BEGRUESSUNG = "Hallo! Ich bin der Hai. Tipp mich an!";

    private static class Blase { float x, y, r, tempo, platzt = -1; }
    private static class Fisch { float x, y, tempo, schub; int farbe; boolean nachRechts; }

    class SeaView extends View {
        private final Random zufall = new Random();
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path pfad = new Path();
        private final RectF rect = new RectF();
        private final TextPaint textFarbe = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final List<Blase> blasen = new ArrayList<>();
        private final List<Fisch> fische = new ArrayList<>();
        private ToneGenerator ton;

        private final long start = System.nanoTime();
        private float t, letzteT;
        private int faktNr = -1;
        private int modus = M_RUHIG;
        private float modusBis = 0;
        private float hupf = 0;           // 1 direkt nach dem Antippen, klingt ab
        private String untertitel = "Tipp mich an!";
        private StaticLayout textLayout;
        private String layoutText = "";
        private int layoutBreite = 0;
        private LinearGradient wasser;
        private int wasserHoehe = 0;

        SeaView(Context c) {
            super(c);
            textFarbe.setColor(0xFF1B3A57);
            textFarbe.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            try { ton = new ToneGenerator(AudioManager.STREAM_MUSIC, 45); } catch (RuntimeException e) { ton = null; }
            int[] farben = { 0xFFFF9A3C, 0xFFFFD23C, 0xFFFF6F91 };
            for (int i = 0; i < 3; i++) {
                Fisch f = new Fisch();
                f.x = zufall.nextFloat(); f.y = 0.55f + 0.12f * i; f.tempo = 0.04f + 0.02f * i;
                f.farbe = farben[i]; f.nachRechts = i % 2 == 0;
                fische.add(f);
            }
            sprichVerzoegert();
        }

        private void sprichVerzoegert() { sprich(BEGRUESSUNG); }

        private Blase neueBlase(boolean ueberallStarten, int h) {
            Blase b = new Blase();
            b.r = h * (0.015f + zufall.nextFloat() * 0.03f);
            b.x = zufall.nextFloat();
            b.y = ueberallStarten ? zufall.nextFloat() * h : h * 0.95f;
            b.tempo = h * (0.06f + zufall.nextFloat() * 0.08f);
            return b;
        }

        private void pop() { if (ton != null) ton.startTone(ToneGenerator.TONE_PROP_ACK, 70); }

        // ---------------------------------------------------------------------------------------------- Zeichnen

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            t = (System.nanoTime() - start) / 1e9f;
            float dt = Math.min(0.1f, t - letzteT);
            letzteT = t;
            hupf = Math.max(0, hupf - dt * 2.2f);
            if (modus != M_RUHIG && t > modusBis) modus = M_RUHIG;
            if (blasen.isEmpty()) for (int i = 0; i < 14; i++) blasen.add(neueBlase(true, h));

            zeichneMeer(c, w, h);

            // kleine Fische
            for (Fisch f : fische) {
                float tempo = f.tempo * (1 + f.schub * 6);
                f.schub = Math.max(0, f.schub - dt * 1.2f);
                f.x += (f.nachRechts ? 1 : -1) * tempo * dt;
                if (f.x > 1.15f) f.x = -0.15f;
                if (f.x < -0.15f) f.x = 1.15f;
                float fy = h * f.y + (float) Math.sin(t * 2 + f.y * 10) * h * 0.015f - f.schub * h * 0.12f;
                zeichneFisch(c, f.x * w, fy, h * 0.04f, f.farbe, f.nachRechts);
            }

            // Hai (groß) und, beim Thema "groß", ein kleiner Hai daneben
            float s = Math.min(w / 560f, h / 430f) * 0.95f;
            float sx = w * 0.5f, sy = h * 0.45f + (float) Math.sin(t * 1.3f) * h * 0.02f;
            float mund = 0, schwanzTempo = 1.6f, schnuppern = 0;
            boolean kiemenBlasen = false;
            float groesse = 1 + 0.12f * hupf;
            switch (modus) {
                case M_ZAEHNE: mund = 0.65f + 0.3f * (float) Math.sin(t * 3.5f); break;
                case M_SCHWANZ: schwanzTempo = 7f; sx += (float) Math.sin(t * 2) * w * 0.03f; break;
                case M_NASE: schnuppern = (float) Math.sin(t * 14) * 0.5f + 0.5f; break;
                case M_GROSS: groesse *= 1.1f; break;
                case M_KIEMEN: kiemenBlasen = true; break;
                default: break;
            }
            if (modus == M_GROSS) {
                float frac = (t * 0.06f) % 1f;
                zeichneHai(c, w * (0.1f + 0.8f * frac), h * 0.17f, s * 0.22f, 0, 3f, 0, false);
            }
            zeichneHai(c, sx, sy, s * groesse, mund, schwanzTempo, schnuppern, kiemenBlasen);
            if (modus == M_SCHWANZ) zeichneTempoStriche(c, sx, sy, s, w);

            // Blasen
            for (Blase b : blasen) {
                if (b.platzt >= 0) {
                    b.platzt += dt;
                    float k = b.platzt / 0.35f;
                    if (k >= 1) { Blase n = neueBlase(false, h); b.x = n.x; b.y = n.y; b.r = n.r; b.tempo = n.tempo; b.platzt = -1; continue; }
                    p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(h * 0.006f);
                    p.setColor(((int) (200 * (1 - k)) << 24) | 0xFFFFFF);
                    c.drawCircle(b.x * w, b.y, b.r * (1 + 1.2f * k), p);
                    continue;
                }
                b.y -= b.tempo * dt;
                if (b.y < -b.r) { Blase n = neueBlase(false, h); b.x = n.x; b.y = n.y; b.r = n.r; b.tempo = n.tempo; }
                float bx = b.x * w + (float) Math.sin(t * 1.5f + b.r) * b.r * 0.6f;
                p.setStyle(Paint.Style.FILL); p.setColor(0x55FFFFFF);
                c.drawCircle(bx, b.y, b.r, p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(2, b.r * 0.12f)); p.setColor(0xCCFFFFFF);
                c.drawCircle(bx, b.y, b.r, p);
                p.setStyle(Paint.Style.FILL); p.setColor(0xDDFFFFFF);
                c.drawCircle(bx - b.r * 0.35f, b.y - b.r * 0.35f, b.r * 0.2f, p);
            }

            zeichneUntertitel(c, w, h);
            postInvalidateOnAnimation();
        }

        private void zeichneMeer(Canvas c, int w, int h) {
            if (wasser == null || wasserHoehe != h) {
                wasser = new LinearGradient(0, 0, 0, h, 0xFF9BDDF7, 0xFF1F66AD, Shader.TileMode.CLAMP);
                wasserHoehe = h;
            }
            p.setStyle(Paint.Style.FILL); p.setShader(wasser);
            c.drawRect(0, 0, w, h, p);
            p.setShader(null);
            // Sand
            p.setColor(0xFFEBD28F);
            pfad.reset(); pfad.moveTo(0, h);
            pfad.lineTo(0, h * 0.88f);
            pfad.quadTo(w * 0.25f, h * 0.83f, w * 0.5f, h * 0.88f);
            pfad.quadTo(w * 0.75f, h * 0.93f, w, h * 0.87f);
            pfad.lineTo(w, h); pfad.close();
            c.drawPath(pfad, p);
            // Seetang
            p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeWidth(h * 0.025f); p.setColor(0xFF2E9E5B);
            float[] xs = { 0.06f, 0.13f, 0.86f, 0.93f, 0.40f };
            for (int i = 0; i < xs.length; i++) {
                float bx = xs[i] * w, by = h * 0.9f, hoehe = h * (0.14f + 0.04f * (i % 3));
                float pendel = (float) Math.sin(t * 1.4f + i) * h * 0.03f;
                pfad.reset(); pfad.moveTo(bx, by);
                pfad.quadTo(bx + pendel, by - hoehe * 0.5f, bx - pendel * 0.5f, by - hoehe);
                c.drawPath(pfad, p);
            }
            p.setStrokeCap(Paint.Cap.BUTT);
        }

        private void zeichneFisch(Canvas c, float x, float y, float r, int farbe, boolean nachRechts) {
            c.save(); c.translate(x, y); if (!nachRechts) c.scale(-1, 1);
            p.setStyle(Paint.Style.FILL); p.setColor(farbe);
            rect.set(-r * 1.3f, -r * 0.8f, r * 1.3f, r * 0.8f); c.drawOval(rect, p);
            pfad.reset(); pfad.moveTo(-r, 0); pfad.lineTo(-r * 2.1f, -r * 0.9f); pfad.lineTo(-r * 2.1f, r * 0.9f); pfad.close();
            c.drawPath(pfad, p);
            p.setColor(0xFFFFFFFF); c.drawCircle(r * 0.6f, -r * 0.2f, r * 0.3f, p);
            p.setColor(0xFF222222); c.drawCircle(r * 0.67f, -r * 0.2f, r * 0.15f, p);
            c.restore();
        }

        private void zeichneTempoStriche(Canvas c, float sx, float sy, float s, int w) {
            p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeWidth(s * 7); p.setColor(0x99FFFFFF);
            for (int i = 0; i < 4; i++) {
                float y = sy + (i - 1.5f) * s * 40;
                float v = ((t * 1.8f + i * 0.27f) % 1f);
                float x1 = sx - s * 270 - v * s * 160;
                c.drawLine(x1, y, x1 - s * 90, y, p);
            }
            p.setStrokeCap(Paint.Cap.BUTT);
        }

        /** Hai in Einheiten von etwa 460 x 260, Nase zeigt nach rechts. */
        private void zeichneHai(Canvas c, float cx, float cy, float s, float mund, float schwanzTempo, float schnuppern, boolean kiemenBlasen) {
            c.save();
            c.translate(cx, cy);
            c.scale(s, s);
            p.setPathEffect(new CornerPathEffect(14));
            p.setStyle(Paint.Style.FILL);

            float wedel = (float) Math.sin(t * schwanzTempo) * 14f;

            // Schwanzflosse (dreht sich am Ansatz)
            c.save(); c.translate(-165, 0); c.rotate(wedel);
            p.setColor(0xFF5E8FC2);
            pfad.reset(); pfad.moveTo(5, -8); pfad.lineTo(-85, -88); pfad.quadTo(-50, 0, -95, 85); pfad.lineTo(5, 8); pfad.close();
            c.drawPath(pfad, p);
            c.restore();

            // Rückenflosse und Brustflosse
            p.setColor(0xFF5E8FC2);
            pfad.reset(); pfad.moveTo(10, -66); pfad.lineTo(-55, -148); pfad.lineTo(-100, -58); pfad.close();
            c.drawPath(pfad, p);
            c.save(); c.translate(20, 55); c.rotate((float) Math.sin(t * 2.2f) * 8f);
            pfad.reset(); pfad.moveTo(10, 0); pfad.lineTo(-60, 70); pfad.lineTo(-62, -12); pfad.close();
            c.drawPath(pfad, p);
            c.restore();

            // Körper
            c.save();
            if (schnuppern > 0) c.rotate(-3f * schnuppern, 0, 0);
            p.setColor(0xFF6FA8DC);
            pfad.reset(); pfad.moveTo(215, 12);
            pfad.quadTo(120, -88, -40, -72); pfad.quadTo(-125, -62, -172, -12);
            pfad.lineTo(-172, 12); pfad.quadTo(-100, 62, -20, 74); pfad.quadTo(130, 72, 215, 12); pfad.close();
            c.drawPath(pfad, p);
            // heller Bauch
            p.setColor(0xFFEAF4FB);
            pfad.reset(); pfad.moveTo(205, 20); pfad.quadTo(130, 76, -20, 72); pfad.quadTo(-100, 62, -165, 14);
            pfad.quadTo(-60, 44, 60, 42); pfad.quadTo(150, 40, 205, 20); pfad.close();
            c.drawPath(pfad, p);
            p.setPathEffect(null);

            // Kiemen
            p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeWidth(kiemenBlasen ? 7 : 4);
            p.setColor(kiemenBlasen ? 0xFF2D5E8E : 0x883B6C9C);
            for (int i = 0; i < 3; i++) c.drawLine(52 - i * 15, -22 + i * 1.5f, 46 - i * 15, 22 + i * 1.5f, p);
            p.setStrokeCap(Paint.Cap.BUTT);

            // Wange, Auge, Nase
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x66FF8FA3); c.drawCircle(105, 22, 15, p);
            p.setColor(0xFFFFFFFF); c.drawCircle(120, -20, 20, p);
            p.setColor(0xFF1A1A1A); c.drawCircle(126, -19, 11, p);
            p.setColor(0xFFFFFFFF); c.drawCircle(130, -23, 4, p);
            p.setColor(0xFF3B6C9C); c.drawCircle(176, 2, 3.5f, p);

            // Mund: lächelt, bei "Zähne" weit offen mit Zähnen
            if (mund > 0.05f) {
                float oben = 28, tief = 52 * mund;
                p.setColor(0xFF8B1A2B);
                rect.set(100, oben - 4, 200, oben + tief); c.drawOval(rect, p);
                p.setColor(0xFFFFFFFF);
                for (int i = 0; i < 6; i++) {
                    float x = 112 + i * 14f;
                    pfad.reset(); pfad.moveTo(x, oben + 1); pfad.lineTo(x + 11, oben + 1); pfad.lineTo(x + 5.5f, oben + 1 + 16 * mund); pfad.close();
                    c.drawPath(pfad, p);
                    float ux = 116 + i * 13f, unten = oben + tief - 5;
                    pfad.reset(); pfad.moveTo(ux, unten); pfad.lineTo(ux + 10, unten); pfad.lineTo(ux + 5, unten - 14 * mund); pfad.close();
                    c.drawPath(pfad, p);
                }
            } else {
                p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeWidth(5); p.setColor(0xFF2B4A6B);
                pfad.reset(); pfad.moveTo(196, 22); pfad.quadTo(150, 56, 100, 32);
                c.drawPath(pfad, p);
                p.setStrokeCap(Paint.Cap.BUTT);
            }
            c.restore();

            // Riechblasen an der Nase / Blasen aus den Kiemen
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(0xDDFFFFFF);
            if (schnuppern > 0) {
                for (int i = 0; i < 3; i++) {
                    float v = (t * 1.2f + i * 0.33f) % 1f;
                    c.drawCircle(225 + v * 60, -6 - v * 40 - i * 10, 6 + i * 3 + v * 4, p);
                }
            }
            if (kiemenBlasen) {
                for (int i = 0; i < 5; i++) {
                    float v = (t * 0.9f + i * 0.2f) % 1f;
                    c.drawCircle(30 - v * 40 + (float) Math.sin(t * 4 + i) * 8, -20 - v * 120, 5 + i * 1.5f, p);
                }
            }
            p.setStyle(Paint.Style.FILL);
            c.restore();
        }

        private void zeichneUntertitel(Canvas c, int w, int h) {
            textFarbe.setTextSize(h * 0.05f);
            int breite = (int) (w * 0.8f);
            if (textLayout == null || !layoutText.equals(untertitel) || layoutBreite != breite) {
                textLayout = StaticLayout.Builder.obtain(untertitel, 0, untertitel.length(), textFarbe, breite)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER).build();
                layoutText = untertitel; layoutBreite = breite;
            }
            float rand = h * 0.025f;
            float boxH = textLayout.getHeight() + rand * 2;
            float boxY = h - boxH - h * 0.04f;
            p.setStyle(Paint.Style.FILL); p.setColor(0xEEFFFFFF);
            rect.set(w * 0.1f - rand, boxY, w * 0.9f + rand, boxY + boxH);
            c.drawRoundRect(rect, rand * 2, rand * 2, p);
            c.save(); c.translate(w * 0.1f, boxY + rand); textLayout.draw(c); c.restore();
        }

        // ---------------------------------------------------------------------------------------------- Antippen

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getActionMasked() != MotionEvent.ACTION_DOWN && e.getActionMasked() != MotionEvent.ACTION_POINTER_DOWN) return true;
            int i = e.getActionIndex();
            float x = e.getX(i), y = e.getY(i);
            int w = getWidth(), h = getHeight();

            // Blasen platzen lassen (großzügige Trefferfläche für kleine Finger)
            for (Blase b : blasen) {
                if (b.platzt >= 0) continue;
                float bx = b.x * w;
                float d = Math.max(b.r * 1.6f, h * 0.05f);
                if ((x - bx) * (x - bx) + (y - b.y) * (y - b.y) < d * d) { b.platzt = 0; pop(); return true; }
            }
            // kleine Fische flitzen davon
            for (Fisch f : fische) {
                float fy = h * f.y;
                if (Math.abs(x - f.x * w) < h * 0.12f && Math.abs(y - fy) < h * 0.1f) { f.schub = 1; pop(); return true; }
            }
            // Hai: nächste Geschichte
            float s = Math.min(w / 560f, h / 430f) * 0.95f;
            if (Math.abs(x - w * 0.5f) < 250 * s && Math.abs(y - h * 0.45f) < 150 * s) {
                faktNr = (faktNr + 1) % FAKT_TEXT.length;
                modus = FAKT_MODUS[faktNr];
                modusBis = t + 9f;
                hupf = 1;
                untertitel = FAKT_TEXT[faktNr];
                sprich(FAKT_TEXT[faktNr]);
            }
            return true;
        }
    }
}
