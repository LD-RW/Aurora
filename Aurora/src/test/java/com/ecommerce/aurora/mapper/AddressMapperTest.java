package com.ecommerce.aurora.mapper;

import com.ecommerce.aurora.model.Address;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.payload.AddressDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressMapperTest {

    private final AddressMapper addressMapper = new AddressMapperImpl();

    @Test
    void mapsEveryFieldFromEntityToDTO() {
        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setAddressId(7L);
        address.setUser(new User("someone", "password12345", "someone@example.com"));

        AddressDTO dto = addressMapper.addressToAddressDTO(address);

        assertThat(dto.getAddressId()).isEqualTo(7L);
        assertThat(dto.getStreet()).isEqualTo("Main Street");
        assertThat(dto.getBuildingName()).isEqualTo("Building A");
        assertThat(dto.getCity()).isEqualTo("Amman");
        assertThat(dto.getState()).isEqualTo("Amman Governorate");
        assertThat(dto.getCountry()).isEqualTo("Jordan");
        assertThat(dto.getPinCode()).isEqualTo("11183");
    }

    @Test
    void mapsDTOToEntityAndLeavesUserUnsetForTheServiceLayerToFill() {
        AddressDTO dto = new AddressDTO(null, "Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");

        Address address = addressMapper.addressDTOToAddress(dto);

        assertThat(address.getStreet()).isEqualTo("Main Street");
        assertThat(address.getPinCode()).isEqualTo("11183");
        assertThat(address.getUser()).isNull();
    }
}
