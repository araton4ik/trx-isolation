package com.learn.trxisolation.service.isolation.impl;

import com.learn.trxisolation.model.StudentModel;
import com.learn.trxisolation.repository.StudentRepository;
import com.learn.trxisolation.service.isolation.IsolationService;
import com.learn.trxisolation.util.Flag;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Primary
public class IsolationServiceImpl implements IsolationService {

    private final StudentRepository studentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String checkDirtyRead(Long studentId) {
        return studentRepository.findById(studentId)
                .map(StudentModel::getGroupName)
                .orElse(null);
    }

    @Override
    public long checkDirtyReadWhenUncommittedTrxDeletedRow() {
        return studentRepository.count();
    }

    @SneakyThrows
    @Override
    public Tuple2<String, String> checkNonRepeatableRead(Long id, Flag flag) {
        String firstGroup = studentRepository.findById(id)
                .map(StudentModel::getGroupName)
                .orElse(null);
        flag.setFlagValue(false);
        while (!flag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(100);
        }
        // we use jdbc template as otherwise hibernate uses cached value
        String secondGroup = jdbcTemplate.queryForObject("Select group_name from student where id = ?", String.class, id);
        return Tuple.of(firstGroup, secondGroup);
    }

    @SneakyThrows
    @Override
    public Tuple2<Long, Long> checkPhantomRead(Flag flag) {
        long firstCount = studentRepository.count();
        flag.setFlagValue(false);
        while (!flag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(100);
        }
        // we use jdbc template as otherwise hibernate uses cached value
        long secondCount = jdbcTemplate.queryForObject("Select count(*) from student", Integer.class);
        return Tuple.of(firstCount, secondCount);
    }
}
