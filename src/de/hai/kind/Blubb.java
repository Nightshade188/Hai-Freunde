package de.hai.kind;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.util.Random;

/**
 * Blubber-Geräusche für das Antippen (statt Piepsen): kurze Blasen, deren Ton beim Aufsteigen höher wird.
 * Die Klänge werden einmal berechnet und bei Bedarf als kurze AudioTracks abgespielt. Keine Berechtigungen nötig.
 */
final class Blubb {
    private static final int RATE = 22050;
    private final Random r = new Random();
    private final short[][] kurz = new short[4][];   // einzelne Blasen in verschiedenen Tonhöhen
    private final short[] lang;                       // mehrere Blasen hintereinander (Belohnung)

    Blubb() {
        float[] hoehe = { 330, 410, 500, 600 };
        for (int i = 0; i < kurz.length; i++) kurz[i] = blase(hoehe[i], 0.11f, 0.5f);
        short[] a = blase(380, 0.12f, 0.5f), b = blase(520, 0.10f, 0.45f), c = blase(700, 0.09f, 0.4f);
        lang = new short[(int) (RATE * 0.42f)];
        mische(lang, a, 0);
        mische(lang, b, (int) (RATE * 0.11f));
        mische(lang, c, (int) (RATE * 0.21f));
    }

    private static void mische(short[] ziel, short[] quelle, int ab) {
        for (int i = 0; i < quelle.length && ab + i < ziel.length; i++) ziel[ab + i] = (short) (ziel[ab + i] + quelle[i]);
    }

    /** Eine Blase: Sinuston, dessen Höhe schnell ansteigt, mit sofortigem Einsatz und weichem Ausklingen. */
    private static short[] blase(float start, float dauer, float lautstaerke) {
        int n = (int) (RATE * dauer);
        short[] s = new short[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double k = (double) i / n;
            double f = start * (1 + 1.6 * k * k + 0.5 * k);
            phase += 2 * Math.PI * f / RATE;
            double huelle = Math.min(1, i / 60.0) * Math.exp(-4.5 * k);
            double ton = Math.sin(phase) + 0.25 * Math.sin(2 * phase);
            s[i] = (short) (ton * huelle * lautstaerke * 20000);
        }
        return s;
    }

    /** Ein kurzes Blubb (zufällige Tonhöhe). */
    void blubb() { spiele(kurz[r.nextInt(kurz.length)]); }

    /** Ein längeres Blubbern aus drei Blasen, z. B. wenn etwas Besonderes passiert. */
    void blubbern() { spiele(lang); }

    private void spiele(short[] daten) {
        try {
            final AudioTrack spur = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(daten.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
            spur.write(daten, 0, daten.length);
            spur.setNotificationMarkerPosition(daten.length - 1);
            spur.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override public void onMarkerReached(AudioTrack t) { t.release(); }
                @Override public void onPeriodicNotification(AudioTrack t) { }
            });
            spur.play();
        } catch (RuntimeException e) {
            // Ton nicht verfügbar: still weiter
        }
    }
}
