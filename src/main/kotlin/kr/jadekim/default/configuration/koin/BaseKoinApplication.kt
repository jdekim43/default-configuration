package kr.jadekim.default.configuration.koin

import kr.jadekim.common.apiserver.enumuration.Environment
import kr.jadekim.common.apiserver.enumuration.IEnvironment
import kr.jadekim.common.util.loadProperties
import kr.jadekim.common.util.parseArgument
import kr.jadekim.common.util.shutdownHook
import kr.jadekim.default.configuration.jlog.default
import kr.jadekim.logger.JLog
import kr.jadekim.logger.context.GlobalLogContext
import kr.jadekim.logger.integration.KoinLogger
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File
import java.io.InputStream
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
        GlobalLogContext["applicationName"] = applicationName
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
        koin.loadModules(
                modules + module {
                    single { serviceEnv }
                    single { properties }
                }
        )
        koin.createRootScope()
        //https://github.com/InsertKoinIO/koin/issues/871
        //Fixed in 2.2.0
    }.koin

    protected open fun loadPropertiesFromArguments(arguments: Map<String, List<String>>) {
        val externalPropertyFiles = mutableListOf<InputStream>()

        (arguments["config"] + arguments["c"]).map {
            if (it.startsWith(CLASSPATH_PREFIX)) {
                it.substring(CLASSPATH_PREFIX.length)
                        .let { path -> javaClass.getResourceAsStream(path) }
            } else {
                File(it).inputStream()
            }
        }.let {
            externalPropertyFiles.addAll(it)
        }

        properties = loadProperties(externalPropertyFiles)
    }

    private operator fun <T> List<T>?.plus(data: List<T>?): List<T> {
        val result = mutableListOf<T>()

        this?.also { result.addAll(it) }
        data?.also { result.addAll(it) }

        return result
    }
}