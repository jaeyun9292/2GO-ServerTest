package com.example.mobisserver.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.anyractive.medroa.ev.pop.AppListAdapter
import com.anyractive.medroa.ev.pop.ImageSlideFragment
import com.anyractive.medroa.ev.pop.MobisApplication
import com.anyractive.medroa.ev.pop.R
import com.anyractive.medroa.ev.pop.service.ServiceManager
import com.super_rabbit.wheel_picker.WheelPicker
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_setting.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

var isLongClick = false
lateinit var uiview : RelativeLayout
lateinit var settingview : LinearLayout
lateinit var videoview : VideoView
lateinit var editIp : EditText
lateinit var editPort : EditText
lateinit var context : Context

class SettingActivity : BaseActivity()  {
    private val logTag = "SettingActivity"
//    private lateinit var mCurrentPhotoPath: String
    private var index = 0
//    private val gallery = 4
    private val cluster = 5
    private val settingApp1 = 6
    private val settingApp2 = 7
    private var imageUri: Uri? = null
    private var isPlay = false
    private var selectApp1 = "NAVIGATION"
    private var selectApp2 = "MEDIA"
    private var app1 = "NAVIGATION"
    private var app2 = "MEDIA"
    private var path = ""
    private var iconResId = 0
    private var isSelected = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        Log.i(logTag, "SettingActivity onCreate")

        if (action == "End") {
            // 종료
        } else {
            setBind()

            MobisApplication.applicationContext()

            val gestureListener = MyGesture()
            val doubleListener = MyDoubleGesture()

            val gesturedetector = GestureDetector(this, gestureListener)

            gesturedetector.setOnDoubleTapListener(doubleListener)

            uiview = findViewById(R.id.ui_view)
            settingview = findViewById(R.id.setting_view)
            editIp = findViewById(R.id.ip_ed)
            editPort = findViewById(R.id.port_ed)
            videoview = findViewById(R.id.main_video)
            videoview.setOnPreparedListener {
                when (index) {
                    0 -> {
                        videoview.start()
                    }
                }
            }

            name_ed.setOnEditorActionListener { _, actionId, _ ->
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    hideSystemUI()
                    val view = this.currentFocus
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view?.windowToken, 0)

                    true
                } else {
                    false
                }
            }

            if (!Settings.canDrawOverlays(this)) {
                // ask for setting
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            }

            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
//            Log.d(TAG, "권한 설정 완료")
            } else {
//            Log.d(TAG, "권한 설정 요청")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
            }

//        val layoutTransition = layout.layoutTransition
//        layoutTransition.setDuration(1) // Change duration
//        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

