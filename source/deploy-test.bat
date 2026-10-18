@echo off
setlocal
cd /d "%~dp0"

set "NO_START=false"
set "ONLINE=false"

:parse_args
if "%~1"=="" goto args_done
if /I "%~1"=="--no-start" (
	set "NO_START=true"
) else if /I "%~1"=="--online" (
	set "ONLINE=true"
) else (
	echo Usage: deploy-test.bat [--online] [--no-start]
	exit /b 1
)
shift
goto parse_args

:args_done
set "PACK_HOST=127.0.0.1"
if /I "%ONLINE%"=="true" (
	set "PACK_HOST="
	for /f "usebackq delims=" %%I in (`powershell -NoProfile -Command "$ip = (Invoke-RestMethod -Uri 'https://api.ipify.org').Trim(); $parsed = [Net.IPAddress]::Parse($ip); if ($parsed.AddressFamily -ne [Net.Sockets.AddressFamily]::InterNetwork) { throw 'A public IPv4 address is required.' }; $ip"`) do set "PACK_HOST=%%I"
	if not defined PACK_HOST (
		echo Could not determine the public IPv4 address for --online mode.
		exit /b 1
	)
)

set "JAVA_DIR="
if defined TASKTUSSLE_JAVA_HOME set "JAVA_DIR=%TASKTUSSLE_JAVA_HOME%"
if not defined JAVA_DIR for /d %%D in ("%ProgramFiles%\Java\jdk-25*") do if exist "%%~fD\bin\java.exe" set "JAVA_DIR=%%~fD"
if not defined JAVA_DIR (
	echo Java 25 was not found. Set TASKTUSSLE_JAVA_HOME to its install folder.
	exit /b 1
)
set "JAVA_HOME=%JAVA_DIR%"
set "PATH=%JAVA_DIR%\bin;%PATH%"

if not exist "..\test-server" (
	if not exist "..\server" (
		echo Could not create test-server because ..\server does not exist.
		exit /b 1
	)
	echo Creating test-server from server...
	powershell -NoProfile -Command "Copy-Item -LiteralPath '..\server' -Destination '..\test-server' -Recurse"
	if errorlevel 1 exit /b 1
)

call gradlew.bat test build
if errorlevel 1 exit /b 1

powershell -NoProfile -Command "$root = (Resolve-Path '..\test-server').Path; $running = Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -and $_.CommandLine.Contains($root) }; if ($running) { exit 1 }"
if errorlevel 1 (
	echo The test server is already running. Stop it cleanly, then deploy again.
	exit /b 1
)

if not exist "..\test-server\mods" mkdir "..\test-server\mods"
copy /Y "build\libs\tasktussle-2.0.0.jar" "..\test-server\mods\tasktussle-2.0.0.jar"
if errorlevel 1 exit /b 1

if exist "..\test-server\tasktussle-resource-pack.zip" del /Q "..\test-server\tasktussle-resource-pack.zip"
jar --create --file "..\test-server\tasktussle-resource-pack.zip" --no-manifest -C "..\resourcepack" .
if errorlevel 1 exit /b 1

set "TASKTUSSLE_PACK_HOST=%PACK_HOST%"
set "TASKTUSSLE_DEPLOY_ONLINE=%ONLINE%"
powershell -NoProfile -Command "$dir = '..\test-server\config\tasktussle'; $path = Join-Path $dir 'tasktussle.json'; New-Item -ItemType Directory -Force $dir | Out-Null; $enabled = $true; if (Test-Path $path) { try { $old = Get-Content -Raw $path | ConvertFrom-Json; if ($null -ne $old.resource_pack_enabled) { $enabled = [bool]$old.resource_pack_enabled } } catch {} }; $config = [ordered]@{ resource_pack_enabled = $enabled; resource_pack_url = 'http://' + $env:TASKTUSSLE_PACK_HOST + ':25566/tasktussle-resource-pack.zip' }; [IO.File]::WriteAllText($path, ($config | ConvertTo-Json), [Text.UTF8Encoding]::new($false))"
if errorlevel 1 exit /b 1

powershell -NoProfile -Command "$online = $env:TASKTUSSLE_DEPLOY_ONLINE -eq 'true'; $path = '..\test-server\server.properties'; $updates = [ordered]@{ 'server-ip' = $(if ($online) { '' } else { '127.0.0.1' }); 'server-port' = '25565'; 'online-mode' = $online.ToString().ToLowerInvariant(); 'white-list' = $online.ToString().ToLowerInvariant(); 'enforce-whitelist' = $online.ToString().ToLowerInvariant(); 'spawn-protection' = '0'; 'resource-pack' = ''; 'resource-pack-sha1' = ''; 'require-resource-pack' = 'false' }; $lines = [Collections.Generic.List[string]](Get-Content -LiteralPath $path); foreach ($entry in $updates.GetEnumerator()) { $index = -1; for ($i = 0; $i -lt $lines.Count; $i++) { if ($lines[$i] -match ('^' + [regex]::Escape($entry.Key) + '=')) { $index = $i; break } }; $line = $entry.Key + '=' + $entry.Value; if ($index -ge 0) { $lines[$index] = $line } else { $lines.Add($line) } }; [IO.File]::WriteAllLines($path, $lines, [Text.UTF8Encoding]::new($false))"
if errorlevel 1 exit /b 1

echo Deployed TaskTussle to test-server on Minecraft port 25565.
echo Resource pack: http://%PACK_HOST%:25566/tasktussle-resource-pack.zip
if /I "%ONLINE%"=="true" (echo Online mode: %PACK_HOST%:25565.) else (echo Local-only mode: 127.0.0.1:25565.)
if /I "%NO_START%"=="true" exit /b 0

pushd "..\test-server"
call start.bat
popd
