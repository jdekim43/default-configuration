package kr.jadekim.default.configuration.koin

import kr.jadekim.common.apiserver.enumuration.Environment
import kr.jadekim.common.apiserver.enumuration.IEnvironment
import kr.jadekim.common.util.loadProperties
import kr.jadekim.common.util.parseArgument
import kr.jadekim.common.util.shutdownHook
import kr.jadekim.default.configuration.jlog.default
import kr.jadekim.logger.JLog
import kr.jadekim.logger.integration.KoinLogger
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File
import java.util.*

abstract class BaseKoinApplication(
    val applicationName: String,
    vararg val args: String
) {

    abstract val modules: List<Module>

    lateinit var serviceEnv: IEnvironment

    protected val logger = JLog.get(applicationName)

    protected lateinit var properties: Properties
    protected lateinit var koin: Koin

    private var initialized = false

    private val CLASSPATH_PREFIX = "classpath:"

    abstract fun startApplication()

    abstract fun stopApplication()

    fun init(serviceEnv: IEnvironment? = null) {

        val serviceEnvValue = System.getenv("SERVICE_ENV")?.toLowerCase() ?: "local"
        this.serviceEnv = serviceEnv ?: Environment.from(serviceEnvValue)
                ?: throw IllegalArgumentException("Invalid SERVICE_ENV value")

        JLog.default(this.serviceEnv)
        logger.info("Startup $applicationName")

        loadArgument(parseArgument(*args))
        onInit()
        initialized = true
    }

    fun start() {
        if (!initialized) {
            init()
        }

        koin = createContainer()
        onCreatedContainer()

        shutdownHook {
            stopApplication()
            koin.close()
        }

        startApplication()
    }

    open fun loadArgument(arguments: Map<String, List<String>>) {
        loadPropertiesFromArguments(arguments)
    }

    //Initialize application
    open fun onInit() {
        //do nothing
    }

    //Created Container
    open fun onCreatedContainer() {
        //do nothing
    }

    @Suppress("UNCHECKED_CAST")
    open fun createContainer(): Koin = startKoin {
        logger(KoinLogger())
        koin._propertyRegistry.saveProperties(properties)
        modules(module {
            single { serviceEnv }
            single { properties }
        })
        modules(modules)
    }.koin

    protected open fun loadPropertiesFromArguments(arguments: Map<String, List<String>>) {
        val externalPropertyFiles = mutableListOf<File>()

        arguments["config"]?.map {
            if (it.startsWith(CLASSPATH_PREFIX)) {
                val resource = javaClass.getResource(it.substring(CLASSPATH_PREFIX.length))
                File(resource.toURI())
            } else {
                File(it)
            }
        }?.let {
            externalPropertyFiles.addAll(it)
        }

        arguments["c"]?.map {
            if (it.startsWith(CLASSPATH_PREFIX)) {
                val resource = javaClass.getResource(it.substring(CLASSPATH_PREFIX.length))
                File(resource.toURI())
            } else {
                File(it)
            }
        }?.let {
            externalPropertyFiles.addAll(it)
        }

        if (externalPropertyFiles.isEmpty()) {
            throw IllegalArgumentException("Require propertyFile path -c or --config")
        }

        properties = loadProperties(externalPropertyFiles)
    }
}