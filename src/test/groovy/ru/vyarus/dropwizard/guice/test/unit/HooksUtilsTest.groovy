package ru.vyarus.dropwizard.guice.test.unit

import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.hook.ConfigurationHooksSupport
import ru.vyarus.dropwizard.guice.hook.GuiceyConfigurationHook
import ru.vyarus.dropwizard.guice.test.util.HooksUtil
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 02.05.2020
 */
class HooksUtilsTest extends Specification {

    def "Check hooks initialization"() {

        when: "registering hook"
        HooksUtil.register(TestHook)
        then: "hook registered"
        ConfigurationHooksSupport.count() == 1
    }

    static class TestHook implements GuiceyConfigurationHook {
        @Override
        void configure(GuiceBundle.Builder builder) {

        }
    }
}
