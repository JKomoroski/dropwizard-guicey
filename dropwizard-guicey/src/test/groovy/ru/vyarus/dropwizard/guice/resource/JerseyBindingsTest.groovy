package ru.vyarus.dropwizard.guice.resource

import com.google.inject.Injector
import io.dropwizard.core.Application
import io.dropwizard.core.Configuration
import io.dropwizard.core.setup.Bootstrap
import io.dropwizard.core.setup.Environment
import org.glassfish.jersey.server.AsyncContext
import org.glassfish.jersey.server.ContainerRequest
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider
import ru.vyarus.dropwizard.guice.AbstractTest
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.test.jupiter.TestDropwizardApp

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.container.ResourceInfo
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Request
import jakarta.ws.rs.core.SecurityContext
import jakarta.ws.rs.core.UriInfo
import jakarta.ws.rs.ext.Providers

/**
 * @author Vyacheslav Rusakov
 * @since 29.03.2017
 */
@TestDropwizardApp(App)
class JerseyBindingsTest extends AbstractTest {

    def "Check jersey bindings"() {

        expect: "bindings ok inside request"
        new URL("http://localhost:8080/sample/").getText() == 'ok'
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .extensions(SampleResource)
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }

    @Path("/sample")
    static class SampleResource {
        @Inject
        Injector injector

        @GET
        String request() {
            [MultivaluedParameterExtractorProvider,
             jakarta.ws.rs.core.Application,
             Providers,
             UriInfo,
             ResourceInfo,
             HttpHeaders,
             SecurityContext,
             Request,
             ContainerRequest,
             AsyncContext].each {
                println it
                assert injector.getInstance(it) != null
            }

            return "ok"
        }
    }
}