package com.learn.trxisolation.service;

import com.learn.trxisolation.model.StudentModel;
import com.learn.trxisolation.util.Flag;

public interface StudentService {

    StudentModel addStudent(StudentModel studentModel, Flag flag, boolean throwException);

    void deleteStudent(Long id, Flag flag, boolean throwException);

    void changeGroup(Long id, String group, Flag flag, boolean throwException);

    void addStudent(StudentModel studentModel);

    void changeGroup(Long id, String group);
}
