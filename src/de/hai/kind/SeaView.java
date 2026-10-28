package de.hai.kind;

import static de.hai.kind.Inhalt.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Die Unterwasserwelt: Hai in verschiedenen Arten, wechselnde Hintergründe und viele Dinge zum Antippen.
 * Alles wird mit Canvas gezeichnet (keine Bilder), die Zeit läuft über System.nanoTime().
 */
class SeaView extends View {
    // ---------------------------------------------------------------------------------------------- Bausteine
    private static class Blase { float x, y, r, tempo, platzt = -1; }
    private static class Fisch { float x, y, tempo, schub; int farbe; boolean nachRechts; }
    private static class Funke { float x, y, vx, vy, leben, gr; int farbe; }
    private static class Qualle { float x, y, phase, schub, leuchten; int farbe; }
    private static class Seestern { float x, dreh, tempo, gr; int farbe; }

    private final MainActivity app;
    private final Random zufall = new Random();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path pfad = new Path();
    private final RectF rect = new RectF();
    private final TextPaint textFarbe = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint menueText = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final List<Blase> blasen = new ArrayList<>();
    private final List<Fisch> fische = new ArrayList<>();
    private final List<Funke> funken = new ArrayList<>();
    private final List<Qualle> quallen = new ArrayList<>();
    private final List<Seestern> seesterne = new ArrayList<>();
    private final Blubb ton = new Blubb();

    // Zeit und Zustand
    private final long start = System.nanoTime();
    private float t, letzteT;
    private int geschNr = -1, art = A_FREUND, szene = S_TAG, szeneAlt = -1;
    private float artWechselT = -10, szeneWechselT = -10;
    private int modus = M_RUHIG;
    private float modusBis = 0, hupf = 0;
    private String untertitel = "Tipp mich an!";
    private StaticLayout textLayout;
    private String layoutText = "";
    private int layoutBreite = 0;
    private final LinearGradient[] verlauf = new LinearGradient[S_ANZAHL];
    private int verlaufHoehe = 0;

    // Planktonteilchen (Tiefsee und Walhai)
    private final float[] plX = new float[46], plY = new float[46], plV = new float[46], plR = new float[46];

    // Krabbe
    private float krabbeX = 0.3f, krabbeRichtung = 1, krabbeVersteckt = 0, krabbeKlappern = 0;
    // Muschel
    private float muschelOffen = 0, muschelHalten = 0, muschelNaechste = 3;
    // Schildkröte
    private boolean schildAktiv = false;
    private float schildX, schildY, schildRichtung = 1, schildKopfEin = 0, schildDreh = 0, schildNaechste = 14;
    // Delfin
    private boolean delfinAktiv = false;
    private float delfinFortschritt = 0, delfinNaechster = 30;
    private boolean delfinNachRechts = true;
    private float delfinSalto = 0;

    // Menü
    private boolean menueOffen = false;
    private float beendenBis = -1;
    private final RectF menueKnopf = new RectF(), menueMusik = new RectF(), menueErzaehlen = new RectF(), menueBeenden = new RectF(), menueZurueck = new RectF(), menueNeu = new RectF(), menueTafel = new RectF();

    SeaView(Context c) {
        super(c);
        app = (MainActivity) c;
        textFarbe.setColor(0xFF1B3A57);
        textFarbe.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        menueText.setColor(0xFFFFFFFF);
        menueText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        menueText.setTextAlign(Paint.Align.CENTER);
        int[] farben = { 0xFFFF9A3C, 0xFFFFD23C, 0xFFFF6F91 };
        for (int i = 0; i < 3; i++) {
            Fisch f = new Fisch();
            f.x = zufall.nextFloat(); f.y = 0.55f + 0.12f * i; f.tempo = 0.04f + 0.02f * i;
            f.farbe = farben[i]; f.nachRechts = i % 2 == 0;
            fische.add(f);
        }
        for (int i = 0; i < 2; i++) {
            Qualle q = new Qualle();
            q.x = 0.14f + 0.7f * i; q.y = 0.3f + 0.25f * i; q.phase = i * 2.1f; q.farbe = i == 0 ? 0xFFFF9AD5 : 0xFFB59BFF;
            quallen.add(q);
        }
        for (int i = 0; i < 2; i++) {
            Seestern s = new Seestern();
            s.x = 0.2f + 0.55f * i; s.gr = 1; s.farbe = i == 0 ? 0xFFFF8A3C : 0xFFFF6F91; s.dreh = i * 20;
            seesterne.add(s);
        }
        for (int i = 0; i < plX.length; i++) {
            plX[i] = zufall.nextFloat(); plY[i] = zufall.nextFloat(); plV[i] = 0.01f + zufall.nextFloat() * 0.03f; plR[i] = 0.003f + zufall.nextFloat() * 0.005f;
        }
        app.sprich(BEGRUESSUNG);
    }

    // ---------------------------------------------------------------------------------------------- Hilfen

    private void pop() { ton.blubb(); }
    private void klingel() { ton.blubbern(); }

    private Blase neueBlase(boolean ueberallStarten, int h) {
        Blase b = new Blase();
        b.r = h * (0.015f + zufall.nextFloat() * 0.03f);
        b.x = zufall.nextFloat();
        b.y = ueberallStarten ? zufall.nextFloat() * h : h * 0.95f;
        b.tempo = h * (0.06f + zufall.nextFloat() * 0.08f);
        return b;
    }

    private void funkeln(float x, float y, int anzahl, int... farben) {
        for (int i = 0; i < anzahl && funken.size() < 90; i++) {
            Funke f = new Funke();
            float a = zufall.nextFloat() * 6.2832f, v = 40 + zufall.nextFloat() * 160;
            f.x = x; f.y = y; f.vx = (float) Math.cos(a) * v; f.vy = (float) Math.sin(a) * v - 40;
            f.leben = 1; f.gr = 4 + zufall.nextFloat() * 7; f.farbe = farben[zufall.nextInt(farben.length)];
            funken.add(f);
        }
    }

    private void blaseAn(float x, float y, int w, int h) {
        Blase b = new Blase();
        b.r = h * (0.01f + zufall.nextFloat() * 0.015f); b.x = x / w; b.y = y; b.tempo = h * (0.08f + zufall.nextFloat() * 0.06f);
        blasen.add(b);
        if (blasen.size() > 40) blasen.remove(0);
    }

    private static float weich(float k) { k = Math.max(0, Math.min(1, k)); return k * k * (3 - 2 * k); }

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

        zeichneHintergrund(c, w, h);
        if (szene == S_TIEF || szeneAlt == S_TIEF) zeichnePlankton(c, w, h, dt);

        aktualisiereBewohner(dt, w, h);
        zeichneDelfin(c, w, h);
        for (Qualle q : quallen) zeichneQualle(c, q, w, h);

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

        // Boden: Seesterne, Muschel, Krabbe
        for (Seestern s : seesterne) zeichneSeestern(c, s, w, h, dt);
        zeichneMuschel(c, w, h);
        zeichneKrabbe(c, w, h);
        zeichneSchildkroete(c, w, h);

        // Hai
        zeichneDenHai(c, w, h);

