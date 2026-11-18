package com.queueless.config;

import com.queueless.entity.*;
import com.queueless.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final ServiceRepository serviceRepository;
    private final CounterRepository counterRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final QueueRepository queueRepository;
    private final QueueTokenRepository queueTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Checking and seeding database data...");
        if (userRepository.count() > 0 || organizationRepository.count() > 0) {
            log.info("Database already contains data. Skipping seeding.");
            return;
        }

        // 1. Organizations
        Organization collegeOrg = organizationRepository.save(Organization.builder()
                .name("Lovely Professional University Administration")
                .category("College")
                .description("Central student administration cell for academic certificates, fees, and examination services.")
                .address("Jalandhar - Delhi G.T. Road, Phagwara")
                .city("Punjab")
                .contactEmail("admin@lpu.co.in")
                .contactPhone("+911824404400")
                .active(true)
                .build());

        Organization bankOrg = organizationRepository.save(Organization.builder()
                .name("State Bank of India - Main Branch")
                .category("Bank")
                .description("Retail banking, deposit/withdrawal counters, loan application desk, and forex services.")
                .address("Connaught Place, Sector 17")
                .city("Chandigarh")
                .contactEmail("help@sbi.co.in")
                .contactPhone("+911722754321")
                .active(true)
                .build());

        Organization hospitalOrg = organizationRepository.save(Organization.builder()
                .name("City Care Super Specialty Hospital")
                .category("Hospital")
                .description("OPD consultations, diagnostic lab services, billing counter, and pharmacy queue.")
                .address("MG Road, Healthcare Complex")
                .city("New Delhi")
                .contactEmail("reception@citycare.org")
                .contactPhone("+911145678900")
                .active(true)
                .build());

        // 2. Users
        String encodedPassword = passwordEncoder.encode("password123");

        User user = userRepository.save(User.builder()
                .email("user@queueless.com")
                .password(encodedPassword)
                .fullName("Anisha Sharma")
                .phoneNumber("+919876543210")
                .role(Role.USER)
                .build());

        User collegeStaff = userRepository.save(User.builder()
                .email("staff@queueless.com")
                .password(encodedPassword)
                .fullName("Rajesh Kumar (College Staff)")
                .phoneNumber("+919876543211")
                .role(Role.STAFF)
                .organization(collegeOrg)
                .build());

        User bankStaff = userRepository.save(User.builder()
                .email("bank.staff@queueless.com")
                .password(encodedPassword)
                .fullName("Suresh Verma (Bank Staff)")
                .phoneNumber("+919876543222")
                .role(Role.STAFF)
                .organization(bankOrg)
                .build());

        User hospitalStaff = userRepository.save(User.builder()
                .email("hospital.staff@queueless.com")
                .password(encodedPassword)
                .fullName("Dr. Ananya Roy (Hospital Staff)")
                .phoneNumber("+919876543333")
                .role(Role.STAFF)
                .organization(hospitalOrg)
                .build());

        User admin = userRepository.save(User.builder()
                .email("admin@queueless.com")
                .password(encodedPassword)
                .fullName("Dr. Vikram Malhotra")
                .phoneNumber("+919876543212")
                .role(Role.ADMIN)
                .organization(collegeOrg)
                .build());

        // 3. College Departments, Services & Counters
        Department studentServices = departmentRepository.save(Department.builder()
                .name("Student Services")
                .description("Issuance of bonafide certificates, transcripts, and ID cards")
                .organization(collegeOrg)
                .build());

        Department examCell = departmentRepository.save(Department.builder()
                .name("Examination Cell")
                .description("Exam forms, degree distribution, and migration certificates")
                .organization(collegeOrg)
                .build());

        com.queueless.entity.Service bonafideService = serviceRepository.save(com.queueless.entity.Service.builder()
                .name("Bonafide Certificate")
                .description("Official student bonafide certificate")
                .prefix("A")
                .defaultEstimatedDurationMinutes(4)
                .department(studentServices)
                .active(true)
                .build());

        com.queueless.entity.Service migrationService = serviceRepository.save(com.queueless.entity.Service.builder()
                .name("Migration Certificate")
                .description("Official university migration certificate")
                .prefix("B")
                .defaultEstimatedDurationMinutes(6)
                .department(examCell)
                .active(true)
                .build());

        Queue queueCollegeA = queueRepository.save(Queue.builder().service(bonafideService).currentSequence(15).build());
        Queue queueCollegeB = queueRepository.save(Queue.builder().service(migrationService).currentSequence(8).build());

        Counter counterCollege01 = counterRepository.save(Counter.builder()
                .name("College Desk 01")
                .counterNumber("C-01")
                .department(studentServices)
                .status(CounterStatus.OPEN)
                .build());

        Counter counterCollege02 = counterRepository.save(Counter.builder()
                .name("College Desk 02")
                .counterNumber("C-02")
                .department(examCell)
                .status(CounterStatus.OPEN)
                .build());

        staffAssignmentRepository.save(StaffAssignment.builder()
                .staff(collegeStaff)
                .counter(counterCollege01)
                .active(true)
                .build());

        // 4. Bank Departments, Services & Counters
        Department cashDept = departmentRepository.save(Department.builder()
                .name("Cash & Deposits")
                .description("Cash deposits, check clearance, and withdrawals")
                .organization(bankOrg)
                .build());

        Department loanDept = departmentRepository.save(Department.builder()
                .name("Loans & Mortgages")
                .description("Home loans, personal loans, and credit inquiries")
                .organization(bankOrg)
                .build());

        com.queueless.entity.Service cashService = serviceRepository.save(com.queueless.entity.Service.builder()
                .name("Cash Deposit & Withdrawal")
                .description("Over-the-counter cash deposits and withdrawals")
                .prefix("BK")
                .defaultEstimatedDurationMinutes(3)
                .department(cashDept)
                .active(true)
                .build());

        com.queueless.entity.Service loanService = serviceRepository.save(com.queueless.entity.Service.builder()
                .name("Personal Loan Consultation")
                .description("New loan application process and document verification")
                .prefix("LN")
                .defaultEstimatedDurationMinutes(10)
                .department(loanDept)
                .active(true)
                .build());

        Queue queueBankCash = queueRepository.save(Queue.builder().service(cashService).currentSequence(10).build());
        Queue queueBankLoan = queueRepository.save(Queue.builder().service(loanService).currentSequence(4).build());

        Counter counterBank01 = counterRepository.save(Counter.builder()
                .name("Bank Teller 01")
                .counterNumber("B-01")
                .department(cashDept)
                .status(CounterStatus.OPEN)
                .build());

        Counter counterBank02 = counterRepository.save(Counter.builder()
                .name("Loan Desk 02")
                .counterNumber("B-02")
                .department(loanDept)
                .status(CounterStatus.OPEN)
                .build());

        staffAssignmentRepository.save(StaffAssignment.builder()
                .staff(bankStaff)
                .counter(counterBank01)
                .active(true)
                .build());

        // 5. Hospital Departments, Services & Counters
        Department opdDept = departmentRepository.save(Department.builder()
                .name("OPD Registration")
                .description("Outpatient Registration and Doctor Slip Desk")
                .organization(hospitalOrg)
                .build());

        com.queueless.entity.Service opdService = serviceRepository.save(com.queueless.entity.Service.builder()
                .name("OPD Ticket & Consultation")
                .description("OPD slip generation and specialist appointment")
                .prefix("MED")
                .defaultEstimatedDurationMinutes(5)
                .department(opdDept)
                .active(true)
                .build());

        Queue queueHospitalOpd = queueRepository.save(Queue.builder().service(opdService).currentSequence(7).build());

        Counter counterHospital01 = counterRepository.save(Counter.builder()
                .name("Hospital OPD Counter 01")
                .counterNumber("H-01")
                .department(opdDept)
                .status(CounterStatus.OPEN)
                .build());

        staffAssignmentRepository.save(StaffAssignment.builder()
                .staff(hospitalStaff)
                .counter(counterHospital01)
                .active(true)
                .build());

        // 6. Sample Tokens for analytics
        for (int i = 1; i <= 10; i++) {
            queueTokenRepository.save(QueueToken.builder()
                    .tokenNumber("A-" + String.format("%03d", i))
                    .sequenceNumber(i)
                    .queue(queueCollegeA)
                    .user(user)
                    .status(TokenStatus.COMPLETED)
                    .counter(counterCollege01)
                    .calledAt(LocalDateTime.now().minusMinutes(60 - i * 5))
                    .serviceStartedAt(LocalDateTime.now().minusMinutes(58 - i * 5))
                    .completedAt(LocalDateTime.now().minusMinutes(54 - i * 5))
                    .actualServiceDurationSeconds(240)
                    .build());
        }

        // Waiting token for College
        queueTokenRepository.save(QueueToken.builder()
                .tokenNumber("A-011")
                .sequenceNumber(11)
                .queue(queueCollegeA)
                .user(user)
                .status(TokenStatus.WAITING)
                .notifyThreshold(10)
                .notificationSent(false)
                .build());

        // Waiting token for Bank
        queueTokenRepository.save(QueueToken.builder()
                .tokenNumber("BK-001")
                .sequenceNumber(1)
                .queue(queueBankCash)
                .user(user)
                .status(TokenStatus.WAITING)
                .notifyThreshold(5)
                .notificationSent(false)
                .build());

        log.info("DataSeeder completed successfully.");
    }
}
