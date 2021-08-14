package com.learn.trxisolation.service;

public interface ReadUncommittedService {

    String readStudentGroupWithReadUncommittedLevel(Long studentId);

    long readStudentCountWhenRowDeleted();
}