        // Blasen
        for (int bi = blasen.size() - 1; bi >= 0; bi--) {
            Blase b = blasen.get(bi);
            if (b.platzt >= 0) {
                b.platzt += dt;
                float k = b.platzt / 0.35f;
                if (k >= 1) { if (bi >= 14) blasen.remove(bi); else { Blase n = neueBlase(false, h); b.x = n.x; b.y = n.y; b.r = n.r; b.tempo = n.tempo; b.platzt = -1; } continue; }
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(h * 0.006f);
                p.setColor(((int) (200 * (1 - k)) << 24) | 0xFFFFFF);
                c.drawCircle(b.x * w, b.y, b.r * (1 + 1.2f * k), p);
                continue;
            }
            b.y -= b.tempo * dt;
            if (b.y < -b.r) { if (bi >= 14) { blasen.remove(bi); continue; } Blase n = neueBlase(false, h); b.x = n.x; b.y = n.y; b.r = n.r; b.tempo = n.tempo; }
            float bx = b.x * w + (float) Math.sin(t * 1.5f + b.r) * b.r * 0.6f;
            p.setStyle(Paint.Style.FILL); p.setColor(0x55FFFFFF);
            c.drawCircle(bx, b.y, b.r, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(2, b.r * 0.12f)); p.setColor(0xCCFFFFFF);
            c.drawCircle(bx, b.y, b.r, p);
            p.setStyle(Paint.Style.FILL); p.setColor(0xDDFFFFFF);
            c.drawCircle(bx - b.r * 0.35f, b.y - b.r * 0.35f, b.r * 0.2f, p);
        }

