package com.famicup.repositorio;

import com.famicup.modelo.entidad.Pago;
import com.famicup.modelo.entidad.ApuestaColombia;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoPago;
import com.famicup.modelo.enumeracion.SistemaPago;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PagoRepository extends JpaRepository<Pago, UUID> {

    List<Pago> findByUserOrderByCreatedAtDesc(Usuario user);

    long countByStatus(EstadoPago status);

    Optional<Pago> findFirstByUserAndSystemOrderByCreatedAtDesc(Usuario user, SistemaPago system);

    Optional<Pago> findByColombiaBet(ApuestaColombia colombiaBet);

    @Query("select coalesce(sum(p.amountCop), 0) from Pago p where p.status = 'PAID'")
    BigDecimal totalPaid();

    @Query("select coalesce(sum(p.amountCop), 0) from Pago p where p.status = 'PAID' and p.system = ?1")
    BigDecimal totalPaidBySystem(SistemaPago system);
}
