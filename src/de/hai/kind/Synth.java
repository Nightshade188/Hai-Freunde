package de.hai.kind;

import java.util.Random;

/**
 * Erzeugt beruhigende Meeresmusik in Echtzeit (keine Audiodateien, reines Rechnen, ohne Android-Klassen):
 * ein weicher Akkord-Klangteppich in D-Dur, leises Wellenrauschen, einzelne sanfte Glockentöne einer Pentatonik
 * mit Echo und gelegentlich ein langsamer "Walgesang"-Ton. Die Musik wiederholt sich nicht, sondern entsteht laufend neu.
 */
final class Synth {
    static final int RATE = 22050;
    private static final double ZWEI_PI = Math.PI * 2;

    // Akkorde als MIDI-Noten (D3 = 50): Dmaj7, Hm7, Gmaj7, A6/9 - klingen offen und ruhig
    private static final int[][] AKKORDE = { { 50, 57, 61, 66 }, { 47, 54, 57, 62 }, { 43, 50, 54, 59 }, { 45, 52, 59, 66 } };
    // D-Dur-Pentatonik für die Glockentöne (D4 bis A5)
    private static final int[] PENTA = { 62, 64, 66, 69, 71, 74, 76, 78, 81 };
    private static final double AKKORD_DAUER = 10.0, UEBERBLENDEN = 4.0;

    private final Random r = new Random();
    private long n = 0;                       // Zähler der erzeugten Samples
    private final double[] phase = new double[24];   // 8 Töne (zwei Akkorde, die sich überblenden) mit je drei Wellen
    private double rauschen = 0;
    private final float[] echo = new float[(int) (RATE * 0.43)];
    private int echoPos = 0;
    private double naechsteGlocke = 1.0;
    private final double[] glockeFreq = new double[6], glockeAlter = new double[6], glockeAmp = new double[6], glockePhase = new double[6];
    private double gesangStart = -1, gesangFreq = 0, gesangPhase = 0;
    private double naechsterGesang = 18.0;

    private static double frequenz(int midi) { return 440.0 * Math.pow(2, (midi - 69) / 12.0); }

    /** Füllt den Puffer mit Samples (16 Bit, mono). */
    void fuelle(short[] puffer, int anzahl) {
        for (int i = 0; i < anzahl; i++) {
            double t = (double) n / RATE;
            double s = klangteppich(t) + wellen(t) + gesang(t);
            double glocken = glocken(t);
            // Echo nur für die Glocken und etwas vom Klangteppich: weicher, räumlicher Klang
            float verzoegert = echo[echoPos];
            double mix = s + glocken + verzoegert * 0.55;
            echo[echoPos] = (float) ((glocken + s * 0.15) + verzoegert * 0.42);
            if (++echoPos >= echo.length) echoPos = 0;
            // weicher Begrenzer, damit nichts übersteuert
            double ausgang = mix / (1 + Math.abs(mix));
            puffer[i] = (short) Math.round(ausgang * 1.25 * 32767);
            n++;
        }
    }

    private double klangteppich(double t) {
        double ab = t % AKKORD_DAUER;
        int zaehler = (int) (t / AKKORD_DAUER), nr = zaehler % AKKORDE.length, naechster = (nr + 1) % AKKORDE.length;
        int platz = (zaehler % 2) * 4, platzNaechster = ((zaehler + 1) % 2) * 4;   // jeder Akkord behält seine Wellen, damit beim Wechsel nichts knackt
        double rest = AKKORD_DAUER - ab;
        double ueber = rest < UEBERBLENDEN ? 0.5 - 0.5 * Math.cos(Math.PI * (1 - rest / UEBERBLENDEN)) : 0;   // 0 bis 1 gegen Ende des Akkords
        double summe = 0;
        for (int k = 0; k < 4; k++) {
            summe += (1 - ueber) * ton(AKKORDE[nr][k], platz + k);
            if (ueber > 0) summe += ueber * ton(AKKORDE[naechster][k], platzNaechster + k);
        }
        // sehr langsames Atmen der Lautstärke
        double atmen = 0.8 + 0.2 * Math.sin(ZWEI_PI * t / 13.0);
        return summe * 0.075 * atmen;
    }

