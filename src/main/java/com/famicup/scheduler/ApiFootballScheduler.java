package com.famicup.scheduler;

import com.famicup.configuracion.SchedulerProperties;
import com.famicup.servicio.ApiFootballSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ApiFootballScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiFootballScheduler.class);

    private final SchedulerProperties schedulerProperties;
    private final ApiFootballSyncService syncService;

    public ApiFootballScheduler(SchedulerProperties schedulerProperties, ApiFootballSyncService syncService) {
        this.schedulerProperties = schedulerProperties;
        this.syncService = syncService;
    }

    @Scheduled(cron = "${famicup.scheduler.fixtures-cron}")
    void syncFixtures() {
        if (!schedulerProperties.enabled()) {
            return;
        }
        LOGGER.info("Iniciando sincronizacion programada de fixtures.");
        syncService.syncFixtures();
    }

    @Scheduled(cron = "${famicup.scheduler.results-cron}")
    void syncResults() {
        if (!schedulerProperties.enabled()) {
            return;
        }
        LOGGER.info("Iniciando sincronizacion programada de resultados.");
        syncService.syncResults();
    }
}
