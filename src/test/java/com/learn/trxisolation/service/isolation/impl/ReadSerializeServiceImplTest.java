package com.learn.trxisolation.service.isolation.impl;

import com.learn.trxisolation.helper.DataPreparation;
import com.learn.trxisolation.model.StudentModel;
import com.learn.trxisolation.service.isolation.ReadSerializeService;
import com.learn.trxisolation.util.Flag;
import io.vavr.Tuple2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ReadSerializeServiceImplTest extends DataPreparation {

    @Autowired
    private ReadSerializeService isolationService;

    @Test
    void checkDirtyRead() throws InterruptedException {
        this.checkDirtyReadNotPossible(isolationService);
    }

    @Test
    void checkDirtyReadWhenUncommittedTrxDeletedRow() throws InterruptedException {
        this.checkDirtyReadNotPossibleWhenRowIsDeleted(isolationService);
    }

    @Test
    void checkNonRepeatableRead() throws ExecutionException, InterruptedException {
        super.checkNonRepeatableReadNotPossible(isolationService);
    }

    @Test
    void checkPhantomRead() throws InterruptedException, ExecutionException {
        var newStudent = new StudentModel(null, "John", "Sidorov", "ASOI", 35);
        // initialise flag object
        var isolationFlag = new Flag(true);

        // run method which should readPhantom
        CompletableFuture<Tuple2<Long, Long>> phantomFuture = CompletableFuture.supplyAsync(() -> isolationService.checkPhantomRead(isolationFlag));

        // wait until method read number of students for the first time
        while (isolationFlag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(50);
        }
        // cal method in another transaction which add new row in db
        studentService.addStudent(newStudent);

        // now when new row was added and committed in db let's allow first transaction read count again
        isolationFlag.setFlagValue(true);
        Tuple2<Long, Long> counts = phantomFuture.get();
        assertAll(
                () -> assertEquals(2, counts._1),
                () -> assertEquals(2, counts._2),
                () -> assertEquals(3, studentRepository.count())
        );
    }
}
