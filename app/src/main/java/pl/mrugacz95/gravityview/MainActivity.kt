package pl.mrugacz95.gravityview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pl.mrugacz95.gravityview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }

    override fun onPause() {
        super.onPause()
        binding.gravityview.stop()
    }

    override fun onResume() {
        super.onResume()
        binding.gravityview.start()
    }
}
