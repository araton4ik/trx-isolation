package com.learn.trxisolation.service.impl;

import com.learn.trxisolation.model.StudentModel;
import com.learn.trxisolation.repository.StudentRepository;
import com.learn.trxisolation.service.StudentService;
import com.learn.trxisolation.util.Flag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private static final String FLAG_SET = "flag was reset";
    private static final String EXIT = "Exit from methods";
    private static final String WAIT = "Wait for flag change...";

    @Override
    @Transactional
    public void deleteStudent(Long id, Flag flag, boolean throwException) {
        log.info("Delete student with id {}", id);
        studentRepository.deleteById(id);
        flushAndWait(flag, throwException);
    }


    @Override
    @Transactional
    public void changeGroup(Long id, String group, Flag flag, boolean throwException) {
        log.info("Change student group to {}", group);
        var student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Wrong id"));
        student.setGroupName(group);
        studentRepository.save(student);
        flushAndWait(flag, throwException);

    }

    private void flushAndWait(Flag flag, boolean throwException) {
        studentRepository.flush();
        flag.setFlagValue(false);
        log.info(FLAG_SET);
        try {
            while (!flag.isFlagValue()) {
                TimeUnit.MILLISECONDS.sleep(500);
                log.info(WAIT);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        if (throwException) {
            throw new RuntimeException("Something went wrong");
        }
        log.info(EXIT);
    }

    @Override
    @Transactional
    public void addStudent(StudentModel studentModel) {
        log.info("Add new student {}", studentModel);
        studentRepository.save(studentModel);
        studentRepository.flush();
    }

    @Override
    @Transactional
    public void changeGroup(Long id, String group) {
        StudentModel studentModel = studentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Wrong id"));
        studentModel.setGroupName(group);
        studentRepository.save(studentModel);
        studentRepository.flush();
        log.info("Group was changed");
    }
}
