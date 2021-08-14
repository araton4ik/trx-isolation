package com.learn.trxisolation.service;

import com.learn.trxisolation.model.StudentModel;
import com.learn.trxisolation.repository.StudentRepository;
import com.learn.trxisolation.util.Flag;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IsolationServiceImpl implements IsolationService {

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
    public long readStudentCountWhenRowDeletedWithUncommittedLevel() {
        return studentRepository.count();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    // this method should read only committed data
    public String readStudentGroupWithReadCommittedLevel(Long studentId) {
        return studentRepository.findById(studentId)
                .map(StudentModel::getGroupName)
                .orElse(null);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    // this method should read only committed data
    public long readStudentCountWhenRowAddedOrDeletedWithReadCommittedLevel() {
        return studentRepository.count();
    }

    @SneakyThrows
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Tuple2<Long, Long> checkPhantomReadWhenLevelIsReadCommitted(Flag flag) {
        return checkPhantomRead(flag);
    }

    @SneakyThrows
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Tuple2<String, String> checkNonRepeatableReadWhenLevelIsReadCommitted(Long id, Flag flag) {
        return readGroups(id, flag);
    }

    @SneakyThrows
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Tuple2<String, String> checkNonRepeatableReadWhenLevelIsReadRepeatable(Long id, Flag flag) {
        return readGroups(id, flag);
    }

    @SneakyThrows
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Tuple2<Long, Long> checkPhantomReadWhenLevelIsReadRepeatable(Flag flag) {
        return checkPhantomRead(flag);
    }

    @SneakyThrows
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Tuple2<String, String> checkNonRepeatableReadWhenLevelIsSerialize(Long id, Flag flag) {
        return readGroups(id, flag);
    }

    @SneakyThrows
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Tuple2<Long, Long> checkPhantomReadWhenLevelIsSerialize(Flag flag) {
        return checkPhantomRead(flag);
    }

    private Tuple2<Long, Long> checkPhantomRead(Flag flag) throws InterruptedException {
        long firstCount = studentRepository.findAll().size();
        flag.setFlagValue(false);
        while (!flag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        long secondCount = studentRepository.findAll().size();
        return Tuple.of(firstCount, secondCount);
    }

    private Tuple2<String, String> readGroups(Long id, Flag flag) throws InterruptedException {
        String firstGroup = studentRepository.findById(id).map(StudentModel::getGroupName).orElse(null);
        flag.setFlagValue(false);
        while (!flag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        String secondGroup = studentRepository.findById(id).map(StudentModel::getGroupName).orElse(null);

        return Tuple.of(firstGroup, secondGroup);
    }
}
