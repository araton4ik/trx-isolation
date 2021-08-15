package com.learn.trxisolation.helper;

import com.learn.trxisolation.model.StudentModel;
import com.learn.trxisolation.repository.StudentRepository;
import com.learn.trxisolation.service.StudentService;
import com.learn.trxisolation.service.isolation.IsolationService;
import com.learn.trxisolation.util.Flag;
import io.vavr.Tuple2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DataPreparation {

    @Autowired
    protected StudentRepository studentRepository;

    @Autowired
    protected StudentService studentService;

    @BeforeEach
    void setupData() {
        studentRepository.save(new StudentModel(null, "Ivan", "Ivanov", "gr-1", 21));
        studentRepository.save(new StudentModel(null, "Petr", "Petrov", "gr-2", 22));
    }

    @AfterEach
    void clean() {
        studentRepository.deleteAll();
    }

    protected void checkDirtyReadNotPossibleWhenRowIsDeleted(IsolationService isolationService) throws InterruptedException {
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
        // check that transaction  didn't read uncommitted value
        assertEquals(2, count, "should be 2 student");

        // allow different transaction finish
        flag.setFlagValue(true);
        assertThrows(ExecutionException.class, changeGroupFuture::get);

        // check that deleted row was restored
        assertEquals(2, studentRepository.count(), "Should be 2 students as transaction rollbacks");
    }

    protected void checkDirtyReadNotPossible(IsolationService isolationService) throws InterruptedException {
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
        // check that transaction didn't read uncommitted value
        assertEquals(initialGroup, group, "Group should be equal as dirty read is possible");

        // allow different transaction finish
        flag.setFlagValue(true);
        assertThrows(ExecutionException.class, changeGroupFuture::get);

        // check that new group of student wasn't saved in db
        var actualGroup = studentRepository.findById(studentId).map(StudentModel::getGroupName).orElse(null);
        assertEquals(initialGroup, actualGroup, "Should be the same");
    }

    protected void checkPhantomReadPossible(IsolationService isolationService) throws InterruptedException, ExecutionException {
        var newStudent = new StudentModel(null, "John", "Sidorov", "gr-1", 35);
        // initialise flag object
        var isolationFlag = new Flag(true);

        // run method which should readPhantom
        CompletableFuture<Tuple2<Long, Long>> phantomFuture = CompletableFuture.supplyAsync(() -> isolationService.checkPhantomRead(isolationFlag));

        // wait until method read number of students for the first time
        while (isolationFlag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(50);
        }
        // cal method in another transaction which add new row in db
        CompletableFuture.runAsync(() -> studentService.addStudent(newStudent)).get();


        // now when new row was added and committed in db let's allow first transaction read count again
        isolationFlag.setFlagValue(true);
        Tuple2<Long, Long> counts = phantomFuture.get();
        assertAll(
                () -> assertEquals(2, counts._1),
                () -> assertEquals(3, counts._2)
        );
    }

    protected void checkNonRepeatableReadPossible(IsolationService isolationService) throws ExecutionException, InterruptedException {
        var student = studentRepository.findAll().iterator().next();
        Long studentId = student.getId();
        String initialGroup = student.getGroupName();
        String newGroup = "NEW";
        // initialise flag object
        var isolationFlag = new Flag(true);

        // run method which should read non-repeatable data
        CompletableFuture<Tuple2<String, String>> nonRepeatableFuture = CompletableFuture.supplyAsync(() -> isolationService.checkNonRepeatableRead(studentId, isolationFlag));

        // wait until method read student group for the first time
        while (isolationFlag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(50);
        }
        // cal method in another transaction which change group
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> studentService.changeGroup(studentId, newGroup));
        future.get();
        String actualGroup = studentRepository.findById(studentId).map(StudentModel::getGroupName).orElse(null);
        assertEquals(newGroup, actualGroup);


        // now when new row was added and committed in db let's allow first transaction read group again
        isolationFlag.setFlagValue(true);
        Tuple2<String, String> groups = nonRepeatableFuture.get();
        assertAll(
                () -> assertEquals(initialGroup, groups._1),
                () -> assertEquals(newGroup, groups._2)
        );
    }

    protected void checkNonRepeatableReadNotPossible(IsolationService isolationService) throws ExecutionException, InterruptedException {
        var student = studentRepository.findAll().iterator().next();
        Long studentId = student.getId();
        String initialGroup = student.getGroupName();
        String newGroup = "NEW";
        // initialise flag object
        var isolationFlag = new Flag(true);

        // run method which should read non-repeatable data
        CompletableFuture<Tuple2<String, String>> nonRepeatableFuture = CompletableFuture.supplyAsync(() -> isolationService.checkNonRepeatableRead(studentId, isolationFlag));

        // wait until method read student group for the first time
        while (isolationFlag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(50);
        }
        // cal method in another transaction which change group
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> studentService.changeGroup(studentId, newGroup));
        future.get();
        String actualGroup = studentRepository.findById(studentId).map(StudentModel::getGroupName).orElse(null);
        assertEquals(newGroup, actualGroup);


        // now when new row was added and committed in db let's allow first transaction read group again
        isolationFlag.setFlagValue(true);
        Tuple2<String, String> groups = nonRepeatableFuture.get();
        assertAll(
                () -> assertEquals(initialGroup, groups._1),
                () -> assertEquals(initialGroup, groups._2)
        );
    }

}
