// src/main/java/com/project/ome/api/validator/ValidOrderConstraint.java
package com.project.ome.api.validator;

import com.project.ome.api.dto.order.PlaceOrderRequest;
import com.project.ome.api.dto.order.OrderType;
import jakarta.validation.*;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = OrderValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrder {
    String message() default "Invalid order configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}