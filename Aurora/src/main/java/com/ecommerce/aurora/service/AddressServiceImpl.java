package com.ecommerce.aurora.service;

import com.ecommerce.aurora.mapper.AddressMapper;
import com.ecommerce.aurora.model.Address;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.payload.AddressDTO;
import com.ecommerce.aurora.repositories.AddressRepository;
import com.ecommerce.aurora.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final AuthUtil authUtil;

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO) {
        User user = authUtil.loggedInUser();

        Address address = addressMapper.addressDTOToAddress(addressDTO);
        address.setUser(user);

        Address savedAddress = addressRepository.save(address);

        return addressMapper.addressToAddressDTO(savedAddress);
    }

    @Override
    public List<AddressDTO> getAllAddresses() {
        return addressRepository.findAll().stream()
                .map(addressMapper::addressToAddressDTO)
                .toList();
    }
}
