package de.hai.kind;

/**
 * Inhalte der App: Haiarten, Szenen (Hintergründe) und die Geschichten, die der Hai erzählt.
 * Jede Geschichte gehört zu einer Haiart und einer Szene und löst eine Bewegung des Hais aus.
 */
final class Inhalt {
    private Inhalt() { }

    // Szenen (Hintergründe)
    static final int S_TAG = 0, S_RIFF = 1, S_TANG = 2, S_TIEF = 3, S_ABEND = 4, S_ANZAHL = 5;

    // Bewegungen des Hais
    static final int M_RUHIG = 0, M_ZAEHNE = 1, M_SCHWANZ = 2, M_NASE = 3, M_GROSS = 4, M_KIEMEN = 5, M_HAMMER = 6, M_PLANKTON = 7, M_RINGELN = 8;

    // Haiarten
    static final int A_FREUND = 0, A_WEISS = 1, A_HAMMER = 2, A_KATZE = 3, A_WAL = 4;

    /** Aussehen einer Haiart. xs/ys strecken den Körper, groesse ist der Anzeigemaßstab. */
    static final class Art {
        final String name;
        final int rueck, bauch, flosse;
        final float xs, ys, groesse, trefferBreite, trefferHoehe;
        Art(String name, int rueck, int bauch, int flosse, float xs, float ys, float groesse, float trefferBreite, float trefferHoehe) {
            this.name = name; this.rueck = rueck; this.bauch = bauch; this.flosse = flosse;
            this.xs = xs; this.ys = ys; this.groesse = groesse; this.trefferBreite = trefferBreite; this.trefferHoehe = trefferHoehe;
        }
    }

    static final Art[] ARTEN = {
            new Art("Hai-Freund", 0xFF6FA8DC, 0xFFEAF4FB, 0xFF5E8FC2, 1.00f, 1.00f, 1.00f, 250, 150),
            new Art("Weißer Hai", 0xFF8A9CAD, 0xFFF4F7FA, 0xFF6B7B8B, 1.05f, 0.95f, 1.05f, 260, 150),
            new Art("Hammerhai", 0xFF9A8F86, 0xFFF1EBE4, 0xFF7A7068, 0.95f, 0.85f, 1.00f, 260, 160),
            new Art("Katzenhai", 0xFFC9A66B, 0xFFF3E6C8, 0xFFA98750, 1.10f, 0.60f, 0.70f, 210, 100),
            new Art("Walhai", 0xFF3E6A8F, 0xFFD9E6F0, 0xFF2F5575, 1.15f, 1.05f, 1.45f, 300, 190),
    };

    static final class Geschichte {
        final int art, szene, modus;
        final String text;
        Geschichte(int art, int szene, int modus, String text) { this.art = art; this.szene = szene; this.modus = modus; this.text = text; }
    }

    static final String BEGRUESSUNG = "Hallo! Ich bin der Hai. Tipp mich an!";

    static final Geschichte[] GESCHICHTEN = {
            // Hai-Freund: allgemeine Geschichten über Haie
            new Geschichte(A_FREUND, S_TAG, M_ZAEHNE, "Der Hai hat ganz viele Zähne. Wenn ein Zahn ausfällt, wächst ein neuer!"),
            new Geschichte(A_FREUND, S_TAG, M_SCHWANZ, "Mit seinem großen Schwanz schwimmt der Hai ganz schnell. Swusch!"),
            new Geschichte(A_FREUND, S_TAG, M_NASE, "Der Hai kann super riechen. Schnuff, schnuff, schnuff!"),
            new Geschichte(A_FREUND, S_TAG, M_GROSS, "Es gibt kleine Haie und riesengroße Haie. Der Walhai ist so groß wie ein Bus!"),
            new Geschichte(A_FREUND, S_TAG, M_KIEMEN, "Der Hai ist ein Fisch. Im Wasser atmet er mit Kiemen. Blubb, blubb!"),
            new Geschichte(A_FREUND, S_ABEND, M_RINGELN, "Haie gibt es schon viel länger als Dinosaurier. Das ist ganz schön lange her!"),
            new Geschichte(A_FREUND, S_ABEND, M_RINGELN, "Der Hai hat keine Knochen. Sein Körper ist aus Knorpel, so weich wie dein Ohr."),
            // Weißer Hai
            new Geschichte(A_WEISS, S_TAG, M_NASE, "Das ist der Weiße Hai. Sein Bauch ist weiß wie Schnee und sein Rücken ist grau."),
            new Geschichte(A_WEISS, S_TAG, M_ZAEHNE, "Der Weiße Hai hat viele Reihen Zähne. Ganz vorne und dahinter noch mehr. Haps!"),
            new Geschichte(A_WEISS, S_TAG, M_SCHWANZ, "Der Weiße Hai ist stark und schwimmt weit durch das große blaue Meer."),
            // Hammerhai
            new Geschichte(A_HAMMER, S_RIFF, M_HAMMER, "Das ist der Hammerhai. Sein Kopf sieht aus wie ein Hammer! Bumm!"),
            new Geschichte(A_HAMMER, S_RIFF, M_HAMMER, "Mit dem breiten Kopf kann der Hammerhai besonders gut gucken und riechen."),
            new Geschichte(A_HAMMER, S_RIFF, M_SCHWANZ, "Hammerhaie schwimmen oft in großen Gruppen. Zusammen macht es mehr Spaß!"),
            // Katzenhai
            new Geschichte(A_KATZE, S_TANG, M_RINGELN, "Das ist der Katzenhai. Er ist klein und hat Augen wie eine Katze. Miau? Nein, blubb!"),
            new Geschichte(A_KATZE, S_TANG, M_NASE, "Der Katzenhai versteckt sich tagsüber gern zwischen Steinen und Seetang. Pssst!"),
            new Geschichte(A_KATZE, S_TANG, M_RINGELN, "Katzenhai-Babys wachsen in einer Hülle, die wie ein kleines Kissen aussieht."),
            // Walhai
            new Geschichte(A_WAL, S_TIEF, M_PLANKTON, "Das ist der Walhai. Er ist der größte Fisch der Welt, so groß wie ein Bus!"),
            new Geschichte(A_WAL, S_TIEF, M_PLANKTON, "Der Walhai beißt nicht. Er schluckt ganz winzig kleines Futter, das Plankton. Schlurf!"),
            new Geschichte(A_WAL, S_TIEF, M_GROSS, "Auf dem Rücken hat der Walhai weiße Punkte. Jeder Walhai hat andere Punkte, wie ein Fingerabdruck."),
            // zurück zum Freund
            new Geschichte(A_FREUND, S_RIFF, M_SCHWANZ, "Manche Haie schwimmen immer weiter, auch wenn sie sich ausruhen. Schwimm, schwimm, schwimm!"),
    };
}
