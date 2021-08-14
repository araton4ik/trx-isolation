package com.learn.trxisolation;

import com.learn.trxisolation.model.StudentModel;
import com.learn.trxisolation.repository.StudentRepository;
import com.learn.trxisolation.service.ReadSerializeService;
import com.learn.trxisolation.service.StudentService;
import com.learn.trxisolation.util.Flag;
import io.vavr.Tuple2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SerializeLevelTest {
    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ReadSerializeService isolationService;


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
    void testThatPhantomReadNotPossible() throws InterruptedException, ExecutionException {
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
        long actualCount = studentRepository.count();

        // now when new row was added and committed in db let's allow first transaction read count again
        isolationFlag.setFlagValue(true);
        Tuple2<Long, Long> counts = phantomFuture.get();
        assertAll(
                () -> assertEquals(3, actualCount),
                () -> assertEquals(2, counts._1),
                () -> assertEquals(2, counts._2)
        );
    }


    @Test
    void testThatNonRepeatableReadNotPossible() throws InterruptedException, ExecutionException {
        var student = studentRepository.findAll().iterator().next();
        Long studentId = student.getId();
        String initialGroup = student.getGroupName();
        String newGroup = "NEW";
        // initialise flag object
        var isolationFlag = new Flag(true);

        // run method which should not read non-repeatable data
        CompletableFuture<Tuple2<String, String>> nonRepeatableFuture = CompletableFuture.supplyAsync(() -> isolationService.checkNonRepeatableRead(studentId, isolationFlag));

        // wait until method read student group
        while (isolationFlag.isFlagValue()) {
            TimeUnit.MILLISECONDS.sleep(100);
        }
        // call method in another transaction which change group
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> studentService.changeGroup(studentId, newGroup));
        future.get();
        String actualGroup = studentRepository.findById(studentId).map(StudentModel::getGroupName).orElse(null);

        // now when new row was added and committed in db let's allow first transaction read group again
        isolationFlag.setFlagValue(true);
        Tuple2<String, String> groups = nonRepeatableFuture.get();
        assertAll(
                () -> assertEquals(newGroup, actualGroup),
                () -> assertEquals(initialGroup, groups._1),
                () -> assertEquals(initialGroup, groups._2)
        );

    }
}
