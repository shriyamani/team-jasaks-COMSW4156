package com.jasaks.repository;

import com.jasaks.model.Booker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookerRepository extends JpaRepository<Booker, String> {
}
