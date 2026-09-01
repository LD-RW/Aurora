package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.constants.AppConstants;
import com.ecommerce.aurora.payload.OrderDTO;
import com.ecommerce.aurora.payload.OrderRequestDTO;
import com.ecommerce.aurora.payload.OrderResponse;
import com.ecommerce.aurora.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/order/users/payments/{paymentMethod}")
    public ResponseEntity<OrderDTO> placeOrder(@PathVariable String paymentMethod,
                                                 @Valid @RequestBody OrderRequestDTO orderRequestDTO) {
        OrderDTO orderDTO = orderService.placeOrder(paymentMethod, orderRequestDTO);
        return new ResponseEntity<>(orderDTO, HttpStatus.CREATED);
    }

    @GetMapping("/admin/orders")
    public ResponseEntity<OrderResponse> getAllOrders(
            @RequestParam(value = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.SORT_ORDERS_BY) String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder
    ) {
        OrderResponse orderResponse = orderService.getAllOrders(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
        OrderDTO orderDTO = orderService.getOrderById(orderId);
        return new ResponseEntity<>(orderDTO, HttpStatus.OK);
    }

    @GetMapping("/users/orders")
    public ResponseEntity<List<OrderDTO>> getCurrentUserOrders() {
        List<OrderDTO> orderDTOs = orderService.getCurrentUserOrders();
        return new ResponseEntity<>(orderDTOs, HttpStatus.OK);
    }
}
