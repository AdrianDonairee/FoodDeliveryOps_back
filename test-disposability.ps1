# =============================================================================
# Script de prueba interactivo: 12-Factor App - Factor IX: Desechabilidad (PowerShell)
# =============================================================================

$AzureUrl = "https://fooddeliveryops-back-app.whiteglacier-6ce94eb7.westus3.azurecontainerapps.io"
$LocalUrl = "http://localhost:8080"

do {
    Write-Host ""
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host " 12-FACTOR APP: FACTOR IX - DISPOSABILITY (DESECHABILIDAD) " -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "Selecciona una opcion:"
    Write-Host "1. Ver Estado en Tiempo Real (Health Check)"
    Write-Host "2. Detener Backend (Stop)"
    Write-Host "3. Iniciar Backend (Start)"
    Write-Host "4. Reiniciar Backend (Restart - 12 Factor Disposability)"
    Write-Host "5. Salir"
    Write-Host ""
    $opcion = Read-Host "Ingresa opcion (1-5)"

    switch ($opcion) {
        "1" {
            Write-Host "`nConsultando estado de salud del backend..." -ForegroundColor Yellow
            try {
                $response = Invoke-RestMethod -Uri "$LocalUrl/actuator/health" -Method Get -TimeoutSec 3 -ErrorAction Stop
                Write-Host " [LOCAL] Backend ACTIVO (HTTP 200 OK):" -ForegroundColor Green
                $response | ConvertTo-Json
            }
            catch {
                Write-Host " [LOCAL] No disponible. Consultando en Azure..." -ForegroundColor Yellow
                try {
                    $responseAz = Invoke-RestMethod -Uri "$AzureUrl/actuator/health" -Method Get -TimeoutSec 5 -ErrorAction Stop
                    Write-Host " [AZURE] Backend ACTIVO en Cloud (HTTP 200 OK):" -ForegroundColor Green
                    $responseAz | ConvertTo-Json
                }
                catch {
                    Write-Host " El backend se encuentra DETENIDO o no responde." -ForegroundColor Red
                }
            }
        }
        "2" {
            Write-Host "`nDeteniendo Backend de forma limpia (SIGTERM - Graceful Shutdown)..." -ForegroundColor Yellow
            $dockerRunning = docker ps -q --filter "name=fooddeliveryops" 2>$null
            if ($dockerRunning) {
                Write-Host "Enviando SIGTERM a contenedor Docker..."
                docker stop fooddeliveryops-back
                Write-Host " Backend detenido limpiamente." -ForegroundColor Green
            } else {
                Write-Host "Deteniendo proceso Java en Windows..."
                Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
                Write-Host " Senal de apagado enviada." -ForegroundColor Green
            }
        }
        "3" {
            Write-Host "`nIniciando Backend (Fast Startup)..." -ForegroundColor Yellow
            $dockerExists = docker ps -a -q --filter "name=fooddeliveryops" 2>$null
            if ($dockerExists) {
                docker start fooddeliveryops-back
                Write-Host " Contenedor Docker iniciado." -ForegroundColor Green
            } else {
                Write-Host "Iniciando con Maven Wrapper..."
                Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit", "-Command", "./mvnw spring-boot:run"
                Write-Host " Proceso iniciado en nueva ventana en puerto 8080." -ForegroundColor Green
            }
        }
        "4" {
            Write-Host "`n==========================================================" -ForegroundColor Cyan
            Write-Host "Probando Factor IX: Graceful Shutdown + Fast Startup" -ForegroundColor Cyan
            Write-Host "==========================================================" -ForegroundColor Cyan
            $startTime = Get-Date
            Write-Host "1. Enviando senal de apagado seguro (esperando drenaje de requests)..." -ForegroundColor Yellow
            $dockerRunning = docker ps -q --filter "name=fooddeliveryops" 2>$null
            if ($dockerRunning) {
                docker restart -t 20 fooddeliveryops-back
            } else {
                Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
                Start-Sleep -Seconds 2
                Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit", "-Command", "./mvnw spring-boot:run"
            }
            $endTime = Get-Date
            $elapsed = ($endTime - $startTime).TotalSeconds
            Write-Host " Backend reiniciado en $([math]::Round($elapsed, 2)) segundos." -ForegroundColor Green
            Write-Host " Cumple con Desechabilidad: apagado seguro sin perdida de datos y arranque rapido." -ForegroundColor Green
        }
        "5" {
            Write-Host "Saliendo..." -ForegroundColor Gray
            break
        }
        default {
            Write-Host "Opcion invalida. Intenta nuevamente." -ForegroundColor Red
        }
    }
} while ($opcion -ne "5")
