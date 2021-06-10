package com.example.mobisserver.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobisserver.databinding.ActivityMainBinding
import com.example.mobisserver.util.Permission
import com.gun0912.tedpermission.PermissionListener

class MainActivity : BaseActivity(), PermissionListener {
    private val logTag = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.start.setOnClickListener {

        }

        binding.stop.setOnClickListener {

        }
    }

    override fun onPermissionGranted() {
        Permission.permissionGiven.value = true
    }

    override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
        this.finish()
        Toast.makeText(this, "권한 허용을 하지 않으면 서비스를 이용할 수 없습니다.", Toast.LENGTH_SHORT).show()
    }


}