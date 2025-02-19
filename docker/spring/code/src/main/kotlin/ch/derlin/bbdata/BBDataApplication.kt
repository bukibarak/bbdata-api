package ch.derlin.bbdata

import ch.derlin.bbdata.common.OnCacheEnabled
import ch.derlin.bbdata.common.dates.JodaUtils
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.servers.Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.*
import org.springframework.core.env.Environment
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.annotation.PostConstruct
import kotlin.system.exitProcess


@SpringBootApplication
@ComponentScan(excludeFilters = [
        ComponentScan.Filter(type = FilterType.CUSTOM, classes = [ExcludePackagesFilter::class])
])
@EnableTransactionManagement
@OpenAPIDefinition( // see https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations
        info = Info(
                title = "BBData API",
                version = "\${info.build.version}",
                description = """
This document describes the different endpoints available through the bbdata API,
a json REST api to let you manage, view, and consult objects and values. Find more information, including
common errors codes and more, by visiting <a href="/#more-info">our landing page</a>.""",
                contact = Contact(url = "http://icosys.ch", name = "Lucy Linder", email = "lucy.derlin@gmail.com")
        ),
        // see https://github.com/springdoc/springdoc-openapi/issues/118#issuecomment-585777113
        servers = [Server(url = "/")]
)
@SecurityScheme(name = "auth", type = SecuritySchemeType.HTTP, scheme = "basic")
@EnableConfigurationProperties
// make the git.properties generated by "com.gorylenko.gradle-git-properties" at build time to the system
// so env.getProperty("git.xxx") returns a value (see ActuatorInfo for more details)
@PropertySource("classpath:git.properties")
class BBDataApplication {

    init {
        // UTC timezone is central !
        JodaUtils.setDefaultTimeZoneUTC()
        // the following set the default datetime handling, but that can
        // be overriden using properties (see CustomConfigProperties)
        JodaUtils.defaultPattern = JodaUtils.FMT_ISO_MILLIS
        JodaUtils.setAcceptableDateRange(from = "2016-01-01")
    }

}

// CORS configuration: allow everything from all origin
@Configuration
class CORSConfigurer : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
    }
}


// this is only to turn off warnings "Spring Data Cassandra - Could not safely identify store assignment"
// to work, spring.data.cassandra.repositories.type=none must be set in the properties file
// if there is a problem, simply delete the class+annotation and remove the property in application.properties
@Configuration
@Profile(Profiles.CASSANDRA)
@EnableCassandraRepositories(basePackages = ["ch.derlin.bbdata.common.cassandra"])
class CassandraConfig


// @EnableCaching adds many layers of AOP/interceptor, even if spring.cache.type=none
// Hence, we only enable it if explicitly asked.
// see @OnCacheEnabled and https://stackoverflow.com/a/56901878
@Configuration
@OnCacheEnabled
@EnableCaching
class CachingConfig {

    @Value("\${spring.cache.type}")
    private lateinit var cacheType: String

    @Autowired
    private lateinit var environment: Environment

    var logger: Logger = LoggerFactory.getLogger(CachingConfig::class.java)

    @PostConstruct
    fun checkCacheType() {
        logger.info("caching ENABLED")
        if (cacheType.toLowerCase() == "simple" &&
                (environment.activeProfiles.contains(Profiles.INPUT_ONLY) || environment.activeProfiles.contains(Profiles.OUTPUT_ONLY))) {
            // forbid in-memory caching if launching the app in split mode (input only or output only)
            logger.error("Using in-memory caching with split application (input|output in different JVMs) can lead to security issues." +
                    "Please, either disable caching or use an external cache such as redis.")
            exitProcess(1)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<BBDataApplication>(*args)
}