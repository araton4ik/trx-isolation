package com.learn.trxisolation.service.impl;

import com.learn.trxisolation.model.StudentModel;
import com.learn.trxisolation.repository.StudentRepository;
import com.learn.trxisolation.service.ReadUncommittedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ReadUncommittedServiceImpl implements ReadUncommittedService {
    private final StudentRepository studentRepository;

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public String readStudentGroupWithReadUncommittedLevel(Long studentId) {
        return studentRepository.findById(studentId)
                .map(StudentModel::getGroupName)
                .orElse(null);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public long readStudentCountWhenRowDeleted() {
        return studentRepository.count();
    }
}