//        val layoutTransition1 = ui_view.layoutTransition
//        layoutTransition1.setDuration(1) // Change duration
//        layoutTransition1.enableTransitionType(LayoutTransition.CHANGING)

            layout.setOnTouchListener { _, event ->
                return@setOnTouchListener gesturedetector.onTouchEvent(event)
            }

            ok_btn.setOnClickListener {
                thum_image.visibility = INVISIBLE
                when (index) {
                    0 -> {

                    }
                    1 -> {

                    }
                    2 -> {
                        var encoded = "0"

//                        bitmap = if (image_view.drawable != null) {
//                            val bitmapDrawable: BitmapDrawable = image_view.drawable as BitmapDrawable
//                            bitmapDrawable.bitmap
//                        } else {
//                            BitmapFactory.decodeResource(
//                                context.resources,
//                                R.drawable.img_thumb
//                            )
//                        }

                        if (image_view.drawable != null) {
                            val bitmapDrawable: BitmapDrawable = image_view.drawable as BitmapDrawable
                            var bitmap = bitmapDrawable.bitmap
                            val height = bitmap.height
                            val width = bitmap.width
                            val stream = ByteArrayOutputStream()

                            bitmap = bitmap.scale(200, height / (width / 200), true)
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val imageInByte: ByteArray = stream.toByteArray()
                            encoded = Base64.getEncoder().encodeToString(imageInByte)
                        }

                        val name = name_ed.text

                        mServiceManager?.sendMessage(
                            "api-command=setting&api-action=profile&api-method=post&sub-system-id=1",
                            "nick-name=$name&pic-base64=$encoded&pic-mimetype=image/png"
                        )
                    }
                    3 -> {
                        if (selectApp1 == selectApp2) {
                            if(isSelected) {
                                thum_image.visibility = VISIBLE
                            } else {
                                thum_image.visibility = INVISIBLE
                            }
                            showPopup()

                            return@setOnClickListener
                        } else {
                            second_step.visibility = INVISIBLE
                            isSelected = true
                            if (!isPlay) {
                                appSettingCompleted()
                            }
                        }
                    }
                    else -> {
                        val pagerAdapter = ScreenSlidePagerAdapter(this, app2)
                        vp_scroll_tutorial.adapter = pagerAdapter
                        viewpager_view.visibility = VISIBLE
                    }
                }

                if (!isPlay) {
                    first_step.visibility = INVISIBLE
                    second_step.visibility = INVISIBLE
                    third_step.visibility = INVISIBLE
                    forth_step.visibility = INVISIBLE

                    if (index < 4) {
                        if (index == 2) {
                            Handler().postDelayed({
                                second_step.visibility = VISIBLE
                            }, 4400L)
                        }
                        if (index == 4) {
                            ok_btn.visibility = INVISIBLE
                        }

                        isPlay = true
                        videoview.start()
                    }
                }
            }

            back_btn.setOnClickListener {
                if (!isPlay) {
                    if (index > 0) {
                        index --
                        changeStep()
                    }
                }
            }

            camera_btn.setOnClickListener {
                CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setMinCropResultSize(1480, 1400)
                    .setMaxCropResultSize(1480, 1400)
                    .start(this)
            }

            user_image.setOnClickListener {

            }

            // 데이터 조회
            val ip = MobisApplication.prefs.getString("ip", "192.168.11.2")
            val port = MobisApplication.prefs.getString("port", "8181")
            editIp.setText(ip)
            editPort.setText(port)

            context = this

            path = "android.resource://$packageName/"

            videoview.setVideoPath(path + R.raw.intro1)
            videoview.setOnCompletionListener {
                isPlay = false
                when(index) {
                    0 -> {
                        ok_btn.visibility = VISIBLE
                        videoview.setVideoPath(path + R.raw.intro2)
                    }
                    1 -> {
                        ok_btn.setTextColor(Color.parseColor("#7a56ff"))
//                        back_btn.visibility = VISIBLE
                        first_step.visibility = VISIBLE
                        second_step.visibility = INVISIBLE
                        videoview.setVideoPath(path + R.raw.intro3)
                    }
                    2 -> {
                        back_btn.setBackgroundResource(R.drawable.btn_back_y)
                        ok_btn.setTextColor(Color.parseColor("#c2a000"))
                        first_step.visibility = INVISIBLE
                        third_step.visibility = INVISIBLE
                    }
                    3 -> {
                        back_btn.setBackgroundResource(R.drawable.btn_call_back)
                        ok_btn.setTextColor(Color.parseColor("#cc1a2e"))
                        second_step.visibility = INVISIBLE
                        third_step.visibility = VISIBLE

                        val app1Id = resources.getIdentifier("setting_$app1", "drawable", packageName)
                        val app2Id = resources.getIdentifier("setting_$app2", "drawable", packageName)

                        setting_app1.setBackgroundResource(app1Id)
                        setting_app2.setBackgroundResource(app2Id)

                        videoview.setVideoPath(path + R.raw.intro4)
                    }
                    4 -> {
                        btn_start.visibility = VISIBLE
                        videoview.setVideoPath(path + R.raw.intro5)
                        back_btn.visibility = INVISIBLE
                    }

                }
                index ++

            }

            val listAdapter = AppListAdapter()
            listAdapter.init()

            app1_picker.setSelectedTextColor(R.color.navi)
