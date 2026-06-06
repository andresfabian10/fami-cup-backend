package com.famicup.modelo.mapper;

import com.famicup.modelo.dto.PagoResponse;
import com.famicup.modelo.entidad.Pago;
import org.springframework.stereotype.Component;

@Component
public class PagoMapper {

    public PagoResponse toResponse(Pago payment) {
        return new PagoResponse(
                payment.getId(),
                payment.getUser().getId(),
                payment.getUser().getUsername(),
                payment.getUser().getFullName(),
                payment.getSystem(),
                payment.getMatch() == null ? null : payment.getMatch().getId(),
                payment.getColombiaBet() == null ? null : payment.getColombiaBet().getId(),
                payment.getAmountCop(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getReference(),
                payment.getPaidAt(),
                payment.getCreatedAt());
    }
}
