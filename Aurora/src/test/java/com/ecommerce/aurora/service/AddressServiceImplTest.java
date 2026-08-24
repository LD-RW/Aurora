package com.ecommerce.aurora.service;

import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import com.ecommerce.aurora.mapper.AddressMapper;
import com.ecommerce.aurora.model.Address;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.payload.AddressDTO;
import com.ecommerce.aurora.repositories.AddressRepository;
import com.ecommerce.aurora.util.AuthUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private AuthUtil authUtil;

    @Test
    void attachesTheLoggedInUserBeforeSavingAndReturnsTheMappedSavedEntity() {
        AddressServiceImpl addressService = new AddressServiceImpl(addressRepository, addressMapper, authUtil);

        AddressDTO incomingDto = new AddressDTO(null, "Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        User loggedInUser = new User("someone", "password12345", "someone@example.com");
        Address mappedFromDto = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        Address savedAddress = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        savedAddress.setAddressId(42L);
        AddressDTO expectedResponseDto = new AddressDTO(42L, "Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");

        when(authUtil.loggedInUser()).thenReturn(loggedInUser);
        when(addressMapper.addressDTOToAddress(incomingDto)).thenReturn(mappedFromDto);
        when(addressRepository.save(mappedFromDto)).thenReturn(savedAddress);
        when(addressMapper.addressToAddressDTO(savedAddress)).thenReturn(expectedResponseDto);

        AddressDTO result = addressService.createAddress(incomingDto);

        assertThat(result).isEqualTo(expectedResponseDto);
        assertThat(mappedFromDto.getUser()).isEqualTo(loggedInUser);
        verify(addressRepository).save(mappedFromDto);
    }

    @Test
    void mapsEveryAddressReturnedByTheRepository() {
        AddressServiceImpl addressService = new AddressServiceImpl(addressRepository, addressMapper, authUtil);

        Address firstAddress = new Address("First Street", "First Building", "Amman", "Amman Governorate", "Jordan", "11183");
        Address secondAddress = new Address("Second Street", "Second Building", "Irbid", "Irbid Governorate", "Jordan", "21110");
        AddressDTO firstDto = new AddressDTO(1L, "First Street", "First Building", "Amman", "Amman Governorate", "Jordan", "11183");
        AddressDTO secondDto = new AddressDTO(2L, "Second Street", "Second Building", "Irbid", "Irbid Governorate", "Jordan", "21110");

        when(addressRepository.findAll()).thenReturn(List.of(firstAddress, secondAddress));
        when(addressMapper.addressToAddressDTO(firstAddress)).thenReturn(firstDto);
        when(addressMapper.addressToAddressDTO(secondAddress)).thenReturn(secondDto);

        List<AddressDTO> result = addressService.getAllAddresses();

        assertThat(result).containsExactly(firstDto, secondDto);
    }

    @Test
    void returnsTheAddressWhenTheCallerIsTheOwner() {
        AddressServiceImpl addressService = new AddressServiceImpl(addressRepository, addressMapper, authUtil);

        User owner = new User("owner", "password12345", "owner@example.com");
        owner.setUserId(1L);
        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(owner);
        address.setAddressId(10L);
        AddressDTO expectedDto = new AddressDTO(10L, "Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");

        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
        when(authUtil.loggedInUserId()).thenReturn(1L);
        when(addressMapper.addressToAddressDTO(address)).thenReturn(expectedDto);

        assertThat(addressService.getAddressById(10L)).isEqualTo(expectedDto);
    }

    @Test
    void returnsTheAddressWhenTheCallerIsAnAdminEvenIfNotTheOwner() {
        AddressServiceImpl addressService = new AddressServiceImpl(addressRepository, addressMapper, authUtil);

        User owner = new User("owner", "password12345", "owner@example.com");
        owner.setUserId(1L);
        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(owner);
        address.setAddressId(10L);
        AddressDTO expectedDto = new AddressDTO(10L, "Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");

        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
        when(authUtil.loggedInUserId()).thenReturn(2L);
        when(authUtil.isCurrentUserAdmin()).thenReturn(true);
        when(addressMapper.addressToAddressDTO(address)).thenReturn(expectedDto);

        assertThat(addressService.getAddressById(10L)).isEqualTo(expectedDto);
    }

    @Test
    void throwsNotFoundWhenTheCallerIsNeitherTheOwnerNorAnAdmin() {
        AddressServiceImpl addressService = new AddressServiceImpl(addressRepository, addressMapper, authUtil);

        User owner = new User("owner", "password12345", "owner@example.com");
        owner.setUserId(1L);
        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(owner);
        address.setAddressId(10L);

        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
        when(authUtil.loggedInUserId()).thenReturn(2L);
        when(authUtil.isCurrentUserAdmin()).thenReturn(false);

        assertThatThrownBy(() -> addressService.getAddressById(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenTheAddressDoesNotExist() {
        AddressServiceImpl addressService = new AddressServiceImpl(addressRepository, addressMapper, authUtil);

        when(addressRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getAddressById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void returnsAddressesBelongingToTheLoggedInUser() {
        AddressServiceImpl addressService = new AddressServiceImpl(addressRepository, addressMapper, authUtil);

        Address firstAddress = new Address("First Street", "First Building", "Amman", "Amman Governorate", "Jordan", "11183");
        Address secondAddress = new Address("Second Street", "Second Building", "Irbid", "Irbid Governorate", "Jordan", "21110");
        User loggedInUser = new User("someone", "password12345", "someone@example.com");
        loggedInUser.setAddresses(List.of(firstAddress, secondAddress));
        AddressDTO firstDto = new AddressDTO(1L, "First Street", "First Building", "Amman", "Amman Governorate", "Jordan", "11183");
        AddressDTO secondDto = new AddressDTO(2L, "Second Street", "Second Building", "Irbid", "Irbid Governorate", "Jordan", "21110");

        when(authUtil.loggedInUser()).thenReturn(loggedInUser);
        when(addressMapper.addressToAddressDTO(firstAddress)).thenReturn(firstDto);
        when(addressMapper.addressToAddressDTO(secondAddress)).thenReturn(secondDto);

        List<AddressDTO> result = addressService.getCurrentUserAddresses();

        assertThat(result).containsExactly(firstDto, secondDto);
    }
}
