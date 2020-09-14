package kr.jadekim.default.configuration.koin

import kr.jadekim.common.apiserver.AbstractServer
import org.koin.core.Koin
import org.koin.core.error.NoBeanDefFoundException
import org.koin.experimental.builder.getArguments
import org.koin.ext.getFullName
import kotlin.reflect.KClass

abstract class BaseServerApplication<Server : AbstractServer>(
        applicationName: String,
        val serverClass: KClass<Server>,
        vararg args: String
) : BaseKoinApplication(applicationName, *args) {

    protected lateinit var server: Server

    override fun onCreatedContainer() {
        server = createServer()
    }

    override fun startApplication() {
        onStartServer()
        server.start()
    }

    override fun stopApplication() {
        onStopServer()
        server.stop()
    }

    open fun onStartServer() {
        //do nothing
    }

    open fun onStopServer() {
        //do nothing
    }

    @Suppress("UNCHECKED_CAST")
    open fun createServer(koin: Koin = this.koin): Server {
        return try {
            koin.get(serverClass::class)
        } catch (e: NoBeanDefFoundException) {
            val constructor = serverClass.java.constructors.firstOrNull()
                    ?: error("No constructor found for class '${serverClass.getFullName()}'")

            constructor.newInstance(*getArguments(constructor, koin._scopeRegistry.rootScope)) as Server
        }
    }
}