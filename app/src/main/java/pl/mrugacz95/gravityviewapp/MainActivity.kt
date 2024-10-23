package pl.mrugacz95.gravityviewapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pl.mrugacz95.gravityview.Vec2
import pl.mrugacz95.gravityviewapp.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var stopped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.jump.setOnClickListener {
            for (body in binding.gravityview.getBodies()) {
                body.velocity += Vec2(0.0, -9.0)
            }
        }
        binding.stop.setOnClickListener {
            if (!stopped) {
                binding.gravityview.stop()
                binding.stop.setText(R.string.start)
            } else {
                binding.stop.setText(R.string.stop)
                binding.gravityview.start()
            }
            stopped = !stopped
        }
        binding.gravitySwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.gravityview.engine.g = if (isChecked) {
                GRAVITY_CONSTANT
            } else {
                0.0
            }
        }
        binding.shake.setOnClickListener {
            for (body in binding.gravityview.getBodies()) {
                if (!body.isStatic)
                    body.velocity += Vec2((Random.nextDouble() - 0.5) * 3, (Random.nextDouble() - 0.5) * 3.0)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.gravityview.stop()
    }

    override fun onResume() {
        super.onResume()
        binding.gravityview.start()
    }

    companion object {
        const val GRAVITY_CONSTANT = 9.81
    }
}
