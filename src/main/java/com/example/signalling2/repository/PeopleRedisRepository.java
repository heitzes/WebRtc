package com.example.signalling2.repository;

import com.example.signalling2.domain.People;
import org.springframework.data.repository.CrudRepository;

public interface PeopleRedisRepository extends CrudRepository<People, String> {
}