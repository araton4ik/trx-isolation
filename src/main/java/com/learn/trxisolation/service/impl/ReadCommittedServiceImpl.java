package com.learn.trxisolation.service.impl;

import com.learn.trxisolation.model.StudentModel;
import com.learn.trxisolation.repository.StudentRepository;
import com.learn.trxisolation.service.ReadCommittedService;
import com.learn.trxisolation.util.Flag;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class ReadCommittedServiceImpl implements ReadCommittedService {
    private final StudentRepository studentRepository;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    // this method should read only committed data
    public String checkDirtyRead(Long studentId) {
        return studentRepository.findById(studentId)
                .map(StudentModel::getGroupName)
                .orElse(null);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long readStudentCountWhenRowAddedOrDeleted() {
        return studentRepository.count();
    }

    @SneakyThrows
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Tuple2<Long, Long> checkPhantomRead(Flag flag) {
        long firstCount = studentRepository.findAll().size();
        flag.setFlagValue(false);
        while (!flag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        long secondCount = studentRepository.findAll().size();
        return Tuple.of(firstCount, secondCount);
    }

    @SneakyThrows
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Tuple2<String, String> checkNonRepeatableRead(Long id, Flag flag) {
        String firstGroup = studentRepository.findById(id).map(StudentModel::getGroupName).orElse(null);
        flag.setFlagValue(false);
        while (!flag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        String secondGroup = studentRepository.findById(id).map(StudentModel::getGroupName).orElse(null);

        return Tuple.of(firstGroup, secondGroup);
    }
}
