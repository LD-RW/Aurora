package com.ecommerce.aurora.mapper;

import com.ecommerce.aurora.model.Address;
import com.ecommerce.aurora.payload.AddressDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    AddressDTO addressToAddressDTO(Address address);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "addressId", ignore = true)
    Address addressDTOToAddress(AddressDTO addressDTO);
}
