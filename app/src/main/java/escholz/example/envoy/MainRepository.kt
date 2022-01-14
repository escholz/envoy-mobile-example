package escholz.example.envoy

import android.content.Context
import io.envoyproxy.envoymobile.*
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class MainRepository constructor(private val context: Context) {
    fun request(
        requestMethod: RequestMethod = RequestMethod.GET,
        url: URL,
        headers: Map<String, List<String>>?,
        onComplete: (statusCode: Int, headers: Map<String, List<String>>?, body: ByteBuffer?) -> Unit
    ) = EnvoyHttpClient(
        AndroidEngineBuilder(context),
        object : EnvoyHttpClientConfiguration {
            override val appId: String
                get() = "EnvoyExample"
            override val appVersion: String
                get() = "0.0.1"
        },
        Executors.newSingleThreadExecutor()
    ).newStream().apply {
        addReceiver(onComplete)
        send(method = requestMethod.name, url = url, headers = headers)
        close()
    }
}