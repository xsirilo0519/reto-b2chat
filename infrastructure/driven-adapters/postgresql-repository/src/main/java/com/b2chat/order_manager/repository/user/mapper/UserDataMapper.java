package com.b2chat.order_manager.repository.user.mapper;

import com.b2chat.order_manager.domain.users.entity.UserEntity;
import com.b2chat.order_manager.repository.user.data.UserData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserDataMapper {
    UserDataMapper INSTANCE = Mappers.getMapper(UserDataMapper.class);

    UserData toData(UserEntity userEntity);
    UserEntity toDomain(UserData userData);
}
