package com.ecommerce.aurora.mapper;

import com.ecommerce.aurora.model.Order;
import com.ecommerce.aurora.payload.OrderDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class, PaymentMapper.class})
public interface OrderMapper {

    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "address.addressId", target = "addressId")
    OrderDTO orderToOrderDTO(Order order);
}