//            app1_picker.setUnselectedTextColor(R.color.color_1_white)
            app1_picker.setAdapter(listAdapter)
            app1_picker.setValue("NAVIGATION")

            app1_picker.setOnValueChangedListener(object :
                com.super_rabbit.wheel_picker.OnValueChangeListener {
                override fun onValueChange(picker: WheelPicker, oldVal: String, newVal: String) {
                    selectApp1 = if (newVal == "") "NAVIGATION" else newVal
                }
            })

            app2_picker.setSelectedTextColor(R.color.navi)
//            app2_picker.setUnselectedTextColor(R.color.color_1_white)
            app2_picker.setAdapter(listAdapter)
            app2_picker.setValue("MEDIA")
            app2_picker.setOnValueChangedListener(object :
                com.super_rabbit.wheel_picker.OnValueChangeListener {
                override fun onValueChange(picker: WheelPicker, oldVal: String, newVal: String) {
                    selectApp2 = if (newVal == "") "NAVIGATION" else newVal
                }
            })

//        app1_picker.setOnTouchListener { v, event ->
//            var ret = false
//            when(event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    moveCount = 0
//                }
//                MotionEvent.ACTION_UP -> {
//                    if (moveCount < 5) {
//                        Log.d("app1_picker", "setOnClickListener")
//                        listAdapter.removeList(app1)
//                        listAdapter.notifyDataSetChanged()
//
//                        app1_picker.visibility = INVISIBLE
//                        app1_arrow.visibility = INVISIBLE
//                        app1_btn.setBackgroundResource(R.drawable.btn_dim)
//                        app1_btn.text = app1
//
//                        app1 = app1.toLowerCase(Locale.getDefault())
//                        if (app1 == "navigation") {
//                            app1 = "navi"
//                            app2 = "media"
//                        } else if (app1 == "media") {
//                            app1 = "media"
//                            app2 = "navi"
//                        }
//
//                        app2_picker.setAdapter(listAdapter)
//                        app2_picker.setValue("")
//                        app2_picker.visibility = VISIBLE
//                        app2_arrow.visibility = VISIBLE
//                        app2_btn.setBackgroundResource(R.drawable.btn_setting_sel)
//                    }
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    moveCount ++
//                }
//            }
//
//            ret
//        }

