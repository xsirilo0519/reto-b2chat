package com.b2chat.order_manager.reactive.web.user.mapper;


import com.b2chat.order_manager.domain.users.entity.UserEntity;
import com.b2chat.order_manager.reactive.web.user.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserMapper {
    UserMapper INTANCE = Mappers.getMapper(UserMapper.class);

    UserEntity toEntity(UserDto userDto);
}
