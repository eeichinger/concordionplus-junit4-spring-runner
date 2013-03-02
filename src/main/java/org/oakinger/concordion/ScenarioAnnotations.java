package org.oakinger.concordion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a method in your specification class using @ScenarioAnnotations to apply common test annotations
 * to either a specific or all scenarios
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ScenarioAnnotations {

    String name() default "";
}
