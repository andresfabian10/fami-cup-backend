package com.famicup.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "famicup.scheduler")
public record SchedulerProperties(
        boolean enabled,
        String fixturesCron,
        String resultsCron,
        int recentResultsHours) {
}
