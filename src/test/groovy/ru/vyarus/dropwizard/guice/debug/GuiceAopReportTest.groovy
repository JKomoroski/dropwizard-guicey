package ru.vyarus.dropwizard.guice.debug

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.debug.renderer.guice.support.AopModule
import ru.vyarus.dropwizard.guice.test.spock.UseGuiceyApp
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 23.08.2019
 */
@UseGuiceyApp(AIApp)
class GuiceAopReportTest extends Specification {

    def "Check guice aop reporting"() {

        // actual reporting checked manually (test used for reporting configuration)

        expect: "checks that reporting doesn't fail"
        true

    }

    static class AIApp extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .modules(new AopModule())
                    .printGuiceAopMap()
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {

        }
    }
}
