@echo off
chcp 65001 >nul
cd /d %~dp0

echo ========================================
echo   PuzzleGame EXE Build
echo ========================================

echo [1/3] javac...
javac *.java
if errorlevel 1 ( echo BUILD FAILED: javac & pause & exit /b 1 )

echo [2/3] jar...
jar cfe PuzzleGame.jar LoginFrame *.class assets

if not exist dist mkdir dist

echo [3/3] launch4j...
set L4J=%~dp0tools\launch4j
java --add-opens java.base/java.lang=ALL-UNNAMED ^
     --add-opens java.base/java.util=ALL-UNNAMED ^
     --add-opens java.base/java.lang.reflect=ALL-UNNAMED ^
     --add-opens java.desktop/java.awt.font=ALL-UNNAMED ^
     --add-opens jdk.unsupported/sun.misc=ALL-UNNAMED ^
     -cp "%L4J%\launch4j.jar;%L4J%\lib\*" ^
     net.sf.launch4j.Main launch4j-config.xml
if errorlevel 1 ( echo BUILD FAILED: launch4j & pause & exit /b 1 )

echo.
echo OK -^> dist\PuzzleGame.exe
pause
