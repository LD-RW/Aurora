package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.model.Address;
import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Role;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.payload.AddressDTO;
import com.ecommerce.aurora.repositories.AddressRepository;
import com.ecommerce.aurora.repositories.RoleRepository;
import com.ecommerce.aurora.repositories.UserRepository;
import com.ecommerce.aurora.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private RoleRepository roleRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAnAddressForTheAuthenticatedUser() throws Exception {
        User user = userRepository.save(new User("addressOwner", "password12345", "owner@example.com"));
        authenticateAs(user);

        AddressDTO requestBody = new AddressDTO(null, "Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addressId").exists())
                .andExpect(jsonPath("$.street").value("Main Street"))
                .andExpect(jsonPath("$.pinCode").value("11183"));

        assertThat(addressRepository.findAll())
                .singleElement()
                .satisfies(saved -> assertThat(saved.getUser().getUserId()).isEqualTo(user.getUserId()));
    }

    @Test
    void rejectsAnUnauthenticatedRequest() throws Exception {
        AddressDTO requestBody = new AddressDTO(null, "Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized());

        assertThat(addressRepository.count()).isZero();
    }

    @Test
    void rejectsAnInvalidAddressPayload() throws Exception {
        User user = userRepository.save(new User("addressOwner2", "password12345", "owner2@example.com"));
        authenticateAs(user);

        AddressDTO tooShortPinCode = new AddressDTO(null, "Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "111");

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooShortPinCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pinCode").value("Pin code must be at least 5 characters"));

        assertThat(addressRepository.count()).isZero();
    }

    @Test
    void ignoresAClientSuppliedAddressIdAndNeverOverwritesAnotherUsersRow() throws Exception {
        User victim = userRepository.save(new User("victim", "password12345", "victim@example.com"));
        Address victimAddress = new Address("Victim Street", "Victim Building", "Amman", "Amman Governorate", "Jordan", "11183");
        victimAddress.setUser(victim);
        Address savedVictimAddress = addressRepository.saveAndFlush(victimAddress);

        User attacker = userRepository.save(new User("attacker", "password12345", "attacker@example.com"));
        authenticateAs(attacker);

        AddressDTO forgedRequestBody = new AddressDTO(
                savedVictimAddress.getAddressId(), "Attacker Street", "Attacker Building",
                "Amman", "Amman Governorate", "Jordan", "99999");

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgedRequestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addressId").value(org.hamcrest.Matchers.not(savedVictimAddress.getAddressId().intValue())));

        Address victimRowAfterAttack = addressRepository.findById(savedVictimAddress.getAddressId()).orElseThrow();
        assertThat(victimRowAfterAttack.getStreet()).isEqualTo("Victim Street");
        assertThat(victimRowAfterAttack.getUser().getUsername()).isEqualTo("victim");

        assertThat(addressRepository.count()).isEqualTo(2);
    }

    @Test
    void listsAllAddressesAcrossEveryUserForAnAdmin() throws Exception {
        User firstOwner = userRepository.save(new User("firstOwner", "password12345", "first@example.com"));
        Address firstAddress = new Address("First Street", "First Building", "Amman", "Amman Governorate", "Jordan", "11183");
        firstAddress.setUser(firstOwner);
        addressRepository.save(firstAddress);

        User secondOwner = userRepository.save(new User("secondOwner", "password12345", "second@example.com"));
        Address secondAddress = new Address("Second Street", "Second Building", "Irbid", "Irbid Governorate", "Jordan", "21110");
        secondAddress.setUser(secondOwner);
        addressRepository.save(secondAddress);

        authenticateAsAdmin();

        mockMvc.perform(get("/api/admin/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].street", org.hamcrest.Matchers.containsInAnyOrder("First Street", "Second Street")));
    }

    @Test
    void returnsAnEmptyListWhenThereAreNoAddressesInsteadOfAnError() throws Exception {
        authenticateAsAdmin();

        mockMvc.perform(get("/api/admin/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsAnAuthenticatedNonAdminUserFromListingAllAddresses() throws Exception {
        User user = userRepository.save(new User("regularUser", "password12345", "regular@example.com"));
        authenticateAs(user);

        mockMvc.perform(get("/api/admin/addresses"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsAnUnauthenticatedRequestToListAllAddresses() throws Exception {
        mockMvc.perform(get("/api/admin/addresses"))
                .andExpect(status().isUnauthorized());
    }

    private void authenticateAs(User user) {
        UserDetailsImpl principal = UserDetailsImpl.build(user);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void authenticateAsAdmin() {
        Role adminRole = roleRepository.save(new Role(AppRole.ROLE_ADMIN));
        User admin = new User("addressTestAdmin", "password12345", "addressadmin@example.com");
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);

        authenticateAs(admin);
    }
}
