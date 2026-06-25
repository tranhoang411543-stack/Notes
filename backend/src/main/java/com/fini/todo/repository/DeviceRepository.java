package com.fini.todo.repository;

import com.fini.todo.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    Optional<Device> findByIdAndUserId(UUID id, UUID userId);
}
