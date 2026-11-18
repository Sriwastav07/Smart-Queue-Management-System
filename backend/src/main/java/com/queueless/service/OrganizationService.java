package com.queueless.service;

import com.queueless.dto.*;
import com.queueless.entity.*;
import com.queueless.repository.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final ServiceRepository serviceRepository;
    private final CounterRepository counterRepository;
    private final QueueRepository queueRepository;
    private final QueueTokenRepository queueTokenRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final QueueEstimationService queueEstimationService;
    private final UserRepository userRepository;

    public List<OrganizationDto> getAllOrganizations(String category, String search) {
        List<Organization> orgs = organizationRepository.findByActiveTrue();

        return orgs.stream()
                .filter(o -> category == null || category.isEmpty() || category.equalsIgnoreCase("ALL") || o.getCategory().equalsIgnoreCase(category))
                .filter(o -> search == null || search.isEmpty() || o.getName().toLowerCase().contains(search.toLowerCase()) || o.getCategory().toLowerCase().contains(search.toLowerCase()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public OrganizationDto getOrganizationById(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        return mapToDto(org);
    }

    public OrganizationDto createOrganization(OrganizationDto dto) {
        Organization org = Organization.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .address(dto.getAddress())
                .city(dto.getCity())
                .contactEmail(dto.getContactEmail())
                .contactPhone(dto.getContactPhone())
                .active(true)
                .build();
        return mapToDto(organizationRepository.save(org));
    }

    public OrganizationDto updateOrganization(Long id, OrganizationDto dto) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        org.setName(dto.getName());
        org.setCategory(dto.getCategory());
        org.setDescription(dto.getDescription());
        org.setAddress(dto.getAddress());
        org.setCity(dto.getCity());
        org.setContactEmail(dto.getContactEmail());
        org.setContactPhone(dto.getContactPhone());

        return mapToDto(organizationRepository.save(org));
    }

    public List<DepartmentDto> getDepartments(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        return departmentRepository.findByOrganizationId(organizationId).stream()
                .map(d -> DepartmentDto.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .description(d.getDescription())
                        .organizationId(org.getId())
                        .organizationName(org.getName())
                        .build())
                .collect(Collectors.toList());
    }

    public DepartmentDto createDepartment(DepartmentDto dto) {
        Organization org = organizationRepository.findById(dto.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        Department d = Department.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .organization(org)
                .build();
        Department saved = departmentRepository.save(d);

        return DepartmentDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .organizationId(org.getId())
                .organizationName(org.getName())
                .build();
    }

    public List<ServiceDto> getServicesByOrganization(Long organizationId) {
        return serviceRepository.findByDepartmentOrganizationIdAndActiveTrue(organizationId).stream()
                .map(this::mapServiceToDto)
                .collect(Collectors.toList());
    }

    public ServiceDto createService(ServiceDto dto) {
        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        com.queueless.entity.Service s = com.queueless.entity.Service.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .prefix(dto.getPrefix())
                .defaultEstimatedDurationMinutes(dto.getDefaultEstimatedDurationMinutes() != null ? dto.getDefaultEstimatedDurationMinutes() : 5)
                .department(dept)
                .active(true)
                .build();

        com.queueless.entity.Service saved = serviceRepository.save(s);
        Queue queue = queueRepository.save(Queue.builder().service(saved).currentSequence(0).build());

        return mapServiceToDto(saved);
    }

    public List<CounterDto> getCountersByOrganization(Long organizationId) {
        return counterRepository.findByDepartmentOrganizationId(organizationId).stream()
                .map(this::mapCounterToDto)
                .collect(Collectors.toList());
    }

    public CounterDto initializeDefaultCounter(Long organizationId) {
        return initializeDefaultCounter(organizationId, null);
    }

    public CounterDto initializeDefaultCounter(Long organizationId, Long userId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        List<Department> depts = departmentRepository.findByOrganizationId(organizationId);
        Department dept;
        if (depts.isEmpty()) {
            dept = departmentRepository.save(Department.builder()
                    .name(org.getCategory() + " Main Department")
                    .description("Default administration and service department")
                    .organization(org)
                    .build());
        } else {
            dept = depts.get(0);
        }

        String catPrefix = org.getCategory() != null && org.getCategory().length() >= 3
                ? org.getCategory().substring(0, 3).toUpperCase()
                : "CNT";

        long count = counterRepository.findByDepartmentOrganizationId(organizationId).size() + 1;

        Counter c = counterRepository.save(Counter.builder()
                .name(org.getCategory() + " Counter " + String.format("%02d", count))
                .counterNumber(catPrefix + "-" + String.format("%02d", count))
                .department(dept)
                .status(CounterStatus.OPEN)
                .build());

        if (userId != null) {
            User staff = userRepository.findById(userId).orElse(null);
            if (staff != null && staff.getRole() == Role.STAFF) {
                staffAssignmentRepository.save(StaffAssignment.builder()
                        .staff(staff)
                        .counter(c)
                        .active(true)
                        .build());
            }
        }

        return mapCounterToDto(c);
    }

    public CounterDto createCounter(CounterDto dto) {
        return createCounter(dto, null);
    }

    @Transactional
    public CounterDto createCounter(CounterDto dto, Long userId) {
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        Department dept = null;
        if (dto.getDepartmentId() != null) {
            dept = departmentRepository.findById(dto.getDepartmentId()).orElse(null);
        }

        Long targetOrgId = null;
        if (dto.getOrganizationId() != null) {
            targetOrgId = dto.getOrganizationId();
        } else if (user != null && user.getOrganization() != null) {
            targetOrgId = user.getOrganization().getId();
        }

        if (dept == null && targetOrgId != null) {
            List<Department> depts = departmentRepository.findByOrganizationId(targetOrgId);
            if (!depts.isEmpty()) {
                dept = depts.get(0);
            } else {
                Organization org = organizationRepository.findById(targetOrgId).orElse(null);
                if (org != null) {
                    dept = departmentRepository.save(Department.builder()
                            .name(org.getCategory() + " Main Department")
                            .description("Default service department")
                            .organization(org)
                            .build());
                }
            }
        }

        if (dept == null) {
            List<Department> allDepts = departmentRepository.findAll();
            if (!allDepts.isEmpty()) {
                dept = allDepts.get(0);
            }
        }

        if (dept == null) {
            throw new IllegalArgumentException("Department not found for creating counter");
        }

        if (user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF)) {
            if (user.getOrganization() != null && !dept.getOrganization().getId().equals(user.getOrganization().getId())) {
                throw new IllegalArgumentException("Unauthorized: Cannot create counter for another organization");
            }
        }

        String counterNumber = dto.getCounterNumber();
        if (counterNumber == null || counterNumber.isBlank()) {
            long count = counterRepository.findByDepartmentOrganizationId(dept.getOrganization().getId()).size() + 1;
            String catPrefix = dept.getOrganization().getCategory() != null && dept.getOrganization().getCategory().length() >= 3
                    ? dept.getOrganization().getCategory().substring(0, 3).toUpperCase()
                    : "CNT";
            counterNumber = catPrefix + "-" + String.format("%02d", count);
        }

        Counter c = Counter.builder()
                .name(dto.getName())
                .counterNumber(counterNumber)
                .department(dept)
                .status(dto.getStatus() != null ? dto.getStatus() : CounterStatus.OPEN)
                .build();

        return mapCounterToDto(counterRepository.save(c));
    }

    @Transactional
    public void deleteCounter(Long counterId, Long userId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new IllegalArgumentException("Counter not found"));

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF)) {
                if (user.getOrganization() != null && !counter.getDepartment().getOrganization().getId().equals(user.getOrganization().getId())) {
                    throw new IllegalArgumentException("Unauthorized: Counter does not belong to your organization");
                }
            }
        }

        // Unlink tokens referencing this counter
        List<QueueToken> tokens = queueTokenRepository.findByCounterId(counterId);
        for (QueueToken t : tokens) {
            t.setCounter(null);
            queueTokenRepository.save(t);
        }

        // Remove staff assignments for this counter
        List<StaffAssignment> assignments = staffAssignmentRepository.findByCounterId(counterId);
        staffAssignmentRepository.deleteAll(assignments);

        counterRepository.delete(counter);
    }

    public CounterDto updateCounterStatus(Long counterId, CounterStatus status, Long userId) {
        Counter c = counterRepository.findById(counterId)
                .orElseThrow(() -> new IllegalArgumentException("Counter not found"));

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && (user.getRole() == Role.STAFF || user.getRole() == Role.ADMIN)) {
                if (user.getOrganization() != null && !c.getDepartment().getOrganization().getId().equals(user.getOrganization().getId())) {
                    throw new IllegalArgumentException("Unauthorized: Counter does not belong to your organization");
                }
            }
        }

        c.setStatus(status);
        return mapCounterToDto(counterRepository.save(c));
    }

    public AdminAnalyticsDto getAdminAnalytics(Long organizationId, Long userId) {
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getRole() == Role.ADMIN && user.getOrganization() != null) {
                organizationId = user.getOrganization().getId();
            }
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        long totalCustomersToday = queueTokenRepository.countByOrganizationAndCreatedAtAfter(organizationId, startOfDay);
        long currentlyWaiting = queueTokenRepository.countByOrganizationAndStatusAndCreatedAtAfter(organizationId, TokenStatus.WAITING, startOfDay);
        long completedToday = queueTokenRepository.countByOrganizationAndStatusAndCreatedAtAfter(organizationId, TokenStatus.COMPLETED, startOfDay);
        long noShowsToday = queueTokenRepository.countByOrganizationAndStatusAndCreatedAtAfter(organizationId, TokenStatus.NO_SHOW, startOfDay);
        long cancelledToday = queueTokenRepository.countByOrganizationAndStatusAndCreatedAtAfter(organizationId, TokenStatus.CANCELLED, startOfDay);

        long activeCounters = counterRepository.countByDepartmentOrganizationIdAndStatus(organizationId, CounterStatus.OPEN);
        List<Counter> allCounters = counterRepository.findByDepartmentOrganizationId(organizationId);

        List<AdminAnalyticsDto.HourlyQueueStat> hourlyStats = new ArrayList<>();
        for (int h = 8; h <= 18; h++) {
            String label = String.format("%02d:00", h);
            long joined = Math.max(0, (totalCustomersToday / 10) + (h % 3) * 5);
            long served = Math.max(0, (completedToday / 10) + (h % 2) * 4);
            hourlyStats.add(new AdminAnalyticsDto.HourlyQueueStat(label, joined, served));
        }

        List<com.queueless.entity.Service> services = serviceRepository.findByDepartmentOrganizationIdAndActiveTrue(organizationId);
        List<AdminAnalyticsDto.ServiceDemandStat> demandStats = services.stream().map(s -> {
            long count = queueTokenRepository.findByQueueIdAndStatusInOrderBySequenceNumberAsc(
                    queueRepository.findByServiceId(s.getId()).map(Queue::getId).orElse(0L),
                    List.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SERVING, TokenStatus.COMPLETED)
            ).size();
            return new AdminAnalyticsDto.ServiceDemandStat(s.getName(), count, s.getDefaultEstimatedDurationMinutes());
        }).collect(Collectors.toList());

        List<AdminAnalyticsDto.CounterPerformanceStat> counterPerf = allCounters.stream().map(c -> {
            var assignment = staffAssignmentRepository.findByCounterIdAndActiveTrue(c.getId());
            String staffName = assignment.map(a -> a.getStaff().getFullName()).orElse("Unassigned");
            long served = queueTokenRepository.findByCounterIdAndStatus(c.getId(), TokenStatus.COMPLETED).size();
            return new AdminAnalyticsDto.CounterPerformanceStat(c.getName(), staffName, served, 240);
        }).collect(Collectors.toList());

        return AdminAnalyticsDto.builder()
                .organizationId(org.getId())
                .organizationName(org.getName())
                .totalCustomersToday(totalCustomersToday)
                .currentlyWaiting(currentlyWaiting)
                .averageWaitMinutes(15)
                .completedToday(completedToday)
                .noShowsToday(noShowsToday)
                .cancelledToday(cancelledToday)
                .activeCountersCount(activeCounters)
                .totalCountersCount((long) allCounters.size())
                .hourlyQueueStats(hourlyStats)
                .serviceWiseDemand(demandStats)
                .counterPerformance(counterPerf)
                .build();
    }

    public OrganizationDto mapToDto(Organization org) {
        List<Department> depts = departmentRepository.findByOrganizationId(org.getId());
        int totalWaiting = 0;
        List<com.queueless.entity.Service> services = serviceRepository.findByDepartmentOrganizationIdAndActiveTrue(org.getId());
        for (var s : services) {
            var queueOpt = queueRepository.findByServiceId(s.getId());
            if (queueOpt.isPresent()) {
                totalWaiting += queueTokenRepository.countByQueueIdAndStatus(queueOpt.get().getId(), TokenStatus.WAITING);
            }
        }

        return OrganizationDto.builder()
                .id(org.getId())
                .name(org.getName())
                .category(org.getCategory())
                .description(org.getDescription())
                .address(org.getAddress())
                .city(org.getCity())
                .contactEmail(org.getContactEmail())
                .contactPhone(org.getContactPhone())
                .active(org.getActive())
                .totalDepartmentsCount(depts.size())
                .activeQueueCount(totalWaiting)
                .estimatedWaitMinutes(totalWaiting * 3)
                .createdAt(org.getCreatedAt())
                .build();
    }

    public ServiceDto mapServiceToDto(com.queueless.entity.Service service) {
        var queueOpt = queueRepository.findByServiceId(service.getId());
        long waitingCount = 0;
        int estWait = service.getDefaultEstimatedDurationMinutes();

        if (queueOpt.isPresent()) {
            waitingCount = queueTokenRepository.countByQueueIdAndStatus(queueOpt.get().getId(), TokenStatus.WAITING);
            estWait = queueEstimationService.calculateEstimatedWaitTimeMinutes(queueOpt.get(), null);
        }

        return ServiceDto.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .prefix(service.getPrefix())
                .defaultEstimatedDurationMinutes(service.getDefaultEstimatedDurationMinutes())
                .departmentId(service.getDepartment().getId())
                .departmentName(service.getDepartment().getName())
                .organizationId(service.getDepartment().getOrganization().getId())
                .organizationName(service.getDepartment().getOrganization().getName())
                .active(service.getActive())
                .queueId(queueOpt.map(Queue::getId).orElse(null))
                .currentWaitingCount((int) waitingCount)
                .estimatedWaitMinutes(estWait)
                .build();
    }

    public CounterDto mapCounterToDto(Counter counter) {
        var assignment = staffAssignmentRepository.findByCounterIdAndActiveTrue(counter.getId());
        var currentTokenOpt = queueTokenRepository.findFirstByCounterIdAndStatusOrderByCalledAtDesc(counter.getId(), TokenStatus.CALLED);
        if (currentTokenOpt.isEmpty()) {
            currentTokenOpt = queueTokenRepository.findFirstByCounterIdAndStatusOrderByCalledAtDesc(counter.getId(), TokenStatus.SERVING);
        }

        return CounterDto.builder()
                .id(counter.getId())
                .name(counter.getName())
                .counterNumber(counter.getCounterNumber())
                .departmentId(counter.getDepartment() != null ? counter.getDepartment().getId() : null)
                .departmentName(counter.getDepartment() != null ? counter.getDepartment().getName() : null)
                .organizationId(counter.getDepartment() != null && counter.getDepartment().getOrganization() != null ? counter.getDepartment().getOrganization().getId() : null)
                .status(counter.getStatus())
                .assignedStaffId(assignment.map(a -> a.getStaff().getId()).orElse(null))
                .assignedStaffName(assignment.map(a -> a.getStaff().getFullName()).orElse(null))
                .currentToken(currentTokenOpt.map(t -> QueueTokenDto.builder()
                        .id(t.getId())
                        .tokenNumber(t.getTokenNumber())
                        .status(t.getStatus())
                        .userName(t.getUser().getFullName())
                        .build()).orElse(null))
                .build();
    }
}
