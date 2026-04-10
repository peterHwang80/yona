param(
    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]]$Tasks = @("compile")
)

$ErrorActionPreference = "Stop"

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$BuildRoot = Join-Path $RepoRoot ".build"
$DownloadRoot = Join-Path $BuildRoot "downloads"
$ActivatorVersion = "1.2.12"
$SbtVersion = "0.13.5"
$SbtScalaVersion = "2.10.4"
$ActivatorZipName = "typesafe-activator-$ActivatorVersion-minimal.zip"
$ActivatorUrl = "https://downloads.typesafe.com/typesafe-activator/$ActivatorVersion/$ActivatorZipName"
$ActivatorHome = Join-Path $BuildRoot "activator-$ActivatorVersion-minimal"
$ActivatorJar = Join-Path $ActivatorHome "activator-launch-$ActivatorVersion.jar"
$RepositoryConfig = Join-Path $PSScriptRoot "sbt-repositories"

function Get-JavaExecutable {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path $candidate) {
            return $candidate
        }
        throw "JAVA_HOME is set, but java.exe was not found under '$candidate'."
    }

    $javaCommand = Get-Command java -ErrorAction Stop
    return $javaCommand.Source
}

function Assert-JavaVersion {
    param(
        [string]$JavaExecutable
    )

    $versionOutput = cmd.exe /c ('"' + $JavaExecutable + '" -version 2>&1')
    if (($versionOutput -join "`n") -notmatch 'version "1\.8') {
        Write-Warning "Yona build is known to work with Java 8. Current runtime: $($versionOutput[0])"
    }
}

function Ensure-Activator {
    if (Test-Path $ActivatorJar) {
        return
    }

    New-Item -ItemType Directory -Force -Path $BuildRoot | Out-Null
    New-Item -ItemType Directory -Force -Path $DownloadRoot | Out-Null

    $zipPath = Join-Path $DownloadRoot $ActivatorZipName
    if (-not (Test-Path $zipPath)) {
        Write-Host "Downloading Typesafe Activator $ActivatorVersion..."
        Invoke-WebRequest -Uri $ActivatorUrl -OutFile $zipPath
    }

    Write-Host "Extracting Typesafe Activator $ActivatorVersion..."
    Expand-Archive -Path $zipPath -DestinationPath $BuildRoot -Force
}

function Convert-ToActivatorHome {
    param(
        [string]$Path
    )

    return "//" + ($Path -replace "\\", "/")
}

$javaExe = Get-JavaExecutable
Assert-JavaVersion -JavaExecutable $javaExe
Ensure-Activator

$javaArgs = @(
    "-Xms512m",
    "-Xmx2048m",
    "-Dfile.encoding=UTF-8",
    "-Dsbt.version=$SbtVersion",
    "-Dsbt.scala.version=$SbtScalaVersion",
    "-Dsbt.override.build.repos=true",
    "-Dsbt.repository.config=$RepositoryConfig",
    "-Dsbt.global.base=$(Join-Path $BuildRoot 'sbt-global')",
    "-Dsbt.boot.directory=$(Join-Path $BuildRoot 'sbt-boot')",
    "-Dactivator.home=$(Convert-ToActivatorHome -Path $ActivatorHome)"
)

$javaArgs += @(
    "-jar",
    $ActivatorJar
) + $Tasks

Push-Location $RepoRoot
try {
    & $javaExe @javaArgs
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
