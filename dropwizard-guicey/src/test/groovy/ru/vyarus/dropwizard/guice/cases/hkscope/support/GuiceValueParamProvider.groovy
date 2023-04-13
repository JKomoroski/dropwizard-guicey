package ru.vyarus.dropwizard.guice.cases.hkscope.support

import org.glassfish.jersey.server.ContainerRequest
import org.glassfish.jersey.server.model.Parameter
import org.glassfish.jersey.server.spi.internal.ValueParamProvider
import ru.vyarus.dropwizard.guice.module.installer.feature.jersey.GuiceManaged

import jakarta.ws.rs.ext.Provider
import java.util.function.Function

/**
 * @author Vyacheslav Rusakov
 * @since 19.01.2016
 */
@Provider
@GuiceManaged
class GuiceValueParamProvider implements ValueParamProvider {


    @Override
    Function<ContainerRequest, ?> getValueProvider(Parameter parameter) {
        return null
    }

    @Override
    PriorityType getPriority() {
        return Priority.LOW
    }
}
