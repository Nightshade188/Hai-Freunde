# APK – Hai-Freunde

Kleine Android-App für Kinder von 2 bis 3 Jahren: Ein freundlicher Hai erzählt etwas über Haie.
Bedienung nur durch Antippen, kein Lesen nötig, ohne Internet und ohne Berechtigungen.

## Was die App kann
- **Hai antippen:** nächste Geschichte (Bild, Bewegung und gesprochener Satz). Dabei wechseln die Haiart und der Hintergrund:
  Hai-Freund, Weißer Hai, Hammerhai, Katzenhai und Walhai (20 kurze Geschichten, z. B. über Zähne, Kiemen, Knorpel, Plankton).
- **Hintergründe:** Tag, Korallenriff, Seetangwald mit Felsen, dunkle Tiefsee mit leuchtendem Plankton, Abendstimmung mit Sonne und Boot (weiche Überblendung).
- **Zum Antippen:** Blasen (platzen), kleine Fische (flitzen davon, auch beim Darüberwischen), Quallen (leuchten auf), Seesterne (drehen sich),
  Krabbe (winkt und versteckt sich), Muschel (öffnet sich und zeigt die Perle), Schildkröte (zieht den Kopf ein), Delfin (springt vorbei, Salto beim Antippen).
  Ein Tipp ins Leere macht Blasen und Glitzer.
- **Musik:** ruhige Meeresmusik (Akkordklänge, Wellenrauschen, sanfte Glocken, ferner Walgesang). Sie wird beim Start berechnet, es sind keine Audiodateien nötig,
  und sie wird leiser, solange der Hai spricht.
- **Menü** (Symbol oben rechts oder Zurück-Taste): Musik an/aus, Erzählen (Text und Stimme) an/aus, Geschichte neu starten, App beenden (zweimal tippen zur Sicherheit), Weiterspielen.
  Die Einstellungen bleiben gespeichert.
- Die Stimme kommt aus der Sprachausgabe des Handys (Deutsch). Der Untertitel steht klein oben im Bild.

## Installieren
`Hai-Freunde.apk` aufs Handy kopieren und öffnen (einmalig „Installation aus unbekannten Quellen“ für die Dateien-App erlauben).
Per USB: `adb install Hai-Freunde.apk`.

## Neu bauen
`bauen.bat` ausführen. Es nutzt JDK 17 und das Android SDK (Plattform 34, Build-Tools 34.0.0) unter `%LOCALAPPDATA%\AndroidBau`
und erzeugt `Hai-Freunde.apk` (mit Debug-Schlüssel signiert, nicht für den Play Store). Alle Dateien unter `src` werden kompiliert.

Der Quelltext ist UTF-8; `bauen.bat` übergibt deshalb `-encoding UTF-8` an den Compiler (ohne diese Angabe nimmt javac unter Windows die Standard-Zeichentabelle, und Umlaute und „ß“ landen als Zeichensalat im Text und in der Sprachausgabe).

## Aufbau (`src/de/hai/kind/`)
- `MainActivity.java` – Start, Sprachausgabe, Einstellungen, Beenden, Lebenszyklus
- `SeaView.java` – Zeichnen aller Szenen, Haiarten und Tiere, Antippen, Menü
- `Inhalt.java` – Haiarten, Szenen und Geschichten (hier Texte ergänzen)
- `Synth.java` – Klangerzeugung der Meeresmusik (reines Java)
- `HaiMusik.java` – spielt die Musik im Hintergrund ab (AudioTrack)
- `Blubb.java` – Blubber-Geräusche beim Antippen
- `AndroidManifest.xml`, `res/` – Manifest und App-Symbol
