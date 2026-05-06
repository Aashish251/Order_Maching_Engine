// src/main/java/com/project/ome/api/validator/OrderValidator.java
package com.project.ome.api.validator;

import com.project.ome.api.dto.order.PlaceOrderRequest;
import com.project.ome.api.dto.order.OrderType;
import jakarta.validation.*;

public class OrderValidator
        implements ConstraintValidator<ValidOrder, PlaceOrderRequest> {

    @Override
    public boolean isValid(PlaceOrderRequest req,
                           ConstraintValidatorContext ctx) {
        if (req.getType() == OrderType.LIMIT && req.getPrice() == null) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(
                    "Price is required for LIMIT orders")
               .addPropertyNode("price")
               .addConstraintViolation();
            return false;
        }
        return true;
    }
}