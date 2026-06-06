package com.famicup.repositorio;

import com.famicup.modelo.entidad.ApiSyncLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiSyncLogRepository extends JpaRepository<ApiSyncLog, UUID> {

    List<ApiSyncLog> findTop5ByOrderByStartedAtDesc();
}
