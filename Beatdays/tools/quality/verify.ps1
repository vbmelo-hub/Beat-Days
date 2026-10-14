param([switch]$Offline)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$cache=Join-Path $projectRoot '.tools\gradle-home'
if($cache -match '[^\x00-\x7F]') {throw 'Use um caminho sem acentos para o projeto e seu cache Gradle no Windows.'}
if(-not (Test-Path -LiteralPath (Join-Path $cache 'caches\modules-2'))) {
    $previousCache=if($env:GRADLE_USER_HOME){$env:GRADLE_USER_HOME}else{Join-Path $env:USERPROFILE '.gradle'}
    New-Item -ItemType Directory -Force -Path (Join-Path $cache 'caches') | Out-Null
    if(Test-Path -LiteralPath (Join-Path $previousCache 'caches\modules-2')) {Copy-Item -LiteralPath (Join-Path $previousCache 'caches\modules-2') -Destination (Join-Path $cache 'caches\modules-2') -Recurse -Force}
    if(Test-Path -LiteralPath (Join-Path $previousCache 'wrapper')) {Copy-Item -LiteralPath (Join-Path $previousCache 'wrapper') -Destination (Join-Path $cache 'wrapper') -Recurse -Force}
}
$temporaryDirectory=Join-Path $projectRoot '.tools\tmp'
New-Item -ItemType Directory -Force -Path $temporaryDirectory | Out-Null
$previousOptions=$env:JAVA_TOOL_OPTIONS
try {
    $env:JAVA_TOOL_OPTIONS=($previousOptions+' -Djdk.net.unixdomain.tmpdir='+$temporaryDirectory).Trim()
    $arguments=@('-g',$cache,'-p',$projectRoot,'--no-configuration-cache','--no-daemon','testDebugUnitTest','assembleDebug','assembleDebugAndroidTest','lintDebug')
    if($Offline) {$arguments+='--offline'}
    & (Join-Path $projectRoot 'gradlew.bat') @arguments
    if($LASTEXITCODE -ne 0) {throw 'Falha nas verificações Gradle.'}
} finally {$env:JAVA_TOOL_OPTIONS=$previousOptions}
