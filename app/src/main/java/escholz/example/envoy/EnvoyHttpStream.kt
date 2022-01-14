package escholz.example.envoy

import io.envoyproxy.envoymobile.*
import io.envoyproxy.envoymobile.RequestHeadersBuilder
import io.envoyproxy.envoymobile.RequestMethod
import io.envoyproxy.envoymobile.UpstreamHttpProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.Executor

class EnvoyHttpStream constructor(
    private val prototype: StreamPrototype,
    private val executor: Executor
): HttpStream {
    init {
        prototype
            .setOnResponseData(this::onResponseData)
            .setOnResponseHeaders(this::onResponseHeaders)
            .setOnResponseTrailers(this::onResponseTrailers)
            .setOnCancel(this::onCancel)
            .setOnError(this::onEnvoyError)
    }

    private val receivers: MutableSet<(statusCode: Int, headers: Map<String, List<String>>?, body: ByteBuffer?) -> Unit> = mutableSetOf()

    private val coroutineScope = CoroutineScope(executor.asCoroutineDispatcher())

    inner class ResponseBuffer(val statusCode: Int, bodyBytes: Int) {
        val byteBuffer: ByteBuffer = ByteBuffer.allocate(bodyBytes)
        val headers: MutableMap<String, List<String>> = mutableMapOf()
    }

    private var current: ResponseBuffer? = null

    private fun onResponseData(responseData: ByteBuffer, endStream: Boolean) = coroutineScope.launch {
        current?.let {
            if (endStream) {
                receivers.onEach { action -> launch { action.invoke(it.statusCode, it.headers, it.byteBuffer.duplicate()) } }
                current = null
            } else {
                it.byteBuffer.put(responseData)
            }
        }
    }

    private fun onResponseHeaders(responseHeaders: ResponseHeaders, endStream: Boolean) = coroutineScope.launch {
        current = ResponseBuffer(
            statusCode = responseHeaders.httpStatus ?: 418,
            bodyBytes = responseHeaders.value("Content-length")?.first()?.toIntOrNull() ?: 0
        ).apply {
            headers.putAll(responseHeaders.headers)
            if (endStream) {
                receivers.onEach { action -> launch { action.invoke(statusCode, headers, null) } }
            } else if (current?.byteBuffer?.hasRemaining() == true) {
                receivers.onEach { action -> launch { action.invoke(statusCode, headers, current?.byteBuffer?.duplicate()) } }
            }
        }
    }

    private fun onResponseTrailers(trailers: ResponseTrailers) = coroutineScope.launch {
        TODO("Determine when this is called, is it a confirmation of the client response when closing?")
    }

    private fun onEnvoyError(error: EnvoyError) = coroutineScope.launch {
        TODO("Set stream to terminal state; Log")
    }

    private fun onCancel() = coroutineScope.launch {
        TODO("Set stream to terminal state; Log")
    }

    private val retryPolicy by lazy {
        RetryPolicy(
            maxRetryCount = 3,
            retryOn = listOf(
                RetryRule.CONNECT_FAILURE,
                RetryRule.GATEWAY_ERROR,
                RetryRule.REFUSED_STREAM,
                RetryRule.RESET,
                RetryRule.RETRIABLE_4XX,
                RetryRule.RETRIABLE_HEADERS,
                RetryRule.STATUS_5XX
            ),
            retryStatusCodes = listOf(503),
            perRetryTimeoutMS = 1000L,
            totalUpstreamTimeoutMS = 60000L)
    }

    private val stream by lazy {
        prototype.start(executor)
    }

    override fun addReceiver(closure: (statusCode: Int, headers: Map<String, List<String>>?, body: ByteBuffer?) -> Unit): HttpStream = apply {
        receivers.add(closure)
    }

    override fun send(method: String, url: URL?, headers: Map<String, List<String>>?, body: ByteBuffer?): HttpStream = apply {
        url?.let {
            val headerBuilder = RequestHeadersBuilder(
                method = RequestMethod.valueOf(method),
                scheme = url.protocol,
                authority = url.authority,
                path = url.path
            )

            headerBuilder
                .addUpstreamHttpProtocol(UpstreamHttpProtocol.HTTP2)
                .addUpstreamHttpProtocol(UpstreamHttpProtocol.HTTP1)

            headerBuilder.addRetryPolicy(retryPolicy)

            headers?.entries?.fold(headerBuilder, { acc, entry -> entry.value.forEach { value -> acc.add(entry.key, value) }; acc})

            stream.sendHeaders(headerBuilder.build(), false)
        }
        body?.let {
            stream.sendData(body)
        }
    }

    override fun close(body: ByteBuffer) {
        stream.close(body)
    }

    override fun close(headers: Map<String, List<String>>) {
        stream.close(headers.entries.fold(RequestTrailersBuilder(), { builder, entry -> builder.set(entry.key, entry.value.toMutableList()) }).build())
    }

    override fun close() {
        stream.close(RequestTrailersBuilder().build())
    }
}