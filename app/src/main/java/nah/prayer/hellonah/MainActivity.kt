package nah.prayer.hellonah

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        HelloNahService.startService(this, 3)

        HelloNahService.setOnTimeEventListener { count ->
            if (count >= 10) {
                HelloNahService.stopService()
            }
        }

    }
}
