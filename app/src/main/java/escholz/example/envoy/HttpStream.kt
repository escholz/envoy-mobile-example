package escholz.example.envoy

import java.io.Closeable
import java.net.URL
import java.nio.ByteBuffer

interface HttpStream : Closeable, AutoCloseable {

    fun close(body: ByteBuffer)

    fun close(headers: Map<String, List<String>>)

    fun send(
        method: String = "GET",
        url: URL? = null,
        headers: Map<String, List<String>>? = null,
        body: ByteBuffer? = null
    ): HttpStream

    fun addReceiver(closure: (statusCode: Int, headers: Map<String, List<String>>?, body: ByteBuffer?) -> Unit): HttpStream

}