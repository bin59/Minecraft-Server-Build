@echo off
chcp 65001 >nul
cd /d "%~dp0"

where python >nul 2>nul
if %errorlevel%==0 (
    python "打包资源包.py"
    goto :end
)

where py >nul 2>nul
if %errorlevel%==0 (
    py "打包资源包.py"
    goto :end
)

echo.
echo [错误] 本机找不到 python。
echo 请安装 Python 3 并勾选 "Add python.exe to PATH"，或手动执行：
echo     python 打包资源包.py
echo.
pause

:end
pause
