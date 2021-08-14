package com.learn.trxisolation.service;

import com.learn.trxisolation.util.Flag;
import io.vavr.Tuple2;

public interface ReadCommittedService {

    String checkDirtyRead(Long studentId);

    long readStudentCountWhenRowAddedOrDeleted();

    Tuple2<Long, Long> checkPhantomRead(Flag flag);

    Tuple2<String, String> checkNonRepeatableRead(Long id, Flag flag);
}