//        app2_picker.setOnTouchListener { v, event ->
//            var ret = false
//            when(event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    moveCount = 0
//                }
//                MotionEvent.ACTION_UP -> {
//                    if (moveCount < 5) {
//                        Log.d("app2_picker", "setOnClickListener")
//                        app2_picker.visibility = INVISIBLE
//                        app2_arrow.visibility = INVISIBLE
//                        app2_btn.setBackgroundResource(R.drawable.btn_setting_dim)
//                        if (app2 == "navi") {
//                            app2_btn.text = "navigation"
//                        } else {
//                            app2_btn.text = app2
//                        }
//
//                        Log.d("app1_picker", "app1 : $app1 / app2 : $app2")
//                    }
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    moveCount ++
//                }
//            }
//
//            ret
//        }

            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            setting_cluster.setOnClickListener {
                setting_cluster_im.visibility = VISIBLE
                setting_app1_im.visibility = INVISIBLE
                setting_app2_im.visibility = INVISIBLE
                intent.action = "Cluster"
                startActivityForResult(intent, cluster)

                overridePendingTransition(R.anim.scale_up, R.anim.hold)
            }

            setting_app1.setOnClickListener {
                setting_cluster_im.visibility = INVISIBLE
                setting_app1_im.visibility = VISIBLE
                setting_app2_im.visibility = INVISIBLE
                if (app1 != "phone" && app1 != "setting") {
                    intent.action = app1
                    startActivityForResult(intent, settingApp1)

                    overridePendingTransition(R.anim.scale_up, R.anim.hold)
                }
            }

            setting_app2.setOnClickListener {
                setting_cluster_im.visibility = INVISIBLE
                setting_app1_im.visibility = INVISIBLE
                setting_app2_im.visibility = VISIBLE
                if (app2 != "phone" && app2 != "setting") {
                    intent.action = app2
                    startActivityForResult(intent, settingApp2)

                    overridePendingTransition(R.anim.scale_up, R.anim.hold)
                }
            }

            vp_scroll_tutorial.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    indicator.animatePageSelected(position)
                    when(position) {
                        0 -> {
                            btn_ok.setBackgroundResource(R.drawable.btn_coach_skip)
                            btn_ok.requestLayout()
                        }
//                        1 -> {
//                            btn_ok.setBackgroundResource(R.drawable.btn_coach_skip)
//                        }
                        1 -> {
                            btn_ok.setBackgroundResource(R.drawable.btn_coach_ok)
                        }
                    }
                }
            })

            btn_ok.setOnClickListener {
                skipBtnClick()
            }

            indicator.setViewPager(vp_scroll_tutorial)
            indicator.createIndicators(2, 0)

            btn_start_back.setOnClickListener {
                if (!isPlay) {
                    index = 4
                    changeStep()
                    start_view.visibility = INVISIBLE
                    btn_start.visibility = INVISIBLE
//                    back_btn.visibility = INVISIBLE
                }
            }

            btn_start.setOnClickListener {
                back_btn.setBackgroundResource(R.drawable.btn_back_w)
                Handler().postDelayed({ start_view.visibility = INVISIBLE }, 100L)
                isPlay = true
                videoview.start()

                mServiceManager?.sendMessage(
                    "api-command=light&api-action=glass-transparency&api-method=post&sub-system-id=1",
                    "onoff=1"
                )

                mServiceManager?.sendMessage(
                    "api-command=status&api-action=welcome&api-method=post&sub-system-id=1",
                    ""
                )

                mServiceManager?.sendMessage(
                    "api-command=light&api-action=light&api-method=post&sub-system-id=1",
                    "type=C"
                )
//            ok_btn.visibility = INVISIBLE
            }

            test1.setOnClickListener {
//                mServiceManager?.sendMessage(
//                    "api-command=status&api-action=steering-position&api-method=post&sub-system-id=1",
//                    "direction=UL"
//                )
                mServiceManager?.sendMessage(
                    "api-command=ecorner&api-action=gear&api-method=post&sub-system-id=1",
                    "gear=P"
                )
            }

            test2.setOnClickListener {
//                mServiceManager?.sendMessage(
//                    "api-command=status&api-action=steering-position&api-method=post&sub-system-id=1",
//                    "direction=LL"
//                )
                mServiceManager?.sendMessage(
                    "api-command=ecorner&api-action=gear&api-method=post&sub-system-id=1",
                    "gear=R"
                )
            }

            test3.setOnClickListener {
//                mServiceManager?.sendMessage(
//                    "api-command=status&api-action=steering-position&api-method=post&sub-system-id=1",
//                    "direction=UR"
//                )
                mServiceManager?.sendMessage(
                    "api-command=ecorner&api-action=gear&api-method=post&sub-system-id=1",
                    "gear=N"
                )
            }

            test4.setOnClickListener {
//                mServiceManager?.sendMessage(
//                    "api-command=status&api-action=steering-position&api-method=post&sub-system-id=1",
//                    "direction=LR"
//                )
                mServiceManager?.sendMessage(
                    "api-command=ecorner&api-action=gear&api-method=post&sub-system-id=1",
                    "gear=D"
                )
            }

