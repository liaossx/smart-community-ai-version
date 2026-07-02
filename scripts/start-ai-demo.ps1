param(
    [switch]$AiOnly,
    [switch]$AllServices,
    [switch]$SkipInfra,
    [switch]$SkipBuild,
    [string]$Java8Home = "C:\Program Files\Java\jdk1.8.0_202",
    [string]$Java17Home = "C:\Program Files\Java\jdk-17",
    [string]$NacosServer = "localhost:8848",
    [string]$MysqlHost = "localhost",
    [string]$MysqlPort = "3306",
    [string]$MysqlDatabase = "smart_community",
    [string]$MysqlUsername = "root",
    [string]$MysqlPassword = "",
    [string]$RedisHost = "localhost",
    [string]$RedisPassword = "",
    [string]$RabbitHost = "localhost"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

function Assert-Directory {
    param(
        [string]$Path,
        [string]$Name
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
        throw "$Name not found: $Path"
    }
}

function Invoke-WithJava {
    param(
        [string]$JavaHome,
        [string]$Command
    )

    Assert-Directory -Path $JavaHome -Name "Java home"
    $previousJavaHome = $env:JAVA_HOME
    $previousPath = $env:Path
    try {
        $env:JAVA_HOME = $JavaHome
        $env:Path = "$JavaHome\bin;$previousPath"
        Invoke-Expression $Command
    } finally {
        $env:JAVA_HOME = $previousJavaHome
        $env:Path = $previousPath
    }
}

function Start-Infra {
    $composeArgs = @("compose", "-f", "docker-compose.infra.yml", "up", "-d")

    if (Get-Command docker -ErrorAction SilentlyContinue) {
        Write-Host "Starting infra with docker compose..." -ForegroundColor Cyan
        Push-Location $Root
        try {
            & docker @composeArgs
        } finally {
            Pop-Location
        }
        return
    }

    if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
        Write-Host "Starting infra with docker-compose..." -ForegroundColor Cyan
        Push-Location $Root
        try {
            & docker-compose -f docker-compose.infra.yml up -d
        } finally {
            Pop-Location
        }
        return
    }

    Write-Warning "Docker command not found. Start MySQL, Redis, Nacos, and RabbitMQ manually, or run again with Docker installed."
}

function Install-CommonArtifacts {
    Write-Host "Installing parent pom and common-module with Java 8..." -ForegroundColor Cyan
    Push-Location $Root
    try {
        Invoke-WithJava -JavaHome $Java8Home -Command "mvn -q -N install -DskipTests"
        Invoke-WithJava -JavaHome $Java8Home -Command "mvn -q -pl common-module install -DskipTests"
    } finally {
        Pop-Location
    }
}

