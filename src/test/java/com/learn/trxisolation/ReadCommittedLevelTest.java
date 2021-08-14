package com.learn.trxisolation;

import com.learn.trxisolation.model.StudentModel;
import com.learn.trxisolation.repository.StudentRepository;
import com.learn.trxisolation.service.ReadCommittedService;
import com.learn.trxisolation.service.StudentService;
import com.learn.trxisolation.util.Flag;
import io.vavr.Tuple2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReadCommittedLevelTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ReadCommittedService isolationService;


    @BeforeEach
    void setupData() {
        studentRepository.save(new StudentModel(null, "Ivan", "Ivanov", "gr-1", 21));
        studentRepository.save(new StudentModel(null, "Petr", "Petrov", "gr-2", 22));
    }

    @AfterEach
    void clean() {
        studentRepository.deleteAll();
    }

    @Test
    void testThatMethodNotReadNotCommittedData() throws ExecutionException, InterruptedException {
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
            TimeUnit.MILLISECONDS.sleep(100);
        }
        // call method which also want to read the same row
        String group = isolationService.checkDirtyRead(studentId);
        assertEquals(initialGroup, group, "Should be equal as method reads only committed data, when new value is not committed yet");

        // allow different transaction finish
        flag.setFlagValue(true);
        changeGroupFuture.get();
        // check that new group of student was saved in db
        var actualGroup = studentRepository.findById(studentId).map(StudentModel::getGroupName).orElse(null);
        assertEquals(newGroupName, actualGroup, "Should be the same");
    }

    @Test
    void testThatMethodAlsoNotSeeNewUncommittedData() throws InterruptedException, ExecutionException {
        var newStudent = new StudentModel(null, "John", "Sidorov", "ASOI", 35);
        // initialise flag object
        var flag = new Flag(true);
        long initialCount = studentRepository.count();

        // run method which should add new student
        CompletableFuture<StudentModel> changeGroupFuture = CompletableFuture.supplyAsync(() -> studentService.addStudent(newStudent, flag, false));
        // wait until changes are flushed to db, but not committed yet
        while (flag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(100);
        }
        long studentCount = isolationService.readStudentCountWhenRowAddedOrDeleted();
        // check that transaction didn't read new uncommitted row;
        assertEquals(initialCount, studentCount);

        // allow different transaction finish
        flag.setFlagValue(true);
        changeGroupFuture.get();

        // check that new student was saved
        assertEquals(3, studentRepository.count(), "Should be the 3 students");
    }

    @Test
    void testThatPhantomReadStillPossible() throws InterruptedException, ExecutionException {
        var newStudent = new StudentModel(null, "John", "Sidorov", "ASOI", 35);
        // initialise flag object
        var isolationFlag = new Flag(true);

        // run method which should readPhantom
        CompletableFuture<Tuple2<Long, Long>> phantomFuture = CompletableFuture.supplyAsync(() -> isolationService.checkPhantomRead(isolationFlag));

        // wait until method read number of students for the first time
        while (isolationFlag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(100);
        }
        // cal method in another transaction which add new row in db
        studentService.addStudent(newStudent);

        // now when new row was added and committed in db let's allow first transaction read count again
        isolationFlag.setFlagValue(true);
        Tuple2<Long, Long> counts = phantomFuture.get();
        assertAll(
                () -> assertEquals(2, counts._1),
                () -> assertEquals(3, counts._2)
        );
    }


    //@Disabled("It should work, but for some reason doesn't")
    @Test
    void testThatNonRepeatableReadStillPossible() throws InterruptedException, ExecutionException {
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
            TimeUnit.MILLISECONDS.sleep(100);
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
}
