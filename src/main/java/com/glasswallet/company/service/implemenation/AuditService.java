package com.glasswallet.company.service.implemenation;

import com.glasswallet.company.data.model.AuditLog;
import com.glasswallet.company.data.repo.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditRepo;

    public String log(UUID userId, String action, String details, String ip) {
        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .action(action)
                .details(details)
                .ipAddress(ip)
                .createdAt(LocalDateTime.now())
                .build();
        auditRepo.save(log);
        return action;
    }
}
