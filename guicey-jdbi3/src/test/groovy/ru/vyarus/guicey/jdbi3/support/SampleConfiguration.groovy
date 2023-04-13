package ru.vyarus.guicey.jdbi3.support

import io.dropwizard.core.Configuration
import io.dropwizard.db.DataSourceFactory

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

/**
 * @author Vyacheslav Rusakov
 * @since 31.08.2018
 */
class SampleConfiguration extends Configuration {

    @Valid
    @NotNull
    DataSourceFactory database = new DataSourceFactory();
}
