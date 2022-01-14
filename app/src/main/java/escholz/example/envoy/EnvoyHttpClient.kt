package escholz.example.envoy

import io.envoyproxy.envoymobile.EngineBuilder
import io.envoyproxy.envoymobile.LogLevel
import java.io.Closeable
import java.util.concurrent.Executor

class EnvoyHttpClient constructor(
    engineBuilder: EngineBuilder,
    configuration: EnvoyHttpClientConfiguration,
    private val executor: Executor
): HttpClient, Closeable {
    private val engine by lazy {
        engineBuilder
            //.addEngineType { }
            //.setOnEngineRunning { notifyEngineRunning() }
            .addAppId(configuration.appId)
            .addAppVersion(configuration.appVersion)
            .addConnectTimeoutSeconds(60000)
            .addDNSFailureRefreshSeconds(10, 60)
            .addDNSRefreshSeconds(600)
            //.addNativeFilter(name = "", typedConfig = "")
            //.addPlatformFilter(name = "") { MyAsyncRequestFilter() }
            //.addStatsDomain("")
            .addStatsFlushSeconds(600)
            //.addStringAccessor(name = "") { "value" }
            //.addVirtualClusters(virtualClusters = "")
            //.setLogger { message -> sendLog(message) }
            .addLogLevel(LogLevel.DEBUG)
            .build()
    }

    override fun newStream(): HttpStream {
        return EnvoyHttpStream(engine.streamClient().newStreamPrototype(), executor)
    }

    override fun close() {
        engine.terminate()
    }
}