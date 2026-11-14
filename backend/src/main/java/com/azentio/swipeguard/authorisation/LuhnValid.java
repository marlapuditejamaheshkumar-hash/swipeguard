package com.azentio.swipeguard.authorisation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = LuhnValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LuhnValid {
    String message() default "must be a Luhn-valid card number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}