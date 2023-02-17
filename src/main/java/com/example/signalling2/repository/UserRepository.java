package com.example.signalling2.repository;

import com.example.signalling2.domain.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, String> {

}
