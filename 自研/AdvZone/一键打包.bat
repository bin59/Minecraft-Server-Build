@echo off
chcp 65001 >nul
setlocal
title AdvZone Build

echo.
echo ============================================================
echo   AdvZone one-click build
echo   output: .\dist\AdvZone.jar   (copy it into plugins\ yourself)
echo ============================================================
echo.

rem ---- 0. check toolchain ----
where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] java not found in PATH. Install JDK 21 and re-run.
  pause
  exit /b 1
)
where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] maven not found in PATH. Install Maven, or build in IDEA.
  pause
  exit /b 1
)

set "PROJECT_DIR=%~dp0"
cd /d "%PROJECT_DIR%"
if not exist "pom.xml" (
  echo [ERROR] pom.xml not found in %PROJECT_DIR%
  pause
  exit /b 1
)

echo [1/3] maven: clean + package ...
call mvn -B -q clean package
if errorlevel 1 (
  echo [ERROR] build failed. Scroll up for the compile error.
  pause
  exit /b 1
)
if not exist "target\AdvZone.jar" (
  echo [ERROR] target\AdvZone.jar was not produced.
  pause
  exit /b 1
)
echo       OK - target\AdvZone.jar

echo [2/3] publishing to dist\ ...
if not exist "dist" mkdir "dist"
copy /Y "target\AdvZone.jar" "dist\AdvZone.jar" >nul
if errorlevel 1 (
  echo [ERROR] copy to dist failed.
  pause
  exit /b 1
)
echo       OK - dist\AdvZone.jar

echo [3/3] backing up current AdvZone config ...
set "STAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%"
set "STAMP=%STAMP: =0%"
set "SERVERDIR="
if exist "%PROJECT_DIR%..\server\plugins\AdvZone\config.yml" set "SERVERDIR=%PROJECT_DIR%..\server\"
if exist "%PROJECT_DIR%..\..\server\plugins\AdvZone\config.yml" set "SERVERDIR=%PROJECT_DIR%..\..\server\"
if exist "%SERVERDIR%plugins\AdvZone\config.yml" (
  if not exist "dist\config-backup\%STAMP%" mkdir "dist\config-backup\%STAMP%"
  copy /Y "%SERVERDIR%plugins\AdvZone\config.yml" "dist\config-backup\%STAMP%\" >nul
  echo       saved: dist\config-backup\%STAMP%\config.yml
) else (
  copy /Y "src\main\resources\config.yml" "dist\config.yml" >nul
  echo       no live config found, template copied to dist\config.yml
)

echo.
echo   Done. dist\AdvZone.jar is ready.
echo   Manual step: stop server, drop dist\AdvZone.jar into plugins\, start.
echo   In game: /az list   /az wand  (left click adds a point, right click undoes)
echo.
pause
endlocal
