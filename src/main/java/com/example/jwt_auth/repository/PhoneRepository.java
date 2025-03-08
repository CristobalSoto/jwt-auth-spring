package com.example.jwt_auth.repository;

import com.example.jwt_auth.models.Phone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PhoneRepository extends JpaRepository<Phone, UUID> {
} 