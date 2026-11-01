# Klarscan – Android

Dokumentenscanner ohne Abo, ohne Konto und ohne Internet-Berechtigung.

## Was die App kann

- **Scannen** mit Kamera oder aus der Galerie. Ränder werden automatisch erkannt und die Seite wird begradigt. Filter und Schattenentfernung sind dabei (Google ML Kit, läuft auf dem Gerät).
- **Mehrseitige Dokumente**: Seiten hinzufügen, umsortieren, löschen.
- **PDF** im A4-Format teilen (Mail, WhatsApp, Drive …) oder irgendwo auf dem Handy speichern.
- **Text erkennen** offline für Deutsch, Französisch, Italienisch, Englisch und andere lateinische Schriften. Den Text kannst du kopieren.
- **Suche** nach Dokumentnamen, Umbenennen.
- **App-Sperre** per Fingerabdruck (Schloss oben rechts). Klappt der Finger nicht, geht die PIN des Handys. Die App sperrt sich, sobald du sie verlässt.

## Datenschutz

- Die INTERNET-Berechtigung ist im Manifest ausdrücklich entfernt. Die App kann nichts hochladen.
- Dokumente liegen nur im privaten App-Speicher. Das Cloud-Backup von Android ist abgeschaltet.
- Bei eingeschalteter Sperre zeigt die App-Übersicht keine Vorschau, und Bildschirmfotos sind blockiert.
- Kein Konto, kein Tracking, keine Werbung, kein Abo.

Voraussetzung: Android 8 oder neuer mit Google Play-Diensten.

## APK bekommen

### Weg A: GitHub (ohne Installation, im Browser)

1. Auf github.com ein Konto anlegen und ein neues Repository erstellen.
2. **Add file → Upload files** und den ganzen Inhalt dieses Ordners hochladen (auch den versteckten Ordner `.github`).
3. Im Tab **Actions** läuft „APK bauen“ automatisch los (ca. 5–8 Minuten).
4. Den fertigen Lauf öffnen, unten unter **Artifacts** „Klarscan-APK“ herunterladen und entpacken.
5. `app-debug.apk` aufs Handy bringen und öffnen. Android fragt einmal, ob Installationen aus dieser Quelle erlaubt sind.

### Weg B: Android Studio

1. Android Studio installieren, **Open** wählen und diesen Ordner öffnen.
2. Warten, bis Gradle synchronisiert ist.
3. Handy per USB anschliessen (Entwickleroptionen → USB-Debugging) und auf **Run ▶** tippen.

## Projektaufbau

```
app/src/main/java/ch/klarscan/
  MainActivity.kt    Bibliothek, Dokumentansicht, Dialoge (Jetpack Compose)
  DocumentStore.kt   Speichern, Seiten verwalten, PDF erzeugen
  AppLock.kt         App-Sperre (Fingerabdruck, Handy-PIN als Ausweg)
  Ocr.kt             Texterkennung (ML Kit, offline)
  Theme.kt           Farben hell und dunkel
```

## Für den Play Store

Für eine Veröffentlichung braucht es noch einen eigenen Signaturschlüssel (Android Studio: Build → Generate Signed App Bundle), eine Datenschutzerklärung und ein Konto in der Google Play Console (einmalig 25 $).
