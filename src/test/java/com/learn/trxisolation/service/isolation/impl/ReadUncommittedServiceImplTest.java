package com.learn.trxisolation.service.isolation.impl;

import com.learn.trxisolation.helper.DataPreparation;
import com.learn.trxisolation.model.StudentModel;
import com.learn.trxisolation.service.isolation.ReadUncommittedService;
import com.learn.trxisolation.util.Flag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ReadUncommittedServiceImplTest extends DataPreparation {

    @Autowired
    private ReadUncommittedService isolationService;

    @Test
    void checkDirtyRead() throws InterruptedException, ExecutionException {
        var newGroupName = "New Student Group";
        // initialise flag object
        var flag = new Flag(true);

        var student = studentRepository.findAll().iterator().next();
        Long studentId = student.getId();
        String initialGroup = student.getGroupName();
        // run method which should change the group name
        CompletableFuture<Void> changeGroupFuture = CompletableFuture.runAsync(() -> studentService.changeGroup(studentId, newGroupName, flag, false));
        // wait until changes are flushed to db, but not committed yet
        while (flag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(50);
        }
        String group = isolationService.checkDirtyRead(studentId);
        assertAll(
                () -> assertEquals(newGroupName, group, "Group should be equal as dirty read is possible"),
                () -> assertNotEquals(newGroupName, initialGroup)
        );
        // allow different transaction finish
        flag.setFlagValue(true);
        changeGroupFuture.get();
    }

    @Test
    void testDirtyReadWithException() throws InterruptedException {
        var newGroupName = "New Student Group";
        // initialise flag object
        var flag = new Flag(true);

        var student = studentRepository.findAll().iterator().next();
        Long studentId = student.getId();
        String initialGroup = student.getGroupName();
        // run method which should change the group name
        CompletableFuture<Void> changeGroupFuture = CompletableFuture.runAsync(() -> studentService.changeGroup(studentId, newGroupName, flag, true));
        // wait until changes are flushed to db, but not committed yet
        while (flag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(50);
        }
        String group = isolationService.checkDirtyRead(studentId);
        // check that transaction read uncommitted value
        assertAll(
                () -> assertEquals(newGroupName, group, "Group should be equal as dirty read is possible"),
                () -> assertNotEquals(newGroupName, initialGroup)
        );
        // allow different transaction finish
        flag.setFlagValue(true);
        assertThrows(ExecutionException.class, changeGroupFuture::get);

        // check that new group of student wasn't saved in db
        var actualGroup = studentRepository.findById(studentId).map(StudentModel::getGroupName).orElse(null);
        assertEquals(initialGroup, actualGroup, "Should be the same");
    }

    @Test
    void checkDirtyReadWhenUncommittedTrxDeletedRow() throws InterruptedException {
        // initialise flag object
        var flag = new Flag(true);

        var student = studentRepository.findAll().iterator().next();
        Long studentId = student.getId();

        // run method which should try to delete student, but then throw exception and rollbacks
        CompletableFuture<Void> changeGroupFuture = CompletableFuture.runAsync(() -> studentService.deleteStudent(studentId, flag, true));
        // wait until changes are flushed to db, but not committed yet
        while (flag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(50);
        }
        Long count = isolationService.checkDirtyReadWhenUncommittedTrxDeletedRow();
        // check that transaction read uncommitted value
        assertEquals(1, count, "should be 1 student");

        // allow different transaction finish
        flag.setFlagValue(true);
        assertThrows(ExecutionException.class, changeGroupFuture::get);

        // check that deleted row was restored
        assertEquals(2, studentRepository.count(), "Should be 2 students as transaction rollbacks");
    }

    @Test
    void checkNonRepeatableRead() throws InterruptedException, ExecutionException {
        super.checkNonRepeatableReadPossible(isolationService);
    }

    @Test
    void checkPhantomRead() throws InterruptedException, ExecutionException {
        super.checkPhantomReadPossible(isolationService);
    }
}