        zeichneFunken(c, dt);
        if (app.istErzaehlenAn()) zeichneUntertitel(c, w, h);
        zeichneMenueKnopf(c, w, h);
        if (menueOffen) zeichneMenue(c, w, h);
        postInvalidateOnAnimation();
    }

    // ---------------------------------------------------------------------------------------------- Hintergründe

    private void baueVerlaeufe(int h) {
        verlauf[S_TAG] = new LinearGradient(0, 0, 0, h, 0xFF9BDDF7, 0xFF1F66AD, Shader.TileMode.CLAMP);
        verlauf[S_RIFF] = new LinearGradient(0, 0, 0, h, 0xFFB3F5EE, 0xFF148FB3, Shader.TileMode.CLAMP);
        verlauf[S_TANG] = new LinearGradient(0, 0, 0, h, 0xFF9AD9B4, 0xFF184F4A, Shader.TileMode.CLAMP);
        verlauf[S_TIEF] = new LinearGradient(0, 0, 0, h, 0xFF1F5FA3, 0xFF040C28, Shader.TileMode.CLAMP);
        verlauf[S_ABEND] = new LinearGradient(0, 0, 0, h, new int[] { 0xFFFFB366, 0xFFE9798C, 0xFF1F5A8F }, new float[] { 0f, 0.35f, 1f }, Shader.TileMode.CLAMP);
        verlaufHoehe = h;
    }

    private void zeichneHintergrund(Canvas c, int w, int h) {
        if (verlaufHoehe != h) baueVerlaeufe(h);
        float k = weich((t - szeneWechselT) / 1.4f);
        if (szeneAlt >= 0 && k < 1) {
            zeichneSzene(c, w, h, szeneAlt);
            c.saveLayerAlpha(0, 0, w, h, (int) (255 * k));
            zeichneSzene(c, w, h, szene);
            c.restore();
        } else {
            szeneAlt = -1;
            zeichneSzene(c, w, h, szene);
        }
    }

    private void zeichneSzene(Canvas c, int w, int h, int s) {
        p.setStyle(Paint.Style.FILL); p.setShader(verlauf[s]);
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
        // Lichtstrahlen von oben (nicht in der Tiefsee)
        if (s != S_TIEF) {
            int alpha = s == S_ABEND ? 0x30 : 0x26;
            for (int i = 0; i < 3; i++) {
                float x0 = w * (0.15f + 0.3f * i) + (float) Math.sin(t * 0.3f + i * 2) * w * 0.03f;
                p.setColor((((int) (alpha * (0.7f + 0.3f * Math.sin(t * 0.5f + i)))) << 24) | 0xFFFFFF);
                pfad.reset(); pfad.moveTo(x0, 0); pfad.lineTo(x0 + w * 0.10f, 0); pfad.lineTo(x0 + w * 0.26f, h * 0.9f); pfad.lineTo(x0 + w * 0.06f, h * 0.9f); pfad.close();
                c.drawPath(pfad, p);
            }
        }
        if (s == S_ABEND) zeichneAbendhimmel(c, w, h);
        // Sandboden
        int[] sand = { 0xFFEBD28F, 0xFFF7E8BA, 0xFF8C7B5C, 0xFF33425E, 0xFFD9B27C };
        p.setStyle(Paint.Style.FILL); p.setColor(sand[s]);
        pfad.reset(); pfad.moveTo(0, h);
        pfad.lineTo(0, h * 0.88f);
        pfad.quadTo(w * 0.25f, h * 0.83f, w * 0.5f, h * 0.88f);
        pfad.quadTo(w * 0.75f, h * 0.93f, w, h * 0.87f);
        pfad.lineTo(w, h); pfad.close();
        c.drawPath(pfad, p);
        switch (s) {
            case S_RIFF: zeichneKorallen(c, w, h); break;
            case S_TANG: zeichneFelsen(c, w, h); zeichneSeetang(c, w, h, 9, 0.30f, 0xFF2F8F4B, 0.03f); break;
            case S_TIEF: zeichneSeetang(c, w, h, 3, 0.10f, 0xFF27596B, 0.02f); break;
            case S_ABEND: zeichneSeetang(c, w, h, 5, 0.14f, 0xFF3C8F66, 0.025f); break;
            default: zeichneSeetang(c, w, h, 5, 0.14f, 0xFF2E9E5B, 0.025f); break;
        }
    }

    private void zeichneAbendhimmel(Canvas c, int w, int h) {
        // Sonne dicht unter der Oberfläche mit Schimmer und ein Boot von unten gesehen
        p.setStyle(Paint.Style.FILL);
        for (int i = 3; i >= 0; i--) {
            p.setColor(((0x22 + (3 - i) * 0x10) << 24) | 0xFFE9A0);
            c.drawCircle(w * 0.78f, -h * 0.02f, h * (0.10f + 0.05f * i), p);
        }
        p.setColor(0xFFFFF0B0); c.drawCircle(w * 0.78f, -h * 0.02f, h * 0.09f, p);
        float bx = w * 0.22f + (float) Math.sin(t * 0.4f) * w * 0.01f, by = h * 0.035f + (float) Math.sin(t * 0.9f) * h * 0.004f;
        p.setColor(0xFF2A3D5A);
        pfad.reset(); pfad.moveTo(bx - h * 0.12f, by - h * 0.03f); pfad.lineTo(bx + h * 0.12f, by - h * 0.03f); pfad.lineTo(bx + h * 0.08f, by + h * 0.025f); pfad.lineTo(bx - h * 0.08f, by + h * 0.025f); pfad.close();
        c.drawPath(pfad, p);
    }

    private void zeichneSeetang(Canvas c, int w, int h, int anzahl, float hoehe, int farbe, float dicke) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeWidth(h * dicke); p.setColor(farbe);
        for (int i = 0; i < anzahl; i++) {
            float fx = anzahl <= 5 ? new float[] { 0.06f, 0.13f, 0.86f, 0.93f, 0.40f }[i] : (0.04f + 0.92f * i / (anzahl - 1));
            float bx = fx * w, by = h * 0.9f, hoeheI = h * (hoehe + 0.04f * (i % 3));
            float pendel = (float) Math.sin(t * 1.4f + i) * h * 0.03f;
            pfad.reset(); pfad.moveTo(bx, by);
            pfad.quadTo(bx + pendel, by - hoeheI * 0.5f, bx - pendel * 0.5f, by - hoeheI);
            c.drawPath(pfad, p);
        }
        p.setStrokeCap(Paint.Cap.BUTT);
    }

    private void zeichneFelsen(Canvas c, int w, int h) {
        p.setStyle(Paint.Style.FILL);
        float[][] f = { { 0.10f, 0.92f, 0.12f, 0.07f }, { 0.22f, 0.95f, 0.07f, 0.05f }, { 0.78f, 0.91f, 0.14f, 0.08f }, { 0.92f, 0.95f, 0.08f, 0.05f } };
        for (float[] r : f) {
            p.setColor(0xFF6E7B80); rect.set(w * (r[0] - r[2]), h * (r[1] - r[3]), w * (r[0] + r[2]), h * (r[1] + r[3])); c.drawOval(rect, p);
            p.setColor(0x33FFFFFF); rect.set(w * (r[0] - r[2] * 0.6f), h * (r[1] - r[3] * 0.9f), w * (r[0] + r[2] * 0.1f), h * (r[1] - r[3] * 0.1f)); c.drawOval(rect, p);
        }
    }

    private void zeichneKorallen(Canvas c, int w, int h) {
        int[] farben = { 0xFFFF6F91, 0xFFFF9A3C, 0xFFB06AD9, 0xFFFFD23C };
        float[] xs = { 0.05f, 0.14f, 0.32f, 0.66f, 0.82f, 0.94f };
        p.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < xs.length; i++) {
            float bx = xs[i] * w, by = h * 0.91f, hoehe = h * (0.12f + 0.04f * (i % 3));
            float wiegen = (float) Math.sin(t * 1.1f + i) * hoehe * 0.04f;
            p.setColor(farben[i % farben.length]);
            if (i % 3 == 0) {            // Geweihkoralle
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(h * 0.016f);
                for (int a = -1; a <= 1; a++) {
                    float tx = bx + a * hoehe * 0.45f + wiegen;
                    c.drawLine(bx, by, tx, by - hoehe, p);
                    c.drawLine(bx + a * hoehe * 0.2f, by - hoehe * 0.45f, tx + (a == 0 ? hoehe * 0.2f : a * hoehe * 0.25f), by - hoehe * 0.75f, p);
                    p.setStyle(Paint.Style.FILL); c.drawCircle(tx, by - hoehe, h * 0.012f, p); p.setStyle(Paint.Style.STROKE);
                }
            } else if (i % 3 == 1) {     // Fächerkoralle
                p.setStyle(Paint.Style.FILL); p.setAlpha(210);
                pfad.reset(); pfad.moveTo(bx, by);
                pfad.quadTo(bx - hoehe * 0.8f + wiegen, by - hoehe * 0.6f, bx - hoehe * 0.5f + wiegen, by - hoehe);
                pfad.quadTo(bx + wiegen, by - hoehe * 1.2f, bx + hoehe * 0.5f + wiegen, by - hoehe);
                pfad.quadTo(bx + hoehe * 0.8f + wiegen, by - hoehe * 0.6f, bx, by); pfad.close();
                c.drawPath(pfad, p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(h * 0.004f); p.setColor(0x55FFFFFF);
                for (int a = -2; a <= 2; a++) c.drawLine(bx, by, bx + a * hoehe * 0.2f + wiegen, by - hoehe * 0.95f, p);
            } else {                      // Kugelkoralle
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(bx, by - hoehe * 0.3f, hoehe * 0.45f, p);
                c.drawCircle(bx - hoehe * 0.35f, by - hoehe * 0.15f, hoehe * 0.3f, p);
                c.drawCircle(bx + hoehe * 0.38f, by - hoehe * 0.12f, hoehe * 0.28f, p);
                p.setColor(0x44FFFFFF); c.drawCircle(bx - hoehe * 0.1f, by - hoehe * 0.45f, hoehe * 0.12f, p);
            }
        }
        // Seeanemone: wiegende Fühler
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(h * 0.008f); p.setColor(0xFFFFB3E6);
        float ax = w * 0.52f, ay = h * 0.915f;
        for (int a = -5; a <= 5; a++) {
            float tx = ax + a * h * 0.012f + (float) Math.sin(t * 2 + a) * h * 0.012f;
            c.drawLine(ax + a * h * 0.008f, ay, tx, ay - h * 0.06f, p);
        }
        p.setStrokeCap(Paint.Cap.BUTT);
    }

    private void zeichnePlankton(Canvas c, int w, int h, float dt) {
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < plX.length; i++) {
            plY[i] -= plV[i] * dt; plX[i] += (float) Math.sin(t * 0.7f + i) * 0.004f * dt * 10;
            if (plY[i] < -0.02f) { plY[i] = 1.02f; plX[i] = zufall.nextFloat(); }
            float glanz = 0.5f + 0.5f * (float) Math.sin(t * 1.5f + i * 1.7f);
            p.setColor(((int) (90 + 120 * glanz) << 24) | 0x9FFFE8);
            c.drawCircle(plX[i] * w, plY[i] * h, plR[i] * h * (0.8f + glanz * 0.5f), p);
            p.setColor(((int) (30 * glanz) << 24) | 0x9FFFE8);
            c.drawCircle(plX[i] * w, plY[i] * h, plR[i] * h * 3, p);
        }
    }

    // ---------------------------------------------------------------------------------------------- Bewohner

    private void aktualisiereBewohner(float dt, int w, int h) {
        // Quallen: pulsieren, steigen langsam auf
        for (Qualle q : quallen) {
            q.leuchten = Math.max(0, q.leuchten - dt * 0.6f);
            q.schub = Math.max(0, q.schub - dt * 0.8f);
            q.y -= (0.012f + q.schub * 0.12f) * dt;
            if (q.y < -0.15f) q.y = 1.0f;
        }
        // Krabbe läuft seitlich über den Boden, versteckt sich nach dem Antippen
        krabbeKlappern = Math.max(0, krabbeKlappern - dt);
        if (krabbeVersteckt > 0) { krabbeVersteckt -= dt; }
        else {
            krabbeX += krabbeRichtung * 0.03f * dt;
            if (krabbeX > 0.88f) { krabbeX = 0.88f; krabbeRichtung = -1; }
            if (krabbeX < 0.12f) { krabbeX = 0.12f; krabbeRichtung = 1; }
        }
        // Muschel öffnet und schließt sich von selbst, nach dem Antippen bleibt sie länger offen
        muschelHalten = Math.max(0, muschelHalten - dt);
        boolean sollOffen = muschelHalten > 0 || ((t % 9f) > 5.5f);
        muschelOffen += ((sollOffen ? 1 : 0) - muschelOffen) * Math.min(1, dt * 3);
        // Schildkröte schwimmt gelegentlich durchs Bild
        if (!schildAktiv && t > schildNaechste) {
            schildAktiv = true; schildRichtung = zufall.nextBoolean() ? 1 : -1;
            schildX = schildRichtung > 0 ? -0.15f : 1.15f; schildY = 0.22f + zufall.nextFloat() * 0.18f;
        }
        if (schildAktiv) {
            schildKopfEin = Math.max(0, schildKopfEin - dt);
            schildDreh *= Math.max(0, 1 - dt * 2.5f);
            schildX += schildRichtung * 0.05f * dt;
            if (schildX > 1.2f || schildX < -0.2f) { schildAktiv = false; schildNaechste = t + 14 + zufall.nextFloat() * 14; }
        }
        // Delfin springt in großen Abständen in einem Bogen vorbei
        if (!delfinAktiv && t > delfinNaechster) {
            delfinAktiv = true; delfinFortschritt = 0; delfinNachRechts = zufall.nextBoolean(); delfinSalto = 0;
        }
        if (delfinAktiv) {
            delfinFortschritt += dt / 3.2f;
            delfinSalto = Math.max(0, delfinSalto - dt * 1.5f);
            if (delfinFortschritt >= 1) { delfinAktiv = false; delfinNaechster = t + 26 + zufall.nextFloat() * 20; }
        }
    }

    private void zeichneQualle(Canvas c, Qualle q, int w, int h) {
        float puls = (float) Math.sin(t * 2.2f + q.phase);
        float r = h * 0.055f * (1 + 0.08f * puls) * (1 + 0.15f * q.schub);
        float x = q.x * w + (float) Math.sin(t * 0.5f + q.phase) * w * 0.02f, y = q.y * h;
        boolean tief = szene == S_TIEF;
        int farbe = tief ? 0xFF8FF0FF : q.farbe;
        // Schein beim Leuchten (Antippen) oder in der Tiefsee
        float schein = Math.max(q.leuchten, tief ? 0.45f : 0);
        p.setStyle(Paint.Style.FILL);
        if (schein > 0) { p.setColor(((int) (70 * schein) << 24) | (farbe & 0xFFFFFF)); c.drawCircle(x, y, r * 2.6f, p); }
        // Fangarme
        p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeWidth(Math.max(2, r * 0.09f));
        p.setColor(((int) (150 + 80 * schein) << 24) | (farbe & 0xFFFFFF));
        for (int i = -2; i <= 2; i++) {
            pfad.reset(); pfad.moveTo(x + i * r * 0.35f, y);
            pfad.quadTo(x + i * r * 0.35f + (float) Math.sin(t * 2 + i + q.phase) * r * 0.35f, y + r * 1.0f, x + i * r * 0.3f + (float) Math.sin(t * 2.4f + i) * r * 0.3f, y + r * 2.0f);
            c.drawPath(pfad, p);
        }
        p.setStrokeCap(Paint.Cap.BUTT);
        // Schirm
        p.setStyle(Paint.Style.FILL); p.setColor(((int) (170 + 60 * schein) << 24) | (farbe & 0xFFFFFF));
        rect.set(x - r, y - r * (0.9f + 0.1f * puls), x + r, y + r * 0.5f); c.drawArc(rect, 180, 180, true, p);
        p.setColor(0x66FFFFFF); c.drawCircle(x - r * 0.35f, y - r * 0.35f, r * 0.14f, p);
    }

    private void zeichneSeestern(Canvas c, Seestern s, int w, int h, float dt) {
        s.dreh += s.tempo * dt; s.tempo = Math.max(0, s.tempo - dt * 120);
        float x = s.x * w, y = h * 0.935f, r = h * 0.045f * s.gr;
        c.save(); c.translate(x, y); c.rotate(s.dreh);
        p.setStyle(Paint.Style.FILL); p.setColor(s.farbe);
        pfad.reset();
        for (int i = 0; i < 10; i++) {
            float rr = (i % 2 == 0) ? r : r * 0.45f, a = (float) (-Math.PI / 2 + i * Math.PI / 5);
            float px = (float) Math.cos(a) * rr, py = (float) Math.sin(a) * rr;
            if (i == 0) pfad.moveTo(px, py); else pfad.lineTo(px, py);
        }
        pfad.close(); p.setPathEffect(new CornerPathEffect(r * 0.18f)); c.drawPath(pfad, p); p.setPathEffect(null);
        p.setColor(0x55FFFFFF);
        for (int i = 0; i < 5; i++) { double a = -Math.PI / 2 + i * 2 * Math.PI / 5; c.drawCircle((float) Math.cos(a) * r * 0.5f, (float) Math.sin(a) * r * 0.5f, r * 0.07f, p); }
        c.restore();
    }

    private void zeichneMuschel(Canvas c, int w, int h) {
        float x = w * 0.64f, y = h * 0.94f, r = h * 0.055f, auf = muschelOffen * 38;
        // untere Hälfte
        c.save(); c.translate(x, y);
        p.setStyle(Paint.Style.FILL); p.setColor(0xFFF2B8D2);
        rect.set(-r, -r * 0.9f, r, r * 0.9f); c.drawArc(rect, 0, 180, true, p);
        // Perle
        if (muschelOffen > 0.3f) {
            float glanz = 0.6f + 0.4f * (float) Math.sin(t * 4);
            p.setColor(((int) (60 * muschelOffen * glanz) << 24) | 0xFFFFFF); c.drawCircle(0, -r * 0.1f, r * 0.7f, p);
            p.setColor(0xFFFFFFFF); c.drawCircle(0, -r * 0.1f, r * 0.3f, p);
            p.setColor(0x88B8E8FF); c.drawCircle(r * 0.08f, -r * 0.05f, r * 0.14f, p);
        }
        // obere Hälfte klappt auf
        c.save(); c.rotate(-auf, -r, 0);
        p.setColor(0xFFE98FB5); rect.set(-r, -r * 0.9f, r, r * 0.9f); c.drawArc(rect, 180, 180, true, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(h * 0.004f); p.setColor(0x66FFFFFF);
        for (int i = -2; i <= 2; i++) c.drawLine(0, 0, i * r * 0.38f, -r * 0.85f, p);
        c.restore(); c.restore();
    }

    private void zeichneKrabbe(Canvas c, int w, int h) {
        float x = krabbeX * w, y = h * 0.93f, r = h * 0.035f;
        if (krabbeVersteckt > 0) {   // nur ein paar Sandbläschen zeigen, wo sie steckt
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(0x88FFFFFF);
            for (int i = 0; i < 3; i++) { float v = (t * 0.9f + i * 0.33f) % 1f; c.drawCircle(x + (float) Math.sin(t * 3 + i) * r * 0.3f, y - v * r * 2.5f, r * 0.15f + i, p); }
            return;
        }
        c.save(); c.translate(x, y);
        p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeWidth(r * 0.14f); p.setColor(0xFFC8412C);
        float lauf = (float) Math.sin(t * 9) * r * 0.12f * krabbeRichtung;
        for (int i = 0; i < 3; i++) {
            c.drawLine(-r * 0.5f - i * r * 0.18f, 0, -r * 1.1f - i * r * 0.25f + lauf, r * 0.65f, p);
            c.drawLine(r * 0.5f + i * r * 0.18f, 0, r * 1.1f + i * r * 0.25f - lauf, r * 0.65f, p);
        }
        float heben = krabbeKlappern > 0 ? (float) Math.sin(t * 16) * r * 0.25f : 0;   // winkt mit den Scheren
        for (int s = -1; s <= 1; s += 2) {
            float ax = s * r * 1.15f, ay = -r * 0.55f - (krabbeKlappern > 0 ? r * 0.5f : 0) + heben;
            c.drawLine(s * r * 0.7f, -r * 0.1f, ax, ay, p);
            p.setStyle(Paint.Style.FILL); c.drawCircle(ax, ay, r * 0.38f, p);
            p.setColor(0xFFE4533A); c.drawCircle(ax, ay, r * 0.3f, p);
            p.setColor(0xFFC8412C); p.setStyle(Paint.Style.STROKE);
        }
        p.setStyle(Paint.Style.FILL); p.setColor(0xFFE4533A);
        rect.set(-r * 0.85f, -r * 0.55f, r * 0.85f, r * 0.55f); c.drawOval(rect, p);
        p.setColor(0xFFFFFFFF); c.drawCircle(-r * 0.3f, -r * 0.7f, r * 0.2f, p); c.drawCircle(r * 0.3f, -r * 0.7f, r * 0.2f, p);
        p.setColor(0xFF222222); c.drawCircle(-r * 0.3f, -r * 0.7f, r * 0.1f, p); c.drawCircle(r * 0.3f, -r * 0.7f, r * 0.1f, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); c.drawArc(-r * 0.3f, -r * 0.2f, r * 0.3f, r * 0.2f, 20, 140, false, p);
        p.setStrokeCap(Paint.Cap.BUTT);
        c.restore();
    }

    private void zeichneSchildkroete(Canvas c, int w, int h) {
        if (!schildAktiv) return;
        float x = schildX * w, y = schildY * h + (float) Math.sin(t * 1.1f) * h * 0.012f, r = h * 0.06f;
        c.save(); c.translate(x, y); if (schildRichtung < 0) c.scale(-1, 1); c.rotate(schildDreh);
        float flosse = (float) Math.sin(t * 2.6f) * 22;
        p.setStyle(Paint.Style.FILL); p.setColor(0xFF7DBB6A);
        // Flossen
        c.save(); c.rotate(flosse, r * 0.4f, r * 0.5f); rect.set(r * 0.1f, r * 0.35f, r * 1.0f, r * 0.75f); c.drawOval(rect, p); c.restore();
        c.save(); c.rotate(-flosse, -r * 0.5f, r * 0.5f); rect.set(-r * 1.0f, r * 0.35f, -r * 0.1f, r * 0.75f); c.drawOval(rect, p); c.restore();
        // Kopf (fährt beim Antippen ein)
        float kopf = schildKopfEin > 0 ? 0.25f : 1f;
        rect.set(r * (0.75f - 0.15f * (1 - kopf)), -r * 0.3f, r * (0.75f + 0.55f * kopf), r * 0.15f); c.drawOval(rect, p);
        if (kopf > 0.5f) { p.setColor(0xFF222222); c.drawCircle(r * 1.15f, -r * 0.12f, r * 0.05f, p); }
        // Panzer
        p.setColor(0xFF8A6B3D); rect.set(-r * 0.95f, -r * 0.7f, r * 0.95f, r * 0.55f); c.drawArc(rect, 180, 180, true, p);
        p.setColor(0xFFA88650);
        for (int i = -1; i <= 1; i++) c.drawCircle(i * r * 0.5f, -r * 0.2f, r * 0.2f, p);
        c.drawCircle(0, -r * 0.5f, r * 0.17f, p);
        c.restore();
    }

    private void zeichneDelfin(Canvas c, int w, int h) {
        if (!delfinAktiv) return;
        float f = delfinFortschritt, rr = h * 0.075f;
        float xn = delfinNachRechts ? -0.1f + 1.2f * f : 1.1f - 1.2f * f;
        float x = xn * w, y = h * (0.30f - 0.20f * (float) Math.sin(Math.PI * f));
        float steigung = (float) Math.cos(Math.PI * f) * 0.9f;   // neigt sich entlang des Bogens
        float winkel = (float) Math.toDegrees(Math.atan(-steigung * 0.8f)) * (delfinNachRechts ? -1 : 1) * -1;
        c.save(); c.translate(x, y); if (!delfinNachRechts) c.scale(-1, 1);
        c.rotate(winkel + delfinSalto * 360);
        p.setStyle(Paint.Style.FILL); p.setColor(0xFF6C8FB5);
        // Schwanz
        pfad.reset(); pfad.moveTo(-rr * 1.3f, 0); pfad.lineTo(-rr * 2.0f, -rr * 0.55f); pfad.lineTo(-rr * 1.85f, 0); pfad.lineTo(-rr * 2.0f, rr * 0.55f); pfad.close();
        c.drawPath(pfad, p);
        // Rückenflosse
        pfad.reset(); pfad.moveTo(-rr * 0.2f, -rr * 0.5f); pfad.lineTo(-rr * 0.55f, -rr * 1.0f); pfad.lineTo(-rr * 0.8f, -rr * 0.45f); pfad.close();
        c.drawPath(pfad, p);
        // Körper und Schnauze
        rect.set(-rr * 1.5f, -rr * 0.55f, rr * 1.2f, rr * 0.55f); c.drawOval(rect, p);
        rect.set(rr * 0.85f, -rr * 0.12f, rr * 1.7f, rr * 0.22f); c.drawOval(rect, p);
        p.setColor(0xFFEAF2F8); rect.set(-rr * 1.3f, 0, rr * 1.1f, rr * 0.5f); c.drawOval(rect, p);
        p.setColor(0xFF1A1A1A); c.drawCircle(rr * 0.7f, -rr * 0.15f, rr * 0.09f, p);
        p.setColor(0xFFFFFFFF); c.drawCircle(rr * 0.73f, -rr * 0.18f, rr * 0.03f, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setStrokeCap(Paint.Cap.ROUND); p.setColor(0xFF2B4A6B);
        c.drawArc(rr * 0.9f, -rr * 0.05f, rr * 1.6f, rr * 0.3f, 10, 130, false, p);
        p.setStrokeCap(Paint.Cap.BUTT);
        c.restore();
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

    private void zeichneFunken(Canvas c, float dt) {
        p.setStyle(Paint.Style.FILL);
        for (int i = funken.size() - 1; i >= 0; i--) {
            Funke f = funken.get(i);
            f.leben -= dt * 1.1f;
            if (f.leben <= 0) { funken.remove(i); continue; }
            f.x += f.vx * dt; f.y += f.vy * dt; f.vy += 90 * dt; f.vx *= 0.98f;
            p.setColor(((int) (255 * f.leben) << 24) | (f.farbe & 0xFFFFFF));
            c.drawCircle(f.x, f.y, f.gr * f.leben, p);
        }
    }

    // ---------------------------------------------------------------------------------------------- Der Hai

    private void zeichneDenHai(Canvas c, int w, int h) {
        Art a = ARTEN[art];
        float s = Math.min(w / 560f, h / 430f) * 0.95f * a.groesse;
        s = Math.min(s, w * 0.95f / (410f * a.xs));
        float sx = w * 0.5f, sy = h * 0.45f + (float) Math.sin(t * 1.3f) * h * 0.02f;
        float einschwimmen = weich((t - artWechselT) / 1.5f);
        sx -= (1 - einschwimmen) * w * 0.7f;   // neue Haiart schwimmt von links herein
        float mund = 0, schwanzTempo = art == A_WAL ? 0.9f : art == A_KATZE ? 3.0f : 1.6f, schnuppern = 0, kopfWiegen = 0, plankton = 0;
        boolean kiemenBlasen = false;
        float groesse = 1 + 0.12f * hupf;
        switch (modus) {
            case M_ZAEHNE: mund = 0.65f + 0.3f * (float) Math.sin(t * 3.5f); break;
            case M_SCHWANZ: schwanzTempo = 7f; sx += (float) Math.sin(t * 2) * w * 0.03f; break;
            case M_NASE: schnuppern = (float) Math.sin(t * 14) * 0.5f + 0.5f; break;
            case M_GROSS: groesse *= 1.1f; break;
            case M_KIEMEN: kiemenBlasen = true; break;
            case M_HAMMER: kopfWiegen = (float) Math.sin(t * 3.2f); schwanzTempo = 3f; break;
            case M_PLANKTON: mund = 0.9f; plankton = 1; schwanzTempo = 1.2f; break;
            case M_RINGELN: sy += (float) Math.sin(t * 5) * h * 0.012f; schwanzTempo = 5f; break;
            default: break;
        }
        if (modus == M_GROSS) {
            float frac = (t * 0.06f) % 1f;
            zeichneHai(c, w * (0.1f + 0.8f * frac), h * 0.17f, s * 0.22f, 0, 3f, 0, false, ARTEN[A_FREUND], 0, 0);
        }
        zeichneHai(c, sx, sy, s * groesse, mund, schwanzTempo, schnuppern, kiemenBlasen, a, kopfWiegen, plankton);
        if (modus == M_SCHWANZ) zeichneTempoStriche(c, sx, sy, s);
        haiX = sx; haiY = sy; haiS = s * groesse;
    }

    private float haiX, haiY, haiS;

    private void zeichneTempoStriche(Canvas c, float sx, float sy, float s) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeWidth(s * 7); p.setColor(0x99FFFFFF);
        for (int i = 0; i < 4; i++) {
            float y = sy + (i - 1.5f) * s * 40;
            float v = ((t * 1.8f + i * 0.27f) % 1f);
            float x1 = sx - s * 270 - v * s * 160;
            c.drawLine(x1, y, x1 - s * 90, y, p);
        }
        p.setStrokeCap(Paint.Cap.BUTT);
    }

    /** Hai in Einheiten von etwa 460 x 260, Nase zeigt nach rechts. Aussehen je nach Haiart. */
    private void zeichneHai(Canvas c, float cx, float cy, float s, float mund, float schwanzTempo, float schnuppern, boolean kiemenBlasen, Art a, float kopfWiegen, float plankton) {
        boolean weiss = a == ARTEN[A_WEISS], hammer = a == ARTEN[A_HAMMER], katze = a == ARTEN[A_KATZE], wal = a == ARTEN[A_WAL];
        c.save();
        c.translate(cx, cy);
        c.scale(s * a.xs, s * a.ys);
        p.setPathEffect(new CornerPathEffect(14));
        p.setStyle(Paint.Style.FILL);
        float wedel = (float) Math.sin(t * schwanzTempo) * (wal ? 10f : 14f);

        // Schwanzflosse (dreht sich am Ansatz)
        c.save(); c.translate(-165, 0); c.rotate(wedel);
        p.setColor(a.flosse);
        pfad.reset(); pfad.moveTo(5, -8); pfad.lineTo(-85, -88); pfad.quadTo(-50, 0, -95, 85); pfad.lineTo(5, 8); pfad.close();
        c.drawPath(pfad, p);
        c.restore();

        // Rückenflosse(n) und Brustflosse
        p.setColor(a.flosse);
        pfad.reset();
        if (weiss) { pfad.moveTo(15, -66); pfad.lineTo(-45, -170); pfad.lineTo(-100, -58); }
        else if (hammer) { pfad.moveTo(25, -66); pfad.quadTo(-10, -150, -50, -175); pfad.quadTo(-70, -110, -100, -58); }
        else if (wal) { pfad.moveTo(-5, -66); pfad.quadTo(-45, -118, -105, -58); }
        else { pfad.moveTo(10, -66); pfad.lineTo(-55, -148); pfad.lineTo(-100, -58); }
        pfad.close();
        c.drawPath(pfad, p);
        if (katze) {   // zweite kleine Rückenflosse
            pfad.reset(); pfad.moveTo(-95, -58); pfad.lineTo(-125, -105); pfad.lineTo(-150, -50); pfad.close();
            c.drawPath(pfad, p);
        }
        c.save(); c.translate(20, 55); c.rotate((float) Math.sin(t * 2.2f) * 8f);
        pfad.reset(); pfad.moveTo(10, 0); pfad.lineTo(wal ? -80 : -60, wal ? 95 : 70); pfad.lineTo(-62, -12); pfad.close();
        c.drawPath(pfad, p);
        c.restore();

        // Körper
        c.save();
        if (schnuppern > 0) c.rotate(-3f * schnuppern, 0, 0);
        p.setColor(a.rueck);
        pfad.reset(); pfad.moveTo(215, 12);
        pfad.quadTo(120, -88, -40, -72); pfad.quadTo(-125, -62, -172, -12);
        pfad.lineTo(-172, 12); pfad.quadTo(-100, 62, -20, 74); pfad.quadTo(130, 72, 215, 12); pfad.close();
        c.drawPath(pfad, p);
        // heller Bauch
        p.setColor(a.bauch);
        pfad.reset(); pfad.moveTo(205, 20); pfad.quadTo(130, 76, -20, 72); pfad.quadTo(-100, 62, -165, 14);
        pfad.quadTo(-60, 44, 60, 42); pfad.quadTo(150, 40, 205, 20); pfad.close();
        c.drawPath(pfad, p);
        p.setPathEffect(null);

        // Muster: Punkte beim Walhai, Flecken beim Katzenhai
        p.setStyle(Paint.Style.FILL);
        if (wal) {
            p.setColor(0xDDFFFFFF);
            for (int zeile = 0; zeile < 5; zeile++) for (int sp = 0; sp < 12; sp++) {
                float px = -150 + sp * 26 + (zeile % 2) * 13, py = -52 + zeile * 20;
                float e = (px / 205f) * (px / 205f) + (py / 66f) * (py / 66f);
                if (e < 0.78f && py < 34) c.drawCircle(px, py, 3.6f, p);
            }
        } else if (katze) {
            p.setColor(0xFF7A5A32);
            float[][] fl = { { -120, -30 }, { -90, -10 }, { -60, -40 }, { -30, -15 }, { 0, -45 }, { 30, -20 }, { 60, -42 }, { -135, 5 }, { -100, 14 }, { -50, 8 }, { 10, 6 }, { 50, 0 }, { 90, -30 }, { 110, -12 } };
            for (float[] f : fl) c.drawCircle(f[0], f[1], 7, p);
        }

        // Kiemen
        p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeWidth(kiemenBlasen ? 7 : 4);
        p.setColor(kiemenBlasen ? 0xFF2D5E8E : 0x883B5C7C);
        int kiemenAnzahl = weiss || hammer ? 5 : wal ? 5 : 3;
        for (int i = 0; i < kiemenAnzahl; i++) { float kx = (wal ? 92 : 52) - i * (wal ? 10 : 15); c.drawLine(kx, -22 + i * 1.5f, kx - 6, 22 + i * 1.5f, p); }
        p.setStrokeCap(Paint.Cap.BUTT);

        // Gesicht je nach Haiart
        p.setStyle(Paint.Style.FILL);
        if (hammer) {
            // Der Kopf wie ein Hammer: breiter Balken mit je einem Auge an den Enden, wiegt hin und her
            c.save(); c.rotate(kopfWiegen * 7f, 160, 0);
            p.setColor(a.rueck); rect.set(160, -92, 238, 94); c.drawRoundRect(rect, 36, 36, p);
            p.setColor(a.bauch); rect.set(200, -60, 238, 70); c.drawRoundRect(rect, 18, 18, p);
            p.setColor(0xFFFFFFFF); c.drawCircle(214, -68, 15, p); c.drawCircle(214, 68, 15, p);
            p.setColor(0xFF1A1A1A); c.drawCircle(220, -67, 8.5f, p); c.drawCircle(220, 69, 8.5f, p);
            p.setColor(0xFFFFFFFF); c.drawCircle(223, -70, 3, p); c.drawCircle(223, 66, 3, p);
            p.setColor(0xFF4A423C); c.drawCircle(232, -22, 3.5f, p); c.drawCircle(232, 24, 3.5f, p);
            c.restore();
            p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeWidth(5); p.setColor(0xFF4A3B33);
            pfad.reset(); pfad.moveTo(168, 36); pfad.quadTo(150, 50, 118, 38);
            c.drawPath(pfad, p);
            p.setStrokeCap(Paint.Cap.BUTT);
        } else {
            // Wange, Auge, Nase
            p.setColor(0x66FF8FA3); c.drawCircle(105, 22, 15, p);
            if (katze) {
                p.setColor(0xFFE2E85A); c.save(); rect.set(104, -36, 140, 4); c.drawOval(rect, p);
                p.setColor(0xFF1A1A1A); rect.set(118, -34, 126, 2); c.drawOval(rect, p);   // schmale Katzenpupille
                p.setColor(0xFFFFFFFF); c.drawCircle(132, -26, 3.5f, p); c.restore();
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2.5f); p.setColor(0xFF5A4326);   // Schnurrhaar-ähnliche Barteln an der Nase
                c.drawLine(190, 8, 214, 22, p); c.drawLine(192, 14, 216, 34, p);
                p.setStyle(Paint.Style.FILL); p.setColor(0xFF7A5A32); c.drawCircle(178, 0, 3.5f, p);
            } else if (wal) {
                p.setColor(0xFFFFFFFF); c.drawCircle(140, -12, 9, p);
                p.setColor(0xFF1A1A1A); c.drawCircle(143, -11, 5.5f, p);
                p.setColor(0xFFFFFFFF); c.drawCircle(145, -14, 2, p);
            } else if (weiss) {
                p.setColor(0xFF1A1A1A); c.drawCircle(124, -20, 11, p);
                p.setColor(0xFFFFFFFF); c.drawCircle(128, -24, 4, p);
                p.setColor(0xFF3A4A59); c.drawCircle(178, 2, 3.5f, p);
            } else {
                p.setColor(0xFFFFFFFF); c.drawCircle(120, -20, 20, p);
                p.setColor(0xFF1A1A1A); c.drawCircle(126, -19, 11, p);
                p.setColor(0xFFFFFFFF); c.drawCircle(130, -23, 4, p);
                p.setColor(0xFF3B6C9C); c.drawCircle(176, 2, 3.5f, p);
            }
            // Mund: lächelt; bei "Zähne" weit offen mit Zähnen; Walhai: breites Maul vorne
            if (mund > 0.05f) {
                float oben = 28, tief = (wal ? 40 : 52) * mund;
                float links = wal ? 118 : 100, rechts = wal ? 215 : 200;
                p.setColor(0xFF8B1A2B);
                rect.set(links, oben - 4, rechts, oben + tief); c.drawOval(rect, p);
                p.setColor(0xFFFFFFFF);
                if (!wal) {
                    int zaehne = weiss ? 8 : 6; float breite = weiss ? 10 : 11, abstand = weiss ? 11 : 14;
                    for (int i = 0; i < zaehne; i++) {
                        float x = 108 + i * abstand;
                        pfad.reset(); pfad.moveTo(x, oben + 1); pfad.lineTo(x + breite, oben + 1); pfad.lineTo(x + breite / 2, oben + 1 + (weiss ? 20 : 16) * mund); pfad.close();
                        c.drawPath(pfad, p);
                        float ux = 112 + i * (abstand - 1), unten = oben + tief - 5;
                        pfad.reset(); pfad.moveTo(ux, unten); pfad.lineTo(ux + breite - 1, unten); pfad.lineTo(ux + breite / 2 - 0.5f, unten - (weiss ? 18 : 14) * mund); pfad.close();
                        c.drawPath(pfad, p);
                    }
                }
            } else {
                p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeWidth(5); p.setColor(0xFF2B4A6B);
                pfad.reset();
                if (wal) { pfad.moveTo(214, 24); pfad.quadTo(170, 40, 118, 30); }
                else { pfad.moveTo(196, 22); pfad.quadTo(150, 56, 100, 32); }
                c.drawPath(pfad, p);
                p.setStrokeCap(Paint.Cap.BUTT);
            }
        }
        c.restore();

        // Plankton, das der Walhai schluckt: Teilchen treiben zum Maul
        if (plankton > 0) {
            p.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 14; i++) {
                float v = (t * 0.5f + i * 0.0714f) % 1f;
                float px = 215 + (1 - v) * 190, py = 28 + (float) Math.sin(i * 2.3f + t) * 36 * (1 - v);
                p.setColor(((int) (200 * Math.min(1, v * 3)) << 24) | 0xA8FFE0);
                c.drawCircle(px, py, 3 + (i % 3) * 1.5f, p);
            }
        }

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

    // ---------------------------------------------------------------------------------------------- Untertitel und Menü

    private void zeichneUntertitel(Canvas c, int w, int h) {
        textFarbe.setTextSize(Math.min(h * 0.034f, w * 0.045f));   // klein gehalten, damit mehr Fläche zum Antippen bleibt
        int breite = (int) (w * 0.8f);
        if (textLayout == null || !layoutText.equals(untertitel) || layoutBreite != breite) {
            textLayout = StaticLayout.Builder.obtain(untertitel, 0, untertitel.length(), textFarbe, breite)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER).build();
            layoutText = untertitel; layoutBreite = breite;
        }
        float rand = h * 0.016f;
        float boxH = textLayout.getHeight() + rand * 2;
        float boxY = h * 0.025f;   // oben, damit der Boden mit Krabbe, Seesternen und Muschel frei bleibt
        p.setStyle(Paint.Style.FILL); p.setColor(0xEEFFFFFF);
        rect.set(w * 0.05f - rand, boxY, w * 0.85f + rand, boxY + boxH);
        c.drawRoundRect(rect, rand * 2, rand * 2, p);
        c.save(); c.translate(w * 0.05f, boxY + rand); textLayout.draw(c); c.restore();
    }

    private void zeichneMenueKnopf(Canvas c, int w, int h) {
        float k = Math.min(w, h) * 0.09f, rand = Math.min(w, h) * 0.025f;
        menueKnopf.set(w - rand - k, rand, w - rand, rand + k);
        p.setStyle(Paint.Style.FILL); p.setColor(0xAAFFFFFF);
        c.drawRoundRect(menueKnopf, k * 0.25f, k * 0.25f, p);
        p.setColor(0xFF1B3A57);
        for (int i = 0; i < 3; i++) {
            float y = menueKnopf.top + k * (0.28f + 0.22f * i);
            rect.set(menueKnopf.left + k * 0.22f, y - k * 0.04f, menueKnopf.right - k * 0.22f, y + k * 0.04f);
            c.drawRoundRect(rect, k * 0.04f, k * 0.04f, p);
        }
    }

    private void zeichneMenue(Canvas c, int w, int h) {
        p.setStyle(Paint.Style.FILL); p.setColor(0xAA000820);
        c.drawRect(0, 0, w, h, p);
        float kante = Math.min(w, h), tw = Math.min(w * 0.88f, kante * 1.15f), th = Math.min(h * 0.9f, kante * 1.0f);
        menueTafel.set((w - tw) / 2, (h - th) / 2, (w + tw) / 2, (h + th) / 2);
        p.setColor(0xF2173A63); c.drawRoundRect(menueTafel, kante * 0.04f, kante * 0.04f, p);
        float zeile = th / 6.6f, bh = zeile * 0.78f, bx0 = menueTafel.left + tw * 0.08f, bx1 = menueTafel.right - tw * 0.08f;
        menueText.setTextSize(Math.min(zeile * 0.4f, tw * 0.07f));
        menueText.setColor(0xFFBFE6FF);
        c.drawText("Menü", menueTafel.centerX(), menueTafel.top + zeile * 0.72f, menueText);
        boolean musik = app.istMusikAn(), erzaehlen = app.istErzaehlenAn(), beendenFrage = t < beendenBis;
        knopf(c, menueMusik, bx0, menueTafel.top + zeile * 1.1f, bx1, bh, musik ? 0xFF2E9E5B : 0xFF6B7A8C, "Musik: " + (musik ? "AN" : "AUS"));
        knopf(c, menueErzaehlen, bx0, menueTafel.top + zeile * 2.1f, bx1, bh, erzaehlen ? 0xFF2E9E5B : 0xFF6B7A8C, "Erzählen (Text + Stimme): " + (erzaehlen ? "AN" : "AUS"));
        knopf(c, menueNeu, bx0, menueTafel.top + zeile * 3.1f, bx1, bh, 0xFFE0892E, "Geschichte neu starten");
        knopf(c, menueBeenden, bx0, menueTafel.top + zeile * 4.1f, bx1, bh, beendenFrage ? 0xFFD64545 : 0xFF9A5A5A, beendenFrage ? "Wirklich beenden? Nochmal tippen" : "App beenden");
        knopf(c, menueZurueck, bx0, menueTafel.top + zeile * 5.1f, bx1, bh, 0xFF3B7DD8, "Weiterspielen");
    }

    private void knopf(Canvas c, RectF ziel, float x0, float y0, float x1, float hoehe, int farbe, String text) {
        ziel.set(x0, y0, x1, y0 + hoehe);
        p.setStyle(Paint.Style.FILL); p.setColor(farbe);
        c.drawRoundRect(ziel, hoehe * 0.3f, hoehe * 0.3f, p);
        menueText.setColor(0xFFFFFFFF);
        float breiteMax = (x1 - x0) * 0.92f, original = menueText.getTextSize();
        while (menueText.measureText(text) > breiteMax && menueText.getTextSize() > original * 0.5f) menueText.setTextSize(menueText.getTextSize() * 0.93f);
        Paint.FontMetrics fm = menueText.getFontMetrics();
        c.drawText(text, ziel.centerX(), ziel.centerY() - (fm.ascent + fm.descent) / 2, menueText);
        menueText.setTextSize(original);
    }

    /** Menü öffnen oder schließen (auch für die Zurück-Taste). */
    void menueUmschalten() { menueOffen = !menueOffen; beendenBis = -1; }

    private void menueTippen(float x, float y) {
        if (menueMusik.contains(x, y)) { app.setzeMusik(!app.istMusikAn()); pop(); }
        else if (menueErzaehlen.contains(x, y)) { app.setzeErzaehlen(!app.istErzaehlenAn()); pop(); }
        else if (menueNeu.contains(x, y)) { geschichteNeuStarten(); pop(); }
        else if (menueBeenden.contains(x, y)) {
            if (t < beendenBis) app.beenden(); else { beendenBis = t + 3f; pop(); }   // erst nach dem zweiten Tippen
        }
        else if (menueZurueck.contains(x, y) || !menueTafel.contains(x, y)) { menueOffen = false; beendenBis = -1; }
    }

    // ---------------------------------------------------------------------------------------------- Antippen

    /** Zurück an den Anfang: Hai-Freund im Tageslicht, Begrüßung wird wieder gesprochen. */
    private void geschichteNeuStarten() {
        menueOffen = false; beendenBis = -1;
        geschNr = -1; modus = M_RUHIG; hupf = 1;
        if (art != A_FREUND) { art = A_FREUND; artWechselT = t; }
        if (szene != S_TAG) { szeneAlt = szene; szene = S_TAG; szeneWechselT = t; }
        untertitel = "Tipp mich an!";
        app.sprich(BEGRUESSUNG);
    }

    private void naechsteGeschichte() {
        geschNr = (geschNr + 1) % GESCHICHTEN.length;
        Geschichte g = GESCHICHTEN[geschNr];
        if (g.art != art) { art = g.art; artWechselT = t; }
        if (g.szene != szene) { szeneAlt = szene; szene = g.szene; szeneWechselT = t; }
        modus = g.modus; modusBis = t + 9f; hupf = 1;
        untertitel = g.text;
        app.sprich(g.text);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int aktion = e.getActionMasked();
        int w = getWidth(), h = getHeight();
        if (aktion == MotionEvent.ACTION_MOVE && !menueOffen) {   // Wischen scheucht die kleinen Fische auf
            for (int i = 0; i < e.getPointerCount(); i++) {
                for (Fisch f : fische) {
                    if (Math.abs(e.getX(i) - f.x * w) < h * 0.12f && Math.abs(e.getY(i) - f.y * h) < h * 0.1f && f.schub < 0.5f) { f.schub = 1; }
                }
            }
            return true;
        }
        if (aktion != MotionEvent.ACTION_DOWN && aktion != MotionEvent.ACTION_POINTER_DOWN) return true;
        int i = e.getActionIndex();
        float x = e.getX(i), y = e.getY(i);
        if (menueOffen) { menueTippen(x, y); return true; }
        if (menueKnopf.contains(x, y)) { menueUmschalten(); pop(); return true; }

        // Delfin im Sprung: Salto und Funken
        if (delfinAktiv) {
            float f = delfinFortschritt, dx = (delfinNachRechts ? -0.1f + 1.2f * f : 1.1f - 1.2f * f) * w, dy = h * (0.30f - 0.20f * (float) Math.sin(Math.PI * f));
            if (Math.abs(x - dx) < h * 0.2f && Math.abs(y - dy) < h * 0.14f) { delfinSalto = 1; funkeln(dx, dy, 22, 0xFFFFFFFF, 0xFF9FE3FF, 0xFFFFE08A); klingel(); return true; }
        }
        // Quallen leuchten auf und huschen nach oben
        for (Qualle q : quallen) {
            if (Math.abs(x - q.x * w) < h * 0.12f && Math.abs(y - q.y * h) < h * 0.14f) { q.leuchten = 1; q.schub = 1; funkeln(q.x * w, q.y * h, 14, q.farbe, 0xFFFFFFFF); pop(); return true; }
        }
        // Seesterne drehen sich
        for (Seestern s : seesterne) {
            if (Math.abs(x - s.x * w) < h * 0.09f && Math.abs(y - h * 0.935f) < h * 0.09f) { s.tempo = 700; funkeln(s.x * w, h * 0.92f, 14, 0xFFFFE08A, 0xFFFFFFFF); pop(); return true; }
        }
        // Krabbe winkt und verschwindet im Sand
        if (krabbeVersteckt <= 0 && Math.abs(x - krabbeX * w) < h * 0.1f && Math.abs(y - h * 0.93f) < h * 0.09f) { krabbeKlappern = 0.9f; krabbeVersteckt = 4f; funkeln(krabbeX * w, h * 0.93f, 10, 0xFFEBD28F, 0xFFFFFFFF); pop(); return true; }
        // Muschel öffnet sich und zeigt die Perle
        if (Math.abs(x - w * 0.64f) < h * 0.1f && Math.abs(y - h * 0.94f) < h * 0.1f) { muschelHalten = 3.5f; funkeln(w * 0.64f, h * 0.92f, 20, 0xFFFFFFFF, 0xFFFFE08A, 0xFFB8E8FF); klingel(); return true; }
        // Schildkröte zieht den Kopf ein
        if (schildAktiv && Math.abs(x - schildX * w) < h * 0.12f && Math.abs(y - schildY * h) < h * 0.1f) { schildKopfEin = 1.6f; schildDreh = 25; funkeln(schildX * w, schildY * h, 10, 0xFF7DBB6A, 0xFFFFFFFF); pop(); return true; }
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
        Art a = ARTEN[art];
        if (Math.abs(x - haiX) < a.trefferBreite * haiS && Math.abs(y - haiY) < a.trefferHoehe * haiS) { naechsteGeschichte(); return true; }
        // sonst: ein paar Blasen und Glitzer an der Stelle
        for (int k = 0; k < 4; k++) blaseAn(x + (zufall.nextFloat() - 0.5f) * h * 0.08f, y + (zufall.nextFloat() - 0.5f) * h * 0.05f, w, h);
        funkeln(x, y, 6, 0xFFFFFFFF, 0xFF9FE3FF);
        pop();
        return true;
    }
}
