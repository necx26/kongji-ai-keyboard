param(
    [string]$GradleHome = 'D:\Unity\6000.6.2f1\Editor\Data\PlaybackEngines\AndroidPlayer\Tools\gradle',
    [ValidateNotNullOrEmpty()]
    [string]$OutputDirectory = (Join-Path $PSScriptRoot 'release')
)
$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not (Test-Path -LiteralPath (Join-Path $GradleHome 'lib'))) {
    throw '请传入 Gradle 9.3.1 目录：.\build.ps1 -GradleHome D:\你的Gradle目录，或用 Android Studio 打开项目。'
}
& java -cp (Join-Path $GradleHome 'lib\*') org.gradle.launcher.GradleMain :app:assembleDebug :app:lintDebug --console=plain
if($LASTEXITCODE -ne 0){throw '编译或检查失败，请查看上方输出。'}
$taskAppVersion = [regex]::Match((Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'app\build.gradle')),"versionName\s+'([^']+)'").Groups[1].Value
if (-not $taskAppVersion) { throw '无法从 app/build.gradle 读取版本号' }
$taskOutputDirectory = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutputDirectory)
New-Item -ItemType Directory -Path $taskOutputDirectory -Force | Out-Null
$taskApkOutput = Join-Path $taskOutputDirectory "控机AI输入法-v$taskAppVersion.apk"
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk') -Destination $taskApkOutput -Force
Write-Output "安装包已生成：$taskApkOutput"
