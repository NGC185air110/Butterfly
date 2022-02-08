package zlc.season.butterflydemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import zlc.season.bufferfly.ButterflyModuleApp
import zlc.season.butterfly.annotation.Butterfly

@Butterfly("aaa")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterflyModuleApp().get()
    }
}