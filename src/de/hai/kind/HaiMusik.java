package de.hai.kind;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/**
 * Spielt die Meeresmusik aus {@link Synth} im Hintergrund ab (eigener Thread, AudioTrack im Streaming-Modus).
 * Während der Hai spricht, wird die Musik leiser gestellt ("Ducking"). Keine Berechtigungen nötig.
 */
final class HaiMusik implements Runnable {
    private volatile boolean laeuft = false;
    private volatile float ziel = 1f;        // 1 = volle Musiklautstärke, kleiner = leiser (der Hai spricht)
    private Thread faden;

    synchronized void start() {
        if (laeuft) return;
        laeuft = true;
        faden = new Thread(this, "hai-musik");
        faden.setDaemon(true);
        faden.start();
    }

    synchronized void stop() {
        laeuft = false;
        Thread f = faden;
        faden = null;
        if (f != null) { try { f.join(1500); } catch (InterruptedException ignoriert) { Thread.currentThread().interrupt(); } }
    }

    boolean spielt() { return laeuft; }

    /** Musik leiser stellen, solange der Hai spricht, und danach wieder anheben. */
    void leiser(boolean sprechen) { ziel = sprechen ? 0.30f : 1f; }

    @Override
    public void run() {
        AudioTrack spur = null;
        try {
            int mindest = AudioTrack.getMinBufferSize(Synth.RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int groesse = Math.max(mindest, Synth.RATE);   // etwa eine halbe Sekunde Vorrat (16 Bit = 2 Byte je Sample)
            spur = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Synth.RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(groesse)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
            spur.play();
            Synth synth = new Synth();
            short[] puffer = new short[1024];
            float lautstaerke = 0f;   // beim Start sanft einblenden
            float volumen = 0.55f;    // Grundlautstärke der Musik, damit die Stimme klar bleibt
            while (laeuft) {
                synth.fuelle(puffer, puffer.length);
                lautstaerke += (ziel - lautstaerke) * 0.06f;
                spur.setVolume(volumen * lautstaerke);
                int geschrieben = spur.write(puffer, 0, puffer.length);
                if (geschrieben < 0) break;
            }
        } catch (RuntimeException e) {
            // Audio nicht verfügbar: die App läuft ohne Musik weiter
        } finally {
            if (spur != null) {
                try { spur.stop(); } catch (RuntimeException ignoriert) { /* schon gestoppt */ }
                spur.release();
            }
            laeuft = false;
        }
    }
}
