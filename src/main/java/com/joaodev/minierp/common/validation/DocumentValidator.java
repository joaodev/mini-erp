package com.joaodev.minierp.common.validation;

import com.joaodev.minierp.common.util.DocumentUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class DocumentValidator implements ConstraintValidator<ValidDocument, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        return DocumentUtils.isValid(value);
    }
}