//        startService()

            Handler().postDelayed({
                videoview.start()
                mServiceManager?.sendMessage(
                    "api-command=light&api-action=light&api-method=post&sub-system-id=1",
                    "type=B"
                )
            }, 500L)

            index = 0
        }

        MobisApplication.isAttach = false
    }

    private fun appSettingCompleted() {
        app1 = selectApp1.toLowerCase(Locale.getDefault())
        app2 = selectApp2.toLowerCase(Locale.getDefault())
        if (app1 == "navigation") {
            app1 = "navi"
        }
        if (app2 == "navigation") {
            app2 = "navi"
        }

        iconResId = resources.getIdentifier("${app1}_$app2", "raw", packageName)
        videoview.setVideoPath(path + iconResId)

        mServiceManager?.sendMessage(
            "api-command=setting&api-action=display-app&api-method=post&sub-system-id=1",
            "cluster=theme1&app1=$app1&app2=$app2"
        )
    }

    private fun changeStep() {
        when(index) {
            1 -> {
                thum_image.visibility = INVISIBLE
                first_step.visibility = INVISIBLE
                second_step.visibility = INVISIBLE
                back_btn.visibility = INVISIBLE
                videoview.setVideoPath(path + R.raw.intro2)
                videoview.start()
                Handler().postDelayed({ videoview.pause() }, 100L)
                ok_btn.setTextColor(Color.parseColor("#ffffff"))
            }
            2 -> {
                thum_image.visibility = INVISIBLE
                first_step.visibility = VISIBLE
                second_step.visibility = INVISIBLE
                third_step.visibility = INVISIBLE
                videoview.setVideoPath(path + R.raw.intro3)
                videoview.start()
                Handler().postDelayed({ videoview.pause() }, 100L)
                ok_btn.setTextColor(Color.parseColor("#7a56ff"))
                back_btn.setBackgroundResource(R.drawable.btn_cluster_back)
            }
            3 -> {
                thum_image.setBackgroundResource(R.drawable.cintro3)
                thum_image.visibility = VISIBLE
                second_step.visibility = VISIBLE
                third_step.visibility = INVISIBLE
                ok_btn.setTextColor(Color.parseColor("#c2a000"))
                back_btn.setBackgroundResource(R.drawable.btn_navi_back)
            }
            4 -> {
                thum_image.setBackgroundResource(R.drawable.cintro4)
                thum_image.visibility = VISIBLE
                third_step.visibility = VISIBLE
                ok_btn.visibility = VISIBLE
                ok_btn.setTextColor(Color.parseColor("#cc1a2e"))
                back_btn.setBackgroundResource(R.drawable.btn_call_back)
                videoview.setVideoPath(path + R.raw.intro4)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            Log.d(logTag, "Permission: " + permissions[0] + "was " + grantResults[0])
        }
    }

//    @SuppressLint("SimpleDateFormat")
//    private fun createImageFile(): File? {
//        // Create an image file name
//        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        return File.createTempFile(
//            "JPEG_${timeStamp}_", /* prefix */
//            ".jpg", /* suffix */
//            storageDir /* directory */
//        ).apply {
//            // Save a file: path for use with ACTION_VIEW intents
//            mCurrentPhotoPath = absolutePath
//        }
//    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        action = intent.action

        if (action == "End") {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e(logTag, "onResume")
        videoview.visibility = VISIBLE
        thum_image.visibility = VISIBLE
        when (index) {
            1 -> {
                thum_image.setBackgroundResource(R.drawable.citro1)
            }
            2 -> {
                thum_image.setBackgroundResource(R.drawable.citro2)
            }
            3 -> {
                thum_image.setBackgroundResource(R.drawable.cintro3)
            }
            4 -> {
                thum_image.setBackgroundResource(R.drawable.cintro4)
            }
            5 -> {

            }
        }

        isSettingView = true
    }

    override fun onPause() {
        super.onPause()
        Log.e(logTag, "onPause")

        videoview.pause()
    }

//    private fun loadImage() {
//        val intent = Intent(Intent.ACTION_PICK)
//        intent.type = "image/*"
//        intent.action = Intent.ACTION_OPEN_DOCUMENT
//        intent.addCategory(Intent.CATEGORY_OPENABLE)
//
//        startActivityForResult(Intent.createChooser(intent, "Load Picture"), gallery)
//    }

//    private fun dispatchTakePictureIntent() {
//        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
//            takePictureIntent.resolveActivity(packageManager)?.also {
//                val photoFile: File? = try {
//                    createImageFile()
//                } catch (ex: IOException) {
//                    // Error occurred while creating the File
//
//                    null
//                }
//                // Continue only if the File was successfully created
//                photoFile?.also {
//                    val photoURI: Uri = FileProvider.getUriForFile(
//                        this,
//                        "com.anyractive.medroa.ev.pop.fileprovider",
//                        it
//                    )
//
//                    imageUri = photoURI
//
//                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
//                }
//            }
//        }
//    }

//    private fun dispatchTakePictureIntent() {
//        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        if (takePictureIntent.resolveActivity(packageManager) != null) {
//            var photoFile: File? = null
//            try {
//                photoFile = createImageFile()
//            } catch (ex: IOException) {
//            }
//            if (photoFile != null) {
//                val photoURI: Uri =
//                    FileProvider.getUriForFile(
//                        this,
//                        "com.anyractive.medroa.mobisapplication.fileprovider",
//                        photoFile
//                    )
//
//                imageUri = photoURI
//                takePictureIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
//                takePictureIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
//                takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
//            }
//        }
//    }

    //사진 자르기
    private fun cropImage() {
        CropImage.activity(imageUri).setGuidelines(CropImageView.Guidelines.ON).start(this)
    }

//    private fun galleryAddPic() {
//        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
//        val f = File(mCurrentPhotoPath)
//        val contentUri = Uri.fromFile(f)
//        mediaScanIntent.data = contentUri
//        this.sendBroadcast(mediaScanIntent)
//    }

//    private fun launchImageCrop(uri: Uri?){
//        CropImage.activity(uri).setGuidelines(CropImageView.Guidelines.ON)
//            .setCropShape(CropImageView.CropShape.RECTANGLE)
//            .start(this)
//    }

//    private fun startService() {
//        serviceClass = ServiceManager::class.java
//        val intent = Intent(applicationContext, serviceClass)
//        intent.action = "Start"
//
//        if (!isServiceRunning(serviceClass as Class<*>)) {
//            startForegroundService(intent)
//        } else {
////            toast("Service already running.")
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("servicesTest", "SettingActivity onDestroy")
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                // permission granted...
                Log.i(logTag, "permission granted")
            } else {
                // permission not granted...
                Log.i(logTag, "permission not granted")
            }
        }
