package com.ecommerce.aurora.repositories;

import com.ecommerce.aurora.model.Address;
import com.ecommerce.aurora.model.User;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class AddressRepositoryTest {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndReloadsAnAddressLinkedToItsUser() {
        User user = userRepository.save(new User("scratchUser", "password12345", "scratch@example.com"));

        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(user);

        Address saved = addressRepository.save(address);

        assertThat(saved.getAddressId()).isNotNull();

        Address reloaded = addressRepository.findById(saved.getAddressId()).orElseThrow();
        assertThat(reloaded.getStreet()).isEqualTo("Main Street");
        assertThat(reloaded.getUser().getUserId()).isEqualTo(user.getUserId());
    }

    @Test
    void rejectsAnAddressThatFailsBeanValidation() {
        Address tooShortPinCode = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "111");

        assertThatThrownBy(() -> addressRepository.saveAndFlush(tooShortPinCode))
                .isInstanceOfAny(ConstraintViolationException.class, DataIntegrityViolationException.class);
    }
}
