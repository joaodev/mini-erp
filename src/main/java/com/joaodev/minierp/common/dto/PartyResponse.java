package com.joaodev.minierp.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public abstract class PartyResponse {
    private UUID id;
    private String name;
    private String document;
    private String email;
    private String phone;
    private boolean active;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
