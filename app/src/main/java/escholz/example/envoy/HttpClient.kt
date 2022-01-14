package escholz.example.envoy

interface HttpClient {

    fun newStream(): HttpStream

}