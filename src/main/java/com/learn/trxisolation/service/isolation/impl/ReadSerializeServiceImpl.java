package com.learn.trxisolation.service.isolation.impl;

import com.learn.trxisolation.service.isolation.ReadSerializeService;
import com.learn.trxisolation.service.isolation.IsolationService;
import com.learn.trxisolation.util.Flag;
import io.vavr.Tuple2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ReadSerializeServiceImpl implements ReadSerializeService {
    private final IsolationService isolationService;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public String checkDirtyRead(Long studentId) {
        return isolationService.checkDirtyRead(studentId);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public long checkDirtyReadWhenUncommittedTrxDeletedRow() {
        return isolationService.checkDirtyReadWhenUncommittedTrxDeletedRow();
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Tuple2<String, String> checkNonRepeatableRead(Long id, Flag flag) {
        return isolationService.checkNonRepeatableRead(id, flag);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Tuple2<Long, Long> checkPhantomRead(Flag flag) {
        return isolationService.checkPhantomRead(flag);
    }
}
