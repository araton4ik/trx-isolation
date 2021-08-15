package com.learn.trxisolation.service.isolation.impl;

import com.learn.trxisolation.helper.DataPreparation;
import com.learn.trxisolation.service.isolation.RepeatableReadService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutionException;

class RepeatableReadServiceImplTest extends DataPreparation {

    @Autowired
    private RepeatableReadService isolationService;

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
        super.checkNonRepeatableReadNotPossible(isolationService);
    }

    @Test
    @Disabled("I don't understand why it doesn't work :(")
    void checkPhantomRead() throws InterruptedException, ExecutionException {
        super.checkPhantomReadPossible(isolationService);
    }
}
