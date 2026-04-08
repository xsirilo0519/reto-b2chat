package com.b2chat.order_manager.repository.user.repository;

import com.b2chat.order_manager.repository.user.data.UserData;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface UserDataRepository extends ReactiveCrudRepository<UserData, Long> {
}
