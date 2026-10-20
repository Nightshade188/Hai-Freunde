@echo off
rem Baut Hai-Freunde.apk ohne Gradle. Werkzeuge: %LOCALAPPDATA%\AndroidBau (JDK 17 + Android SDK, Plattform 34, Build-Tools 34.0.0)
setlocal
set "TOOLS=%LOCALAPPDATA%\AndroidBau"
set "JAVA_HOME=%TOOLS%\jdk\jdk17.0.20_12"
set "SDK=%TOOLS%\sdk"
set "BT=%SDK%\build-tools\34.0.0"
set "AJAR=%SDK%\platforms\android-34\android.jar"
set "PATH=%JAVA_HOME%\bin;%BT%;%PATH%"
set "HIER=%~dp0"
set "BAU=%TOOLS%\bau-hai"
if exist "%BAU%" rmdir /s /q "%BAU%"
mkdir "%BAU%\klassen" || exit /b 1

call aapt2 compile --dir "%HIER%res" -o "%BAU%\res.zip" || exit /b 1
call aapt2 link -o "%BAU%\roh.apk" -I "%AJAR%" --manifest "%HIER%AndroidManifest.xml" --min-sdk-version 23 --target-sdk-version 34 "%BAU%\res.zip" || exit /b 1
javac --release 8 -cp "%AJAR%" -d "%BAU%\klassen" "%HIER%src\de\hai\kind\MainActivity.java" || exit /b 1
jar cf "%BAU%\klassen.jar" -C "%BAU%\klassen" . || exit /b 1
call d8 --lib "%AJAR%" --min-api 23 --output "%BAU%" "%BAU%\klassen.jar" || exit /b 1
rem classes.dex in das APK packen (cd, damit der Eintrag ohne Pfad heisst)
pushd "%BAU%"
jar uf roh.apk classes.dex || exit /b 1
popd
call zipalign -f -p 4 "%BAU%\roh.apk" "%BAU%\ausgerichtet.apk" || exit /b 1
if not exist "%TOOLS%\hai-debug.keystore" keytool -genkeypair -keystore "%TOOLS%\hai-debug.keystore" -alias hai -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Hai Freunde" || exit /b 1
call apksigner sign --ks "%TOOLS%\hai-debug.keystore" --ks-pass pass:android --key-pass pass:android --out "%HIER%Hai-Freunde.apk" "%BAU%\ausgerichtet.apk" || exit /b 1
call apksigner verify "%HIER%Hai-Freunde.apk" || exit /b 1
echo FERTIG: %HIER%Hai-Freunde.apk
