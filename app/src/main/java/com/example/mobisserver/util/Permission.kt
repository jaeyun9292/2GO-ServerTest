package com.example.mobisserver.util

import android.Manifest
import android.app.Activity
import androidx.lifecycle.MutableLiveData
import com.example.mobisserver.activity.MainActivity
import com.gun0912.tedpermission.TedPermission

object Permission {
    var permissionGiven = MutableLiveData<Boolean>()
//    const val REQUEST_CODE_PERMISSIONS = 10

    /**
     * Checks all prerequisitory permission
     *
     * @return true if all permissions are granted
     * @return false if otherwise
     */
    internal fun checkPermission(activity: Activity, listener: MainActivity) {
        // 마시멜로(안드로이드 6.0) 이상 권한 체크
        TedPermission.with(activity)
            .setPermissionListener(listener)
            .setRationaleMessage("카메라를 사용하기 위해서는 접근 권한이 필요합니다")
            .setDeniedMessage("앱에서 요구하는 권한설정이 필요합니다...\n [설정] > [권한] 에서 사용으로 활성화해주세요.")
            .setPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .check()
    }
}