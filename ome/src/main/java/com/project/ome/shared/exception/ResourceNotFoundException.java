// src/main/java/com/project/ome/shared/exception/ResourceNotFoundException.java
package com.project.ome.shared.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String id) {
        super(resource + " not found with id: " + id);
    }
}