package com.ecommerce.aurora.service;

import com.ecommerce.aurora.constants.AppConstants;
import com.ecommerce.aurora.exceptions.APIException;
import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import com.ecommerce.aurora.mapper.OrderMapper;
import com.ecommerce.aurora.model.Address;
import com.ecommerce.aurora.model.Cart;
import com.ecommerce.aurora.model.CartItem;
import com.ecommerce.aurora.model.Order;
import com.ecommerce.aurora.model.OrderItem;
import com.ecommerce.aurora.model.Payment;
import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.payload.OrderDTO;
import com.ecommerce.aurora.payload.OrderRequestDTO;
import com.ecommerce.aurora.payload.OrderResponse;
import com.ecommerce.aurora.repositories.AddressRepository;
import com.ecommerce.aurora.repositories.CartRepository;
import com.ecommerce.aurora.repositories.OrderItemRepository;
import com.ecommerce.aurora.repositories.OrderRepository;
import com.ecommerce.aurora.repositories.PaymentRepository;
import com.ecommerce.aurora.repositories.ProductRepository;
import com.ecommerce.aurora.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final AuthUtil authUtil;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderDTO placeOrder(String paymentMethod, OrderRequestDTO orderRequestDTO) {
        User user = authUtil.loggedInUser();

        Cart cart = cartRepository.findCartByEmail(user.getEmail());
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "email", user.getEmail());
        }

        // Snapshot the items before anything below can mutate cart.getItems() --
        // cartService.deleteProductFromCart(...) removes from that exact list, and
        // iterating a list while something else removes from it either throws
        // ConcurrentModificationException or, worse, silently skips entries as the
        // list shifts underneath the iterator.
        List<CartItem> cartItems = new ArrayList<>(cart.getItems());
        if (cartItems.isEmpty()) {
            throw new APIException("Cart is empty");
        }

        Address address = addressRepository.findById(orderRequestDTO.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", orderRequestDTO.getAddressId()));
        if (!address.getUser().getUserId().equals(user.getUserId())) {
            throw new ResourceNotFoundException("Address", "addressId", orderRequestDTO.getAddressId());
        }

        Payment payment = new Payment(paymentMethod, orderRequestDTO.getPgPaymentId(),
                orderRequestDTO.getPgStatus(), orderRequestDTO.getPgResponseMessage(), orderRequestDTO.getPgName());
        Payment savedPayment = paymentRepository.save(payment);

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order Accepted");
        order.setAddress(address);
        order.setPayment(savedPayment);
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setOrder(savedOrder);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItems.add(orderItem);
        }
        List<OrderItem> savedOrderItems = orderItemRepository.saveAll(orderItems);
        // savedOrder.orderItems is still the empty list it was constructed with --
        // it was never loaded from the database, so nothing repopulates it on its
        // own. Set it explicitly so the mapper below reflects what was actually saved.
        savedOrder.setOrderItems(savedOrderItems);

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            product.setQuantity(product.getQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            cartService.deleteProductFromCart(cart.getCartId(), product.getProductId());
        }

        return orderMapper.orderToOrderDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        if (!AppConstants.ALLOWED_ORDER_SORT_FIELDS.contains(sortBy)) {
            throw new APIException("Invalid sort field: " + sortBy);
        }
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortByAndOrder = Sort.by(direction, sortBy);
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<Order> orderPage = orderRepository.findAll(pageDetails);

        List<Long> orderIds = orderPage.getContent().stream().map(Order::getOrderId).toList();
        if (!orderIds.isEmpty()) {
            orderRepository.findAllWithItemsByOrderIdIn(orderIds);
        }

        return buildOrderResponse(orderPage);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        authUtil.assertOwnerOrAdmin(order.getUser().getUserId(), "Order", "orderId", orderId);

        return orderMapper.orderToOrderDTO(order);
    }

    private OrderResponse buildOrderResponse(Page<Order> orderPage) {
        List<OrderDTO> orderDTOs = orderPage.getContent().stream()
                .map(orderMapper::orderToOrderDTO)
                .toList();
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setContent(orderDTOs);
        orderResponse.setPageNumber(orderPage.getNumber());
        orderResponse.setPageSize(orderPage.getSize());
        orderResponse.setTotalPages(orderPage.getTotalPages());
        orderResponse.setTotalElements(orderPage.getTotalElements());
        orderResponse.setLastPage(orderPage.isLast());
        return orderResponse;
    }
}
