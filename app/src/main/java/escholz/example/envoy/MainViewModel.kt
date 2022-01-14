package escholz.example.envoy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.envoyproxy.envoymobile.RequestMethod
import java.net.URL

class MainViewModel constructor(application: Application): AndroidViewModel(application) {

    private val repository: MainRepository by lazy {
        MainRepository(application)
    }

    fun callLocalhost() {
        repository.request(RequestMethod.GET, URL("http://localhost"), mapOf()) { statusCode, byteBuffer, headers ->

        }
    }
}