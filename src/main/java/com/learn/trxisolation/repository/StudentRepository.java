package com.learn.trxisolation.repository;

import com.learn.trxisolation.model.StudentModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<StudentModel, Long> {
}