//        else if (requestCode == gallery) {
//            galleryAddPic()
//        }
        else if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {
                cropImage()
            }
        } else if (requestCode == CROP_FROM_CAMERA) {
            if (resultCode == RESULT_OK) {
//                val file = File(mCurrentPhotoPath)
//                var bitmap: Bitmap
                if (Build.VERSION.SDK_INT >= 29) {
//                    val source = ImageDecoder.createSource(contentResolver, Uri.fromFile(file))
                    try {
//                        bitmap = ImageDecoder.decodeBitmap(source);
//                        if (bitmap != null) {
//                            image_view.setImageBitmap(bitmap)
//                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    try {
//                        bitmap = MediaStore.Images.Media.getBitmap(
//                            getContentResolver(), Uri.fromFile(
//                                file
//                            )
//                        )
//                        if (bitmap != null) {
//                            image_view.setImageBitmap(bitmap)
//                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                CropImage.activity(imageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this)

//                Handler().postDelayed({
//                    data?.data?.let { uri ->
//                        launchImageCrop(uri)
//                    }
//
//                    Log.d(TAG, "data : " + data)
//
//
//                    // 카메라로부터 받은 데이터가 있을경우에만
//                    if (mCurrentPhotoPath == null) {
//                        try {
//                            Log.d(TAG, "2222")
////                            createImageFile()
//                        } catch (ex: IOException) {
//                            null
//                        }
//                    }
//                    val file = File(mCurrentPhotoPath)
//                    val selectedUri = Uri.fromFile(file)
//                    try{
//                        selectedUri?.let {
//                            Log.d(TAG, "3333")
//                            if (Build.VERSION.SDK_INT < 28) {
//                                Log.d(TAG, "4444")
//                                val bitmap = MediaStore.Images.Media
//                                    .getBitmap(contentResolver, selectedUri)
//                                launchImageCrop(selectedUri)
//                            }
//                            else{
//                                Log.d(TAG, "contentResolver : " + contentResolver + " / selectedUri : " + selectedUri)
//                                val decode = ImageDecoder.createSource(this.contentResolver,
//                                    selectedUri)
//
//                                try {
//                                    val bitmap = ImageDecoder.decodeBitmap(decode);
//                                    if (bitmap != null) {
//                                        launchImageCrop(selectedUri)
//                                        image_view.setImageBitmap(bitmap)
//                                    }
//                                } catch (e: IOException) {
//                                    e.printStackTrace()
//                                }
//                            }
//                        }
//
//                    }catch (e: java.lang.Exception){
//                        e.printStackTrace()
//                    }
//                }, 1000L)
            }
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE ) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                if (result != null) {
                    result.uri?.let {
                        image_view.setImageURI(result.uri)
                    }
                }
            } else if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                val error = result?.error
                if (error != null) {
                    Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
                }
            }
        } else if(requestCode == cluster) {
            if (resultCode == RESULT_OK) {
                setting_cluster.isSelected = true
                val type = data?.getStringExtra("type")
                mServiceManager?.sendMessage(
                    "api-command=setting&api-action=cluster&api-method=post&sub-system-id=1",
                    "info-type=$type"
                )

            }
        } else if(requestCode == settingApp1) {
            if (resultCode == RESULT_OK) {
                setting_app1.isSelected = true
                data?.let { setData(app1, it) }
            }
        } else if(requestCode == settingApp2) {
            if (resultCode == RESULT_OK) {
                setting_app2.isSelected = true
                data?.let { setData(app2, it) }
            }
        }
    }

    private fun setData(type: String, data: Intent) {
        when (type) {
            "navi" -> {
                setNaviData(data)
            }
            "media" -> {
                setMediaData(data)
            }
            "climate" -> {
                data.run { setClimateData(this@SettingActivity, this) }
            }
            "phone" -> {
                setPhoneData(data)
            }
            "setting" -> {
                setSettingData(data)
            }
        }
    }

    private fun setNaviData(data: Intent) {
        val destination = data.getStringExtra("destination")
        val route = data.getStringExtra("route")

        mServiceManager?.sendMessage(
            "api-command=setting&api-action=navi&api-method=post&sub-system-id=1",
            "destination=$destination&route=$route"
        )
    }

    private fun setMediaData(data: Intent) {
        val type = data.getStringExtra("type")
        val value = data.getStringExtra("value")
        val repeat = data.getStringExtra("repeat")
        val maximize = data.getStringExtra("maximize")

        mServiceManager?.sendMessage(
            "api-command=setting&api-action=media-player&api-method=post&sub-system-id=1",
            "type=$type&value=$value&repeat-status=$repeat&maximize-onoff=$maximize"
        )
    }

    private fun setPhoneData(data: Intent) {
        val type = data.getStringExtra("type")

        mServiceManager?.sendMessage(
            "api-command=setting&api-action=phone&api-method=post&sub-system-id=1",
            "type=$type"
        )
    }

    private fun setSettingData(data: Intent) {
        val type = data.getStringExtra("type")

        mServiceManager?.sendMessage(
            "api-command=setting&api-action=text-content&api-method=post&sub-system-id=1",
            "type=$type"
        )
    }

    private fun showPopup() {
        // 다이얼로그 바디
//        val dlg: AlertDialog.Builder = AlertDialog.Builder(
//            this,
//            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth
//        )
//        dlg.setTitle("Ev Pop") //제목
//        dlg.setMessage("Set different apps on each button") // 메시지
//        dlg.setPositiveButton("Ok") { _, _ ->
//
//        }
//        dlg.show()

        val d = Dialog(this)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setContentView(R.layout.alertdialog) // custom layour for dialog.

        val okBtn = d.findViewById(R.id.pop_ok_btn) as Button
        okBtn.setOnClickListener {
            d.dismiss()
        }
        d.show()
    }

