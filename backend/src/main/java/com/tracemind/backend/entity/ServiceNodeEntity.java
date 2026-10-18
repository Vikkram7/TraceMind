package com.tracemind.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "services")
public class ServiceNodeEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @Column(length = 64)
    private String environment = "local";

    @Column(length = 32)
    private String version = "1.0.0";

    @Column(length = 32)
    private String status = "HEALTHY"; // HEALTHY, DEGRADED, CRITICAL

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat = Instant.now();

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public ServiceNodeEntity() {}

    public ServiceNodeEntity(String id, String name) {
        this.id = id;
        this.name = name;
        this.lastHeartbeat = Instant.now();
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
