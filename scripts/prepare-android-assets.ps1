param(
  [string]$Root = (Split-Path -Parent $PSScriptRoot)
)

$server = Join-Path $Root 'server'
$assets = Join-Path $Root 'app/src/main/assets/nodejs-project'
$stage = Join-Path ([System.IO.Path]::GetTempPath()) ("logvar-android-deps-" + [guid]::NewGuid())

if (!(Test-Path (Join-Path $server 'package-lock.json'))) {
  throw "Missing server/package-lock.json"
}

New-Item -ItemType Directory -Path $stage -Force | Out-Null
try {
  Copy-Item (Join-Path $server 'package.json') $stage
  Copy-Item (Join-Path $server 'package-lock.json') $stage
  npm ci --omit=dev --prefix $stage

  $runtimeDeps = Join-Path $assets 'node_modules'
  if (Test-Path $runtimeDeps) {
    Remove-Item -LiteralPath $runtimeDeps -Recurse -Force
  }
  Copy-Item (Join-Path $stage 'node_modules') $runtimeDeps -Recurse
  Write-Host "Android runtime dependencies prepared in $runtimeDeps"
}
finally {
  if (Test-Path $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
}
