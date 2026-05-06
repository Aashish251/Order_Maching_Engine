// src/main/java/com/project/ome/shared/dto/ApiError.java
package com.project.ome.shared.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Getter
@Builder
public class ApiError {
    private final String              code;      // machine-readable: ORDER_NOT_FOUND
    private final String              message;   // human-readable
    private final Map<String, String> fields;    // validation errors per field
}