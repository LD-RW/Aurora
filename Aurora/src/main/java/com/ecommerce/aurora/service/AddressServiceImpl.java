package com.ecommerce.aurora.service;

import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import com.ecommerce.aurora.mapper.AddressMapper;
import com.ecommerce.aurora.model.Address;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.payload.AddressDTO;
import com.ecommerce.aurora.repositories.AddressRepository;
import com.ecommerce.aurora.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        authUtil.assertOwnerOrAdmin(address.getUser().getUserId(), "Address", "addressId", addressId);

        return addressMapper.addressToAddressDTO(address);
    }

    @Override
    public List<AddressDTO> getCurrentUserAddresses() {
        User user = authUtil.loggedInUser();

        return user.getAddresses().stream()
                .map(addressMapper::addressToAddressDTO)
                .toList();
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        authUtil.assertOwnerOrAdmin(address.getUser().getUserId(), "Address", "addressId", addressId);

        address.setStreet(addressDTO.getStreet());
        address.setBuildingName(addressDTO.getBuildingName());
        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setCountry(addressDTO.getCountry());
        address.setPinCode(addressDTO.getPinCode());

        Address updatedAddress = addressRepository.save(address);

        return addressMapper.addressToAddressDTO(updatedAddress);
    }

    @Override
    public AddressDTO deleteAddress(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        authUtil.assertOwnerOrAdmin(address.getUser().getUserId(), "Address", "addressId", addressId);

        addressRepository.delete(address);

        return addressMapper.addressToAddressDTO(address);
    }
}
