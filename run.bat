@echo off
rem Downloads the optional libraries (FlatLaf look, MP3 support) once,
rem then builds and starts the player.
cd /d "%~dp0"
if not exist lib mkdir lib
call :get com/formdev/flatlaf/3.6 flatlaf-3.6.jar
call :get com/googlecode/soundlibs/mp3spi/1.9.5.4 mp3spi-1.9.5.4.jar
call :get com/googlecode/soundlibs/jlayer/1.0.1.4 jlayer-1.0.1.4.jar
call :get com/googlecode/soundlibs/tritonus-share/0.3.7.4 tritonus-share-0.3.7.4.jar
javac -d bin -cp "lib\*" -sourcepath src src\MusicPlayerUI.java || (pause & exit /b 1)
start "" javaw -cp "bin;lib\*" MusicPlayerUI %*
exit /b

:get
if not exist "lib\%2" (
  echo Downloading %2...
  curl.exe -fsSL -o "lib\%2" "https://repo1.maven.org/maven2/%1/%2" || del "lib\%2" 2>nul
)
exit /b
