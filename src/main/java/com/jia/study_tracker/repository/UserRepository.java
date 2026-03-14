package com.jia.study_tracker.repository;


import com.jia.study_tracker.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

}