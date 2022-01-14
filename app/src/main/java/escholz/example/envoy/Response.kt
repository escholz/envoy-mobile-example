package escholz.example.envoy

import java.nio.ByteBuffer

interface Response {
    val statusCode: Int

    val body: ByteBuffer?

    val headers: Map<String, List<String>>

}