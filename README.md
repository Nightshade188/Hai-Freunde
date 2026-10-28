# APK – Hai-Freunde

Kleine Android-App für Kinder von 2 bis 3 Jahren: Ein freundlicher Hai erzählt etwas über Haie.
Bedienung nur durch Antippen, kein Lesen nötig, ohne Internet und ohne Berechtigungen.

- **Hai antippen:** nächste Geschichte (Bild, Bewegung und gesprochener Satz): Zähne, Schwanz, Nase, Größe (Walhai), Kiemen
- **Blasen antippen:** sie platzen. **Kleine Fische antippen:** sie flitzen davon.
- Die Stimme kommt aus der Sprachausgabe des Handys (Deutsch). Fehlt sie, steht der Text trotzdem groß im Bild.

## Installieren
`Hai-Freunde.apk` aufs Handy kopieren und öffnen (einmalig „Installation aus unbekannten Quellen“ für die Dateien-App erlauben).
Per USB: `adb install Hai-Freunde.apk`.

## Neu bauen
`bauen.bat` ausführen. Es nutzt JDK 17 und das Android SDK (Plattform 34, Build-Tools 34.0.0) unter `%LOCALAPPDATA%\AndroidBau`
und erzeugt `Hai-Freunde.apk` (mit Debug-Schlüssel signiert, nicht für den Play Store).

Der Quelltext ist UTF-8; `bauen.bat` übergibt deshalb `-encoding UTF-8` an den Compiler (ohne diese Angabe nimmt javac unter Windows die Standard-Zeichentabelle, und Umlaute und „ß“ landen als Zeichensalat im Text und in der Sprachausgabe). Der Untertitel unten im Bild ist bewusst klein (3,4 % der Bildhöhe), damit mehr Fläche zum Antippen bleibt.

## Aufbau
- `src/de/hai/kind/MainActivity.java` – die ganze App (Zeichnen, Animation, Sprachausgabe, Antippen)
- `AndroidManifest.xml`, `res/` – Manifest und App-Symbol
