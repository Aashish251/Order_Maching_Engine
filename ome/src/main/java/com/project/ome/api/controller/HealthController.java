// src/main/java/com/project/ome/api/controller/HealthController.java
package com.project.ome.api.controller;

import com.project.ome.shared.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.ok(Map.of(
            "status",  "UP",
            "service", "Order Matching Engine",
            "version", "1.0.0"
        ));
    }
}