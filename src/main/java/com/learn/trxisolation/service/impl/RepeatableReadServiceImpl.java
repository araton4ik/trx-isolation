package com.learn.trxisolation.service.impl;

import com.learn.trxisolation.model.StudentModel;
import com.learn.trxisolation.repository.StudentRepository;
import com.learn.trxisolation.service.RepeatableReadService;
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
public class RepeatableReadServiceImpl implements RepeatableReadService {
    private final StudentRepository studentRepository;

    @SneakyThrows
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Tuple2<String, String> checkNonRepeatableRead(Long id, Flag flag) {
        String firstGroup = studentRepository.findById(id).map(StudentModel::getGroupName).orElse(null);
        flag.setFlagValue(false);
        while (!flag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        String secondGroup = studentRepository.findById(id).map(StudentModel::getGroupName).orElse(null);
        return Tuple.of(firstGroup, secondGroup);
    }

    @SneakyThrows
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Tuple2<Long, Long> checkPhantomRead(Flag flag) {
        long firstCount = studentRepository.findAll().size();
        flag.setFlagValue(false);
        while (!flag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        long secondCount = studentRepository.findAll().size();
        return Tuple.of(firstCount, secondCount);
    }
}