//    fun backBtnClick() {
//        viewpager_view.visibility = INVISIBLE
//        ok_btn.visibility = VISIBLE
//        btn_start.visibility = INVISIBLE
//        thum_image.setBackgroundResource(R.drawable.cintro4)
//        thum_image.visibility = VISIBLE
//        third_step.visibility = VISIBLE
//    }

    fun skipBtnClick() {
        start_view.visibility = VISIBLE
        viewpager_view.visibility = INVISIBLE
        ok_btn.visibility = INVISIBLE
        btn_ok.setBackgroundResource(R.drawable.btn_coach_skip)
        isPlay = true
        videoview.start()
    }

    companion object {
        private fun setClimateData(settingActivity: SettingActivity, data: Intent) {
            val power = data.getStringExtra("power")
            val valve = data.getStringExtra("valve")
            val speed = data.getStringExtra("speed")
            val hotwire = data.getStringExtra("hotwire")
            val temp = data.getStringExtra("temp")
            val mode = data.getStringExtra("mode")
            val ac = data.getStringExtra("ac")

            settingActivity.mServiceManager?.sendMessage(
                "api-command=setting&api-action=climate&api-method=post&sub-system-id=1",
                "power-onoff=$power&valve=$valve&wind=$speed&hotwire-onoff=$hotwire&temp=$temp&mode=$mode&ac-onoff=$ac"
            )
        }

        const val REQUEST_OVERLAY_PERMISSION = 0
        const val REQUEST_TAKE_PHOTO = 1
        const val CROP_FROM_CAMERA = 2
        const val REQUEST_IMAGE_CAPTURE = 3
    }
}

