package ru.vyarus.dropwizard.guice.test.jupiter.param;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for junit 5 parameter injection. Specifies application port parameter.
 * Target parameter must be of type {@link int}. Works only with
 * {@link ru.vyarus.dropwizard.guice.test.jupiter.TestDropwizardApp} extension.
 * <p>
 * Example usage:
 * <pre>{@code
 * @BeforeAll
 * static before(@AppPort int port) {
 *      baseUrl = "http://localhost:" + port + "/";
 * }
 * }</pre>
 * <p>
 * Actual value obtained from {@link io.dropwizard.testing.DropwizardTestSupport#getLocalPort()}
 *
 * @author Vyacheslav Rusakov
 * @since 29.04.2020
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AppPort {
}
