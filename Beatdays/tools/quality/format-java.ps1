param([switch]$Check)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$toolDirectory=Join-Path $projectRoot '.tools'
$formatter=Join-Path $toolDirectory 'google-java-format-1.24.0-all-deps.jar'
$expectedHash='812f805f58112460edf01bf202a8e61d0fd1f35c0d4fabd54220640776ec57a1'
if(-not (Test-Path -LiteralPath $formatter)) {
    New-Item -ItemType Directory -Force -Path $toolDirectory | Out-Null
    Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/com/google/googlejavaformat/google-java-format/1.24.0/google-java-format-1.24.0-all-deps.jar' -OutFile $formatter
}
if((Get-FileHash -LiteralPath $formatter -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash) {throw 'Integridade do formatador divergente.'}
if($env:JAVA_HOME) {$java=Join-Path $env:JAVA_HOME 'bin\java.exe'} else {$java=(Get-Command java -ErrorAction Stop).Source}
$files=(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'app\src') -Recurse -File -Filter '*.java').FullName
$arguments=@('-jar',$formatter,'--aosp','--skip-reflowing-long-strings')
if($Check) {$arguments+=@('--dry-run','--set-exit-if-changed')} else {$arguments+='--replace'}
& $java @arguments @files
exit $LASTEXITCODE