class MyGesture : GestureDetector.OnGestureListener {

    // 제스처 이벤트를 받아서 text를 변경
    override fun onShowPress(e: MotionEvent?) {

    }
    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        settingview.visibility = INVISIBLE
        videoview.visibility = VISIBLE
//        videoview.start()
        val ip = editIp.text.toString()
        val port = editPort.text.toString()
        MobisApplication.prefs.setString("ip", ip)
        MobisApplication.prefs.setString("port", port)
        return true
    }
    override fun onDown(e: MotionEvent?): Boolean {

        return true
    }
    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {

        return true
    }
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {

        return true
    }
    override fun onLongPress(e: MotionEvent?) {
        isLongClick = true
    }
}

class MyDoubleGesture : GestureDetector.OnDoubleTapListener
{

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        if (isLongClick) {
            settingview.visibility = VISIBLE
            videoview.pause()
            videoview.visibility = INVISIBLE
            isLongClick = false

            val serviceClass = ServiceManager::class.java
            val intent = Intent(context, serviceClass)
            if (isServiceRunning(serviceClass)) {
                // Start the service
                context.stopService(intent)
            }
        }
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {

        return true
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        // Loop through the running services
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {

            if (serviceClass.name == service.service.className) {
                // If the service is running then return true
                return true
            }
        }
        return false
    }
}

private class ScreenSlidePagerAdapter(fa: FragmentActivity, val type : String) : FragmentStateAdapter(fa) {
//    private var type = "MEDIA"
//
//    fun setType(type : String) {
//        this.type = type
//    }
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> ImageSlideFragment(R.drawable.bg_coach_mark_1, position, type)
//            1 -> ImageSlideFragment(R.drawable.bg_coach_mark_3, position, type)
            else -> ImageSlideFragment(R.drawable.bg_coach_mark_3, position, type)
        }
    }
}