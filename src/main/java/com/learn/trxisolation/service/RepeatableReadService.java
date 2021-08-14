package com.learn.trxisolation.service;

import com.learn.trxisolation.util.Flag;
import io.vavr.Tuple2;

public interface RepeatableReadService {

    Tuple2<String, String> checkNonRepeatableRead(Long id, Flag flag);

    Tuple2<Long, Long> checkPhantomRead(Flag flag);
}
