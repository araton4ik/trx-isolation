package com.learn.trxisolation.service;

import com.learn.trxisolation.util.Flag;
import io.vavr.Tuple2;

public interface IsolationService {

    String readStudentGroupWithReadUncommittedLevel(Long studentId);

    long readStudentCountWhenRowDeletedWithUncommittedLevel();

    String readStudentGroupWithReadCommittedLevel(Long studentId);

    long readStudentCountWhenRowAddedOrDeletedWithReadCommittedLevel();

    Tuple2<Long, Long> checkPhantomReadWhenLevelIsReadCommitted(Flag flag);

    Tuple2<String, String> checkNonRepeatableReadWhenLevelIsReadCommitted(Long id, Flag flag);

    Tuple2<String, String> checkNonRepeatableReadWhenLevelIsReadRepeatable(Long id, Flag flag);

    Tuple2<Long, Long> checkPhantomReadWhenLevelIsReadRepeatable(Flag flag);

    Tuple2<String, String> checkNonRepeatableReadWhenLevelIsSerialize(Long id, Flag flag);

    Tuple2<Long, Long> checkPhantomReadWhenLevelIsSerialize(Flag flag);

}