    /** Weicher Ton aus zwei leicht verstimmten Sinuswellen plus einer leisen Oktave darunter. */
    private double ton(int midi, int platz) {
        double f = frequenz(midi);
        int p = platz * 3;
        phase[p] = (phase[p] + ZWEI_PI * f / RATE) % ZWEI_PI;
        phase[p + 1] = (phase[p + 1] + ZWEI_PI * f * 1.003 / RATE) % ZWEI_PI;
        phase[p + 2] = (phase[p + 2] + ZWEI_PI * f * 0.5 / RATE) % ZWEI_PI;
        return Math.sin(phase[p]) * 0.5 + Math.sin(phase[p + 1]) * 0.5 + Math.sin(phase[p + 2]) * 0.25;
    }

    /** Leises Wellenrauschen: tiefpassgefiltertes Rauschen, das langsam an- und abschwillt. */
    private double wellen(double t) {
        rauschen += 0.035 * (r.nextGaussian() * 0.6 - rauschen);
        double welle = 0.5 + 0.5 * Math.sin(ZWEI_PI * t / 9.0 - 1.2);
        double welle2 = 0.5 + 0.5 * Math.sin(ZWEI_PI * t / 5.3 + 0.7);
        double h = welle * welle * 0.8 + welle2 * 0.2;
        return rauschen * (0.05 + 0.20 * h);
    }

    private double glocken(double t) {
        if (t >= naechsteGlocke) {
            for (int k = 0; k < glockeFreq.length; k++) {
                if (glockeAlter[k] > 5 || glockeFreq[k] == 0) {
                    glockeFreq[k] = frequenz(PENTA[r.nextInt(PENTA.length)]);
                    glockeAlter[k] = 0; glockeAmp[k] = 0.45 + r.nextDouble() * 0.5; glockePhase[k] = 0;
                    break;
                }
            }
            naechsteGlocke = t + 1.8 + r.nextDouble() * 3.8;
        }
        double summe = 0;
        for (int k = 0; k < glockeFreq.length; k++) {
            if (glockeFreq[k] == 0 || glockeAlter[k] > 5) continue;
            double a = glockeAlter[k];
            double hue = Math.min(1, a / 0.006) * Math.exp(-a / 1.3);   // sehr kurzes Einschwingen, dann langes Ausklingen
            glockePhase[k] = (glockePhase[k] + ZWEI_PI * glockeFreq[k] / RATE) % ZWEI_PI;
            double klang = Math.sin(glockePhase[k]) + 0.28 * Math.sin(2 * glockePhase[k]) * Math.exp(-a / 0.5);
            summe += klang * hue * glockeAmp[k];
            glockeAlter[k] += 1.0 / RATE;
        }
        return summe * 0.11;
    }

    /** Gelegentlich ein langer, tiefer Ton mit sanftem Gleiten und leichtem Vibrato, wie ein ferner Wal. */
    private double gesang(double t) {
        if (gesangStart < 0 && t >= naechsterGesang) { gesangStart = t; gesangFreq = frequenz(50 + 7 * r.nextInt(2)); gesangPhase = 0; }
        if (gesangStart < 0) return 0;
        double a = t - gesangStart, dauer = 7.0;
        if (a > dauer) { gesangStart = -1; naechsterGesang = t + 22 + r.nextDouble() * 25; return 0; }
        double huellkurve = Math.sin(Math.PI * a / dauer);
        double f = gesangFreq * (1 + 0.12 * Math.sin(Math.PI * a / dauer)) * (1 + 0.004 * Math.sin(ZWEI_PI * 5.2 * a));
        gesangPhase = (gesangPhase + ZWEI_PI * f / RATE) % ZWEI_PI;
        return Math.sin(gesangPhase) * huellkurve * huellkurve * 0.07;
    }
}
