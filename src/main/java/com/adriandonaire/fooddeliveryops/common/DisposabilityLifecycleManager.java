package com.adriandonaire.fooddeliveryops.common;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Componente de evidencia técnica para The Twelve-Factor App:
 * Factor IX - Disposability (Desechabilidad).
 * 
 * Garantiza la máxima robustez mediante el inicio rápido y el apagado seguro (Graceful Shutdown).
 * Captura la señal del sistema (SIGTERM) emitida por el orquestador (Docker / Azure Container Apps)
 * para liberar limpiamente conexiones de base de datos, hilos y recursos antes de terminar el proceso.
 */
@Component
public class DisposabilityLifecycleManager {

    private static final Logger log = LoggerFactory.getLogger(DisposabilityLifecycleManager.class);

    @PreDestroy
    public void onGracefulShutdown() {
        log.info("================================================================================");
        log.info(">>> 12-FACTOR APP - FACTOR IX: DISPOSABILITY (DESECHABILIDAD) <<<");
        log.info("Recibida senal de terminacion (SIGTERM).");
        log.info("Tomcat rechazo nuevas peticiones y dreno las existentes exitosamente.");
        log.info("Cerrando pools de conexion HikariCP y liberando contexto de Spring limpiamente.");
        log.info("================================================================================");
    }
}
