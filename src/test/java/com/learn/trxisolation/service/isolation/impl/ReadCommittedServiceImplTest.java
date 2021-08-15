package com.learn.trxisolation.service.isolation.impl;

import com.learn.trxisolation.helper.DataPreparation;
import com.learn.trxisolation.service.isolation.ReadCommittedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutionException;

class ReadCommittedServiceImplTest extends DataPreparation {

    @Autowired
    private ReadCommittedService isolationService;

    @Test
    void checkDirtyRead() throws InterruptedException {
        super.checkDirtyReadNotPossible(isolationService);
    }

    @Test
    void checkDirtyReadWhenUncommittedTrxDeletedRow() throws InterruptedException {
        super.checkDirtyReadNotPossibleWhenRowIsDeleted(isolationService);
    }

    @Test
    void checkNonRepeatableRead() throws ExecutionException, InterruptedException {
        super.checkNonRepeatableReadPossible(isolationService);
    }

    @Test
    void checkPhantomRead() throws InterruptedException, ExecutionException {
        super.checkPhantomReadPossible(isolationService);
    }
}