function Start-ServiceWindow {
    param(
        [string]$Title,
        [string]$JavaHome,
        [string]$MavenFile,
        [string[]]$AppArgs
    )

    Assert-Directory -Path $JavaHome -Name "$Title Java home"
    $pomPath = Join-Path $Root $MavenFile
    if (-not (Test-Path -LiteralPath $pomPath -PathType Leaf)) {
        throw "$Title pom not found: $pomPath"
    }

    $joinedArgs = ($AppArgs | ForEach-Object { '"' + ($_ -replace '"', '\"') + '"' }) -join ","
    $command = @"
`$Host.UI.RawUI.WindowTitle = "$Title"
Set-Location "$Root"
`$env:JAVA_HOME = "$JavaHome"
`$env:Path = "$JavaHome\bin;`$env:Path"
`$env:MYSQL_HOST = "$MysqlHost"
`$env:MYSQL_PORT = "$MysqlPort"
`$env:MYSQL_DATABASE = "$MysqlDatabase"
`$env:MYSQL_USERNAME = "$MysqlUsername"
`$env:MYSQL_PASSWORD = "$MysqlPassword"
`$env:REDIS_HOST = "$RedisHost"
`$env:REDIS_PASSWORD = "$RedisPassword"
`$env:RABBITMQ_HOST = "$RabbitHost"
`$env:AI_CUSTOMER_KNOWLEDGE_STORE = "jdbc"
`$env:AI_EMBEDDING_PROVIDER = "hash"
`$appArgs = @($joinedArgs)
mvn -f "$pomPath" spring-boot:run "-Dspring-boot.run.arguments=`$(`$appArgs -join ' ')"
"@

    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy",
        "Bypass",
        "-Command",
        $command
    )

    Write-Host "Started window: $Title" -ForegroundColor Green
}

function Get-LegacyAppArgs {
    return @(
        "--spring.cloud.nacos.discovery.server-addr=$NacosServer",
        "--spring.cloud.nacos.config.server-addr=$NacosServer",
        "--MYSQL_HOST=$MysqlHost",
        "--MYSQL_PORT=$MysqlPort",
        "--MYSQL_DATABASE=$MysqlDatabase",
        "--MYSQL_USERNAME=$MysqlUsername",
        "--MYSQL_PASSWORD=$MysqlPassword",
        "--REDIS_HOST=$RedisHost",
        "--REDIS_PASSWORD=$RedisPassword",
        "--RABBITMQ_HOST=$RabbitHost"
    )
}

function Get-AiAppArgs {
    return @(
        "--MYSQL_HOST=$MysqlHost",
        "--MYSQL_PORT=$MysqlPort",
        "--MYSQL_DATABASE=$MysqlDatabase",
        "--MYSQL_USERNAME=$MysqlUsername",
        "--MYSQL_PASSWORD=$MysqlPassword",
        "--AI_CUSTOMER_KNOWLEDGE_STORE=jdbc",
        "--AI_EMBEDDING_PROVIDER=hash"
    )
}

Write-Host "Smart community AI demo startup" -ForegroundColor Cyan
Write-Host "Root: $Root"

if (-not $SkipInfra) {
    Start-Infra
}

if (-not $SkipBuild -and -not $AiOnly) {
    Install-CommonArtifacts
}

if ($AiOnly) {
    Start-ServiceWindow `
        -Title "ai-service :8090" `
        -JavaHome $Java17Home `
        -MavenFile "ai-service\pom.xml" `
        -AppArgs (Get-AiAppArgs)
} else {
    $legacyServices = @(
        @{ Title = "user-service :8082"; Module = "user-service\pom.xml" },
        @{ Title = "property-service :8085"; Module = "property-service\pom.xml" },
        @{ Title = "workorder-service :8088"; Module = "workorder-service\pom.xml" }
    )

    if ($AllServices) {
        $legacyServices = @(
            @{ Title = "system-service"; Module = "system-service\pom.xml" },
            @{ Title = "user-service :8082"; Module = "user-service\pom.xml" },
            @{ Title = "house-service"; Module = "house-service\pom.xml" },
            @{ Title = "parking-service"; Module = "parking-service\pom.xml" },
            @{ Title = "property-service :8085"; Module = "property-service\pom.xml" },
            @{ Title = "workorder-service :8088"; Module = "workorder-service\pom.xml" },
            @{ Title = "community-service"; Module = "community-service\pom.xml" }
        )
    }

    foreach ($service in $legacyServices) {
        Start-ServiceWindow `
            -Title $service.Title `
            -JavaHome $Java8Home `
            -MavenFile $service.Module `
            -AppArgs (Get-LegacyAppArgs)
        Start-Sleep -Seconds 2
    }

    Start-ServiceWindow `
        -Title "gateway-service :80" `
        -JavaHome $Java8Home `
        -MavenFile "gateway-service\pom.xml" `
        -AppArgs (Get-LegacyAppArgs)

    Start-Sleep -Seconds 2

    Start-ServiceWindow `
        -Title "ai-service :8090" `
        -JavaHome $Java17Home `
        -MavenFile "ai-service\pom.xml" `
        -AppArgs (Get-AiAppArgs)
}

Write-Host ""
Write-Host "Startup commands were issued. Wait until each window prints Started before testing." -ForegroundColor Yellow
Write-Host "Gateway:    http://localhost:80"
Write-Host "AI service: http://localhost:8090"
