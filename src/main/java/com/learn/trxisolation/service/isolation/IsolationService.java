package com.learn.trxisolation.service.isolation;

import com.learn.trxisolation.util.Flag;
import io.vavr.Tuple2;

public interface IsolationService {

    String checkDirtyRead(Long studentId);

    long checkDirtyReadWhenUncommittedTrxDeletedRow();

    Tuple2<String, String> checkNonRepeatableRead(Long id, Flag flag);

    Tuple2<Long, Long> checkPhantomRead(Flag flag);
}
