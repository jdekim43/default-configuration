package kr.jadekim.default.configuration.koin

import kr.jadekim.common.apiserver.enumuration.Environment
import kr.jadekim.common.apiserver.enumuration.IEnvironment
import kr.jadekim.common.util.loadProperties
import kr.jadekim.common.util.parseArgument
import kr.jadekim.common.util.shutdownHook
import kr.jadekim.default.configuration.jlog.default
import kr.jadekim.default.configuration.ktor.BaseKtorServer
import kr.jadekim.logger.JLog
import kr.jadekim.logger.integration.KoinLogger
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.experimental.builder.getArguments
import org.koin.ext.getFullName
import org.koin.ext.scope
import java.io.File
import java.util.*
import kotlin.reflect.KClass

abstract class BaseKtorApplication<Server : BaseKtorServer>(
        val applicationName: String,
        val serverClass: KClass<Server>,
        vararg val args: String
) {

    abstract val modules: List<Module>

    lateinit var serviceEnv: IEnvironment

    protected val logger = JLog.get(applicationName)

    protected lateinit var properties: Properties
    protected lateinit var koin: Koin
    protected lateinit var server: Server

    private var initialized = false

    private val CLASSPATH_PREFIX = "classpath:"

    fun init(serviceEnv: IEnvironment? = null) {
        logger.info("Startup $applicationName")

        val serviceEnvValue = System.getenv("SERVICE_ENV")?.toLowerCase() ?: "local"
        this.serviceEnv = serviceEnv ?: Environment.from(serviceEnvValue)
                ?: throw IllegalArgumentException("Invalid SERVICE_ENV value")

        JLog.default(this.serviceEnv)

        loadArgument(parseArgument(*args))
        onInit()
        initialized = true
    }

    fun start(blocking: Boolean = true) {
        if (!initialized) {
            init()
        }

        koin = createContainer()
        server = createServer()

        shutdownHook {
            server.stop()
            onStop()
            koin.close()
        }

        onStart()
        server.start(blocking)
    }

    open fun onInit() {
        //do nothing
    }

    open fun loadArgument(arguments: Map<String, List<String>>) {
        loadPropertiesFromArguments(arguments)
    }

    open fun onStart() {
        //do nothing
    }

    open fun onStop() {
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

    @Suppress("UNCHECKED_CAST")
    open fun createServer(koin: Koin = this.koin): Server {
        val constructor = serverClass.java.constructors.firstOrNull()
                ?: error("No constructor found for class '${serverClass.getFullName()}'")

        return constructor.newInstance(*getArguments(constructor, koin.scope)) as Server
    }

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