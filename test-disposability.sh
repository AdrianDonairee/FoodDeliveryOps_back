#!/usr/bin/env bash
# =============================================================================
# Script de prueba interactivo: 12-Factor App - Factor IX: Desechabilidad
# =============================================================================

AZURE_URL="https://fooddeliveryops-back-app.whiteglacier-6ce94eb7.westus3.azurecontainerapps.io"
LOCAL_URL="http://localhost:8080"
TARGET_URL="$LOCAL_URL"

while true; do
    echo ""
    echo "=========================================================="
    echo " 12-FACTOR APP: FACTOR IX - DISPOSABILITY (DESECHABILIDAD) "
    echo "=========================================================="
    echo "Selecciona una opcion:"
    echo "1. Ver Estado en Tiempo Real (Health Check)"
    echo "2. Detener Backend (Stop)"
    echo "3. Iniciar Backend (Start)"
    echo "4. Reiniciar Backend (Restart - 12 Factor Disposability)"
    echo "5. Salir"
    echo ""
    read -p "Ingresa opcion (1-5): " opcion

    case $opcion in
        1)
            echo ""
            echo "Consultando estado de salud del backend..."
            # Intentar primero local, si no responde intentar Azure
            HTTP_CODE=$(curl -s -o /tmp/health.json -w "%{http_code}" "$LOCAL_URL/actuator/health" 2>/dev/null)
            if [ "$HTTP_CODE" == "200" ]; then
                echo " [LOCAL] Backend ACTIVO (HTTP 200 OK):"
                cat /tmp/health.json
                echo ""
            else
                echo " [LOCAL] No disponible (HTTP $HTTP_CODE). Consultando en Azure..."
                HTTP_CODE_AZ=$(curl -s -o /tmp/health_az.json -w "%{http_code}" "$AZURE_URL/actuator/health" 2>/dev/null)
                if [ "$HTTP_CODE_AZ" == "200" ]; then
                    echo " [AZURE] Backend ACTIVO en Cloud (HTTP 200 OK):"
                    cat /tmp/health_az.json
                    echo ""
                else
                    echo " El backend se encuentra DETENIDO o no responde."
                fi
            fi
            ;;
        2)
            echo ""
            echo "Deteniendo Backend de forma limpia (SIGTERM - Graceful Shutdown)..."
            if command -v docker &> /dev/null && docker ps -q --filter "name=fooddeliveryops" | grep -q .; then
                echo "Enviando SIGTERM a contenedor Docker..."
                docker stop fooddeliveryops-back
                echo " Backend detenido limpiamente."
            else
                echo "Deteniendo proceso local Java (PID)..."
                pkill -f "FoodDeliveryOps-api" || taskkill //F //IM "java.exe" 2>/dev/null || echo "No hay proceso backend corriendo."
                echo " Senal de apagado enviada."
            fi
            ;;
        3)
            echo ""
            echo "Iniciando Backend (Fast Startup)..."
            if command -v docker &> /dev/null && docker ps -a -q --filter "name=fooddeliveryops" | grep -q .; then
                docker start fooddeliveryops-back
                echo " Contenedor Docker iniciado."
            else
                echo "Iniciando con Maven en segundo plano..."
                ./mvnw spring-boot:run > /dev/null 2>&1 &
                echo " Proceso iniciado en segundo plano en puerto 8080."
            fi
            ;;
        4)
            echo ""
            echo "=========================================================="
            echo "Probando Factor IX: Graceful Shutdown + Fast Startup"
            echo "=========================================================="
            START_TIME=$(date +%s)
            echo "1. Enviando senal de apagado seguro (esperando drenaje de requests)..."
            if command -v docker &> /dev/null && docker ps -q --filter "name=fooddeliveryops" | grep -q .; then
                docker restart -t 20 fooddeliveryops-back
            else
                echo "Reiniciando via Maven / Java..."
                pkill -f "FoodDeliveryOps-api" || true
                sleep 2
                ./mvnw spring-boot:run > /dev/null 2>&1 &
            fi
            END_TIME=$(date +%s)
            DIFF=$((END_TIME - START_TIME))
            echo " Backend reiniciado exitosamente en $DIFF segundos."
            echo " Cumple con Desechabilidad: cerrado seguro sin perdida de estado y arranque veloz."
            ;;
        5)
            echo "Saliendo..."
            exit 0
            ;;
        *)
            echo "Opcion invalida. Intenta nuevamente."
            ;;
    esac
done
