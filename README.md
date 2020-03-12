# default-configuration
개발할 때 자주 사용하는 세팅

* jackson
* j-logger
* ktor
    * ktor with koin

## Install
### Gradle Project
1. Add dependency
    ```
    build.gradle.kts
   
    implementation("kr.jadekim:default-configuration:1.0.0")
    ```

## How to use
### Jackson
#### Install
```
build.gradle.kts

implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
implementation("joda-time:joda-time:$jodaTimeVersion")
```
#### Timestamp DateFormat
```
val mapper = jacksonObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .setDateFormat(TimestampDateFormat())
    .registerModule(timestampModule)
```
### j-logger
#### default setting
* Environment.LOCAL 이면 TextPrinter 사용, 그 외에는 JsonPrinter 사용
* loggerName 축약 : loggerName 을 패키지를 포함한 클래스 이름으로 설정했고 32자 이상일 경우 중간 패키지명 알파뱃 한글자로 축약
* 주요 logger logging 레벨 기본값 적용
```
val environment = Environment.LOCAL
val applicationPackage = listOf("kr.jadekim")
val isAsyncLogging = true

JLog.default(environment, applicationPackage, isAsyncLogging)
```
### ktor
#### Install
```
build.gradle.kts
    
implementation("kr.jadekim:common-api-server:$commonApiServerVersion")
implementation("kr.jadekim:ktor-extension:$ktorExtensionVersion")
implementation("io.ktor:ktor-server-host-common:$ktorVersion")
implementation("io.ktor:ktor-server-netty:$ktorVersion")
```
#### BaseKtorServer
* Netty engine
* Initial j-logger global log context
* Install default ktor feature
    * KtorLogContextFeature (j-logger)
    * XForwardedHeaderSupport
    * RequestLogFeature
    * AutoHeadResponse
    * ContentNegotiation (json - Jackson)
    * StatusPages
##### 서버 설정
```
class KtorServer : BaseKtorServer(
    serviceEnv = Environment.LOCAL,
    port = 8080,
    release = "release-200202-rc1",
    jackson = jacksonObjectMapper(),
    serverName = "ktor-server"
) {

    //Engine 변경
    override val server = embeddedServer(CIO, port = port) {
        configure()
    }

    //RequestLogger 에서 파라미터를 저장할 때 일부 파라미터 제거
    override val filterParameters: List<String> = listOf(
        "password"
    )

    //요청별 LogContext 에서 추가할 값 설정
    override fun PipelineContext<Unit, ApplicationCall>.logContext(): Map<String, String> = mapOf(
        "userId" to context.principal<UserTokenPrincipal>()?.userId
    )

    //Install ktor feature
    override fun Application.installExtraFeature() {
        install(Authentication) {
            ...
        }
    }

    //Configure route
    override fun Routing.configureRoute() {
        get("/path/name") {
            call.respondText("Hello!")
        }
    }

    //ErrorHandler 변경
    override fun StatusPages.Configuration.configureErrorHandler() {
        status(HttpStatusCode.InternalServerError) {
            errorLogger.sLog(Level.ERROR, "InternalServerError-UnknownException")

            responseError(UnknownException(Exception()))
        }
        exception<Throwable> {
            val wrapper = UnknownException(it, it.message)

            errorLogger.sLog(Level.ERROR, wrapper.message ?: it.javaClass.simpleName, wrapper)

            responseError(wrapper)
        }
        exception<ApiException> {
            val errorContext = Jackson.convertValue<Map<String, Any?>>(it)
                .filterKeys { it == "cause" }

            errorLogger.sLog(it.logLevel, it.message ?: it.javaClass.simpleName, it, errorContext)

            responseError(it)
        }
    }

    //Override error response
    override suspend fun PipelineContext<*, ApplicationCall>.responseError(exception: ApiException) {
        context.respond(HttpStatusCode.fromValue(converted.httpStatus), exception.toResponse(locale))
    }
}
```
##### 서버 실행 / 종료
```
val server = KtorServer()

//서버 실행
server.start() or server.start(true)

//서버 백그라운드에서 실행
server.start(false)

//서버 종료
server.stop()
```
#### BaseKtorApplication (Koin)
* j-logger default 설정
* args 에서 properties 파일 불러오기
* koin container 생성 및 default 설정
* ktor server 생성 및 실행
* shutdownHook 설정
##### Install
```
build.gradle.kts

implementation("org.koin:koin-core:$koinVersion")
implementation("org.koin:koin-core-ext:$koinVersion")
implementation("kr.jadekim:common-util:$commonUtilVersion")
```
##### 애플리케이션 설정
```
class KtorServerApplication(
    varargs val args: String
) : BaseKtorApplication(
    "ktor-server-application",
    KtorServer::class,
    *args
) {
    // set up koin modules
    override val modules = listOf(
        ...
    )

    //initalize callback
    override fun onInit() { ... }

    //start callback
    override fun onStart() { ... }

    //stop callback
    override fun onStop() { ... }

    //customize koin container
    override fun createContainer(): Koin = startKoin {
        ...
    }.koin
}
```
##### 애플리케이션 실행
```
fun main(vararg args: String) = KtorServerApplication(*args).start()
```