package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.model.Address;
import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Order;
import com.ecommerce.aurora.model.Payment;
import com.ecommerce.aurora.model.Role;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.repositories.AddressRepository;
import com.ecommerce.aurora.repositories.OrderRepository;
import com.ecommerce.aurora.repositories.PaymentRepository;
import com.ecommerce.aurora.repositories.RoleRepository;
import com.ecommerce.aurora.repositories.UserRepository;
import com.ecommerce.aurora.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deleting an address an order still points at is refused by the foreign key on
 * orders.address_id. The resulting DataIntegrityViolationException had no handler, so it reached
 * the catch-all and was reported as a 500 -- an unhandled server error for what is really a
 * conflict the caller can understand and act on.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AddressDeletionConflictTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reportsAConflictRatherThanAServerErrorWhenAnOrderStillReferencesTheAddress() throws Exception {
        User owner = persistUser();
        Address address = persistAddressFor(owner);
        persistOrderFor(owner, address);

        authenticateAs(owner);

        mockMvc.perform(delete("/api/addresses/" + address.getAddressId()))
                .andExpect(status().isConflict());

        assertThat(addressRepository.findById(address.getAddressId())).isPresent();
    }

    @Test
    void stillDeletesAnAddressNoOrderReferences() throws Exception {
        User owner = persistUser();
        Address address = persistAddressFor(owner);

        authenticateAs(owner);

        mockMvc.perform(delete("/api/addresses/" + address.getAddressId()))
                .andExpect(status().isOk());

        assertThat(addressRepository.findById(address.getAddressId())).isEmpty();
    }

    private User persistUser() {
        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(AppRole.ROLE_USER)));
        User user = new User("addressConflictUser", "password12345", "addressconflict@example.com");
        user.setRoles(Set.of(userRole));
        user.setAddresses(new ArrayList<>());
        return userRepository.saveAndFlush(user);
    }

    private Address persistAddressFor(User user) {
        Address address = new Address();
        address.setStreet("123 Conflict Street");
        address.setBuildingName("Conflict Tower");
        address.setCity("Conflictville");
        address.setState("CF");
        address.setCountry("Testland");
        address.setPinCode("12345");
        address.setUser(user);
        return addressRepository.saveAndFlush(address);
    }

    private void persistOrderFor(User user, Address address) {
        Payment payment = paymentRepository.saveAndFlush(
                new Payment("CashOnDelivery", null, null, null, null));

        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setPayment(payment);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(BigDecimal.valueOf(100));
        order.setOrderStatus("Order Accepted");
        orderRepository.saveAndFlush(order);
    }

    private void authenticateAs(User user) {
        UserDetailsImpl principal = UserDetailsImpl.build(user);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
