package com.example.localmessenger

import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.localmessenger.databinding.ActivityMessageBinding

class MessageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMessageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val connectFragment = ConnectFragment()
        val messageFragment = MessageFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.flFragment, connectFragment, "CONNECT")
            .add(R.id.flFragment, messageFragment, "MESSAGES")
            .hide(messageFragment)
            .commit()

        binding.bottomNavigationView.setOnItemSelectedListener {
            supportFragmentManager.beginTransaction().apply {
                when (it.itemId) {
                    R.id.miConnect -> {
                        hide(messageFragment); show(connectFragment)
                    }
                    R.id.miMessages -> {
                        hide(connectFragment); show(messageFragment)
                    }
                }
                commit()
            }
            true
        }
    }

    private fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            addToBackStack(null)
            commit()
        }
    }
}