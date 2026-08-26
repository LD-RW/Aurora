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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @PersistenceContext
    private EntityManager entityManager;

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

    @Test
    void ownerCanFetchTheirOwnAddressById() throws Exception {
        User owner = userRepository.save(new User("addressGetOwner", "password12345", "getowner@example.com"));
        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(owner);
        Address savedAddress = addressRepository.saveAndFlush(address);

        authenticateAs(owner);

        mockMvc.perform(get("/api/addresses/" + savedAddress.getAddressId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(savedAddress.getAddressId()))
                .andExpect(jsonPath("$.street").value("Main Street"));
    }

    @Test
    void adminCanFetchAnyUsersAddressById() throws Exception {
        User owner = userRepository.save(new User("addressGetOwner2", "password12345", "getowner2@example.com"));
        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(owner);
        Address savedAddress = addressRepository.saveAndFlush(address);

        authenticateAsAdmin();

        mockMvc.perform(get("/api/addresses/" + savedAddress.getAddressId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(savedAddress.getAddressId()));
    }

    @Test
    void treatsAnotherUsersAddressAsNotFoundForANonOwnerNonAdmin() throws Exception {
        User owner = userRepository.save(new User("addressGetOwner3", "password12345", "getowner3@example.com"));
        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(owner);
        Address savedAddress = addressRepository.saveAndFlush(address);

        User otherUser = userRepository.save(new User("addressGetOther", "password12345", "getother@example.com"));
        authenticateAs(otherUser);

        mockMvc.perform(get("/api/addresses/" + savedAddress.getAddressId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Address not found with addressId: " + savedAddress.getAddressId()));
    }

    @Test
    void returnsNotFoundForANonexistentAddressIdWithTheSameMessageShapeAsANotOwnedOne() throws Exception {
        User user = userRepository.save(new User("addressGetOwner4", "password12345", "getowner4@example.com"));
        authenticateAs(user);

        mockMvc.perform(get("/api/addresses/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Address not found with addressId: 999999"));
    }

    @Test
    void rejectsAnUnauthenticatedRequestToGetAddressById() throws Exception {
        mockMvc.perform(get("/api/addresses/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsOnlyTheCurrentUsersAddresses() throws Exception {
        User owner = userRepository.save(new User("myAddressesOwner", "password12345", "myaddresses@example.com"));
        Address firstAddress = new Address("First Street", "First Building", "Amman", "Amman Governorate", "Jordan", "11183");
        firstAddress.setUser(owner);
        addressRepository.save(firstAddress);
        Address secondAddress = new Address("Second Street", "Second Building", "Amman", "Amman Governorate", "Jordan", "11184");
        secondAddress.setUser(owner);
        addressRepository.save(secondAddress);

        User otherUser = userRepository.save(new User("myAddressesOther", "password12345", "myaddressesother@example.com"));
        Address otherUsersAddress = new Address("Other Street", "Other Building", "Irbid", "Irbid Governorate", "Jordan", "21110");
        otherUsersAddress.setUser(otherUser);
        addressRepository.save(otherUsersAddress);

        // Force a fresh read of `owner` for authentication: within this single test
        // transaction, `owner`'s in-memory `addresses` collection was never touched
        // before the two addresses above were created via their owning (Address) side.
        // Without clearing the persistence context, Hibernate's first-level cache would
        // hand the controller this exact same `User` instance later, and the *first*
        // access to its `addresses` collection would still correctly lazy-load from the
        // database -- so this only matters here because the assertion happens inside
        // the same transaction as the setup, which a real request never does (each HTTP
        // request gets its own persistence context).
        entityManager.flush();
        entityManager.clear();
        User refreshedOwner = userRepository.findById(owner.getUserId()).orElseThrow();
        authenticateAs(refreshedOwner);

        mockMvc.perform(get("/api/users/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].street", org.hamcrest.Matchers.containsInAnyOrder("First Street", "Second Street")));
    }

    @Test
    void returnsAnEmptyListWhenTheCurrentUserHasNoAddresses() throws Exception {
        User user = userRepository.save(new User("noAddressesUser", "password12345", "noaddresses@example.com"));
        authenticateAs(user);

        mockMvc.perform(get("/api/users/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsAnUnauthenticatedRequestToGetCurrentUserAddresses() throws Exception {
        mockMvc.perform(get("/api/users/addresses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanUpdateTheirOwnAddress() throws Exception {
        User owner = userRepository.save(new User("addressUpdateOwner", "password12345", "updateowner@example.com"));
        Address address = new Address("Old Street", "Old Building", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(owner);
        Address savedAddress = addressRepository.saveAndFlush(address);

        authenticateAs(owner);

        AddressDTO updateRequestBody = new AddressDTO(null, "New Street", "New Building", "Irbid", "Irbid Governorate", "Jordan", "21110");

        mockMvc.perform(put("/api/addresses/" + savedAddress.getAddressId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(savedAddress.getAddressId()))
                .andExpect(jsonPath("$.street").value("New Street"))
                .andExpect(jsonPath("$.city").value("Irbid"));

        Address updatedAddress = addressRepository.findById(savedAddress.getAddressId()).orElseThrow();
        assertThat(updatedAddress.getStreet()).isEqualTo("New Street");
        assertThat(updatedAddress.getCity()).isEqualTo("Irbid");
        assertThat(updatedAddress.getUser().getUserId()).isEqualTo(owner.getUserId());
    }

    @Test
    void adminCanUpdateAnyUsersAddress() throws Exception {
        User owner = userRepository.save(new User("addressUpdateOwner2", "password12345", "updateowner2@example.com"));
        Address address = new Address("Old Street", "Old Building", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(owner);
        Address savedAddress = addressRepository.saveAndFlush(address);

        authenticateAsAdmin();

        AddressDTO updateRequestBody = new AddressDTO(null, "New Street", "New Building", "Irbid", "Irbid Governorate", "Jordan", "21110");

        mockMvc.perform(put("/api/addresses/" + savedAddress.getAddressId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("New Street"));

        Address updatedAddress = addressRepository.findById(savedAddress.getAddressId()).orElseThrow();
        assertThat(updatedAddress.getUser().getUserId()).isEqualTo(owner.getUserId());
    }

    @Test
    void treatsAnotherUsersAddressAsNotFoundWhenUpdatingAsANonOwnerNonAdmin() throws Exception {
        User owner = userRepository.save(new User("addressUpdateOwner3", "password12345", "updateowner3@example.com"));
        Address address = new Address("Old Street", "Old Building", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(owner);
        Address savedAddress = addressRepository.saveAndFlush(address);

        User otherUser = userRepository.save(new User("addressUpdateOther", "password12345", "updateother@example.com"));
        authenticateAs(otherUser);

        AddressDTO updateRequestBody = new AddressDTO(null, "New Street", "New Building", "Irbid", "Irbid Governorate", "Jordan", "21110");

        mockMvc.perform(put("/api/addresses/" + savedAddress.getAddressId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestBody)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Address not found with addressId: " + savedAddress.getAddressId()));

        Address untouchedAddress = addressRepository.findById(savedAddress.getAddressId()).orElseThrow();
        assertThat(untouchedAddress.getStreet()).isEqualTo("Old Street");
    }

    @Test
    void returnsNotFoundWhenUpdatingANonexistentAddress() throws Exception {
        User user = userRepository.save(new User("addressUpdateOwner4", "password12345", "updateowner4@example.com"));
        authenticateAs(user);

        AddressDTO updateRequestBody = new AddressDTO(null, "New Street", "New Building", "Irbid", "Irbid Governorate", "Jordan", "21110");

        mockMvc.perform(put("/api/addresses/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestBody)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Address not found with addressId: 999999"));
    }

    @Test
    void rejectsAnInvalidUpdatePayload() throws Exception {
        User owner = userRepository.save(new User("addressUpdateOwner5", "password12345", "updateowner5@example.com"));
        Address address = new Address("Old Street", "Old Building", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(owner);
        Address savedAddress = addressRepository.saveAndFlush(address);

        authenticateAs(owner);

        AddressDTO tooShortPinCode = new AddressDTO(null, "New Street", "New Building", "Irbid", "Irbid Governorate", "Jordan", "111");

        mockMvc.perform(put("/api/addresses/" + savedAddress.getAddressId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooShortPinCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pinCode").value("Pin code must be at least 5 characters"));

        Address untouchedAddress = addressRepository.findById(savedAddress.getAddressId()).orElseThrow();
        assertThat(untouchedAddress.getStreet()).isEqualTo("Old Street");
    }

    @Test
    void rejectsAnUnauthenticatedRequestToUpdateAnAddress() throws Exception {
        AddressDTO updateRequestBody = new AddressDTO(null, "New Street", "New Building", "Irbid", "Irbid Governorate", "Jordan", "21110");

        mockMvc.perform(put("/api/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestBody)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanDeleteTheirOwnAddress() throws Exception {
        User owner = userRepository.save(new User("addressDeleteOwner", "password12345", "deleteowner@example.com"));
        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(owner);
        Address savedAddress = addressRepository.saveAndFlush(address);

        authenticateAs(owner);

        mockMvc.perform(delete("/api/addresses/" + savedAddress.getAddressId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(savedAddress.getAddressId()))
                .andExpect(jsonPath("$.street").value("Main Street"));

        assertThat(addressRepository.findById(savedAddress.getAddressId())).isEmpty();
    }

    @Test
    void adminCanDeleteAnyUsersAddress() throws Exception {
        User owner = userRepository.save(new User("addressDeleteOwner2", "password12345", "deleteowner2@example.com"));
        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(owner);
        Address savedAddress = addressRepository.saveAndFlush(address);

        authenticateAsAdmin();

        mockMvc.perform(delete("/api/addresses/" + savedAddress.getAddressId()))
                .andExpect(status().isOk());

        assertThat(addressRepository.findById(savedAddress.getAddressId())).isEmpty();
    }

    @Test
    void treatsAnotherUsersAddressAsNotFoundWhenDeletingAsANonOwnerNonAdmin() throws Exception {
        User owner = userRepository.save(new User("addressDeleteOwner3", "password12345", "deleteowner3@example.com"));
        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(owner);
        Address savedAddress = addressRepository.saveAndFlush(address);

        User otherUser = userRepository.save(new User("addressDeleteOther", "password12345", "deleteother@example.com"));
        authenticateAs(otherUser);

        mockMvc.perform(delete("/api/addresses/" + savedAddress.getAddressId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Address not found with addressId: " + savedAddress.getAddressId()));

        assertThat(addressRepository.findById(savedAddress.getAddressId())).isPresent();
    }

    @Test
    void returnsNotFoundWhenDeletingANonexistentAddress() throws Exception {
        User user = userRepository.save(new User("addressDeleteOwner4", "password12345", "deleteowner4@example.com"));
        authenticateAs(user);

        mockMvc.perform(delete("/api/addresses/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Address not found with addressId: 999999"));
    }

    @Test
    void rejectsAnUnauthenticatedRequestToDeleteAnAddress() throws Exception {
        mockMvc.perform(delete("/api/addresses/1"))
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
