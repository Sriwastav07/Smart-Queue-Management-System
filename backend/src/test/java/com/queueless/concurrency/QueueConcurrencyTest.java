package com.queueless.concurrency;

import com.queueless.dto.QueueTokenDto;
import com.queueless.entity.*;
import com.queueless.repository.*;
import com.queueless.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
public class QueueConcurrencyTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private QueueTokenRepository queueTokenRepository;

    private Long serviceId;
    private List<User> testUsers = new ArrayList<>();

    @BeforeEach
    void setup() {
        queueTokenRepository.deleteAll();
        queueRepository.deleteAll();
        serviceRepository.deleteAll();
        departmentRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        Organization org = organizationRepository.save(Organization.builder().name("Test Org").active(true).build());
        Department dept = departmentRepository.save(Department.builder().name("Test Dept").organization(org).build());
        com.queueless.entity.Service service = serviceRepository.save(
                com.queueless.entity.Service.builder().name("Test Service").prefix("T").defaultEstimatedDurationMinutes(5).department(dept).active(true).build()
        );
        serviceId = service.getId();

        queueRepository.save(com.queueless.entity.Queue.builder().service(service).currentSequence(0).build());

        for (int i = 1; i <= 20; i++) {
            User u = userRepository.save(User.builder()
                    .email("user" + i + "@test.com")
                    .password("password")
                    .fullName("User " + i)
                    .role(Role.USER)
                    .build());
            testUsers.add(u);
        }
    }

    @Test
    @DisplayName("Verify 20 simultaneous token requests produce unique sequential token numbers")
    void testSimultaneousTokenRequests() throws InterruptedException, ExecutionException {
        int numberOfThreads = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);

        List<Future<QueueTokenDto>> futures = new ArrayList<>();

        for (User user : testUsers) {
            Callable<QueueTokenDto> task = () -> {
                latch.await(); // Wait for all threads to be ready
                return queueService.issueToken(user.getId(), serviceId, 5);
            };
            futures.add(executorService.submit(task));
        }

        latch.countDown(); // Release threads simultaneously

        Set<String> tokenNumbers = ConcurrentHashMap.newKeySet();
        Set<Integer> sequenceNumbers = ConcurrentHashMap.newKeySet();

        for (Future<QueueTokenDto> future : futures) {
            QueueTokenDto dto = future.get();
            assertNotNull(dto);
            tokenNumbers.add(dto.getTokenNumber());
            sequenceNumbers.add(dto.getSequenceNumber());
        }

        executorService.shutdown();

        assertEquals(20, tokenNumbers.size(), "20 simultaneous requests must generate 20 unique token numbers!");
        assertEquals(20, sequenceNumbers.size(), "20 simultaneous requests must generate 20 unique sequence numbers!");

        for (int i = 1; i <= 20; i++) {
            assertTrue(sequenceNumbers.contains(i), "Sequence should contain number " + i);
        }
    }
}
