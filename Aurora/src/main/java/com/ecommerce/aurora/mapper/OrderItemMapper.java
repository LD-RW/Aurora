package com.ecommerce.aurora.mapper;

import com.ecommerce.aurora.model.OrderItem;
import com.ecommerce.aurora.payload.OrderItemDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ProductMapper.class)
public interface OrderItemMapper {
    OrderItemDTO orderItemToOrderItemDTO(OrderItem orderItem);
}
