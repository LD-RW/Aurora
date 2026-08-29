package com.ecommerce.aurora.mapper;

import com.ecommerce.aurora.model.Payment;
import com.ecommerce.aurora.payload.PaymentDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentDTO paymentToPaymentDTO(Payment payment);
}
