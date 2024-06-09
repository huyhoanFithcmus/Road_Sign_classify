package org.tensorflow.lite.examples.classification

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.database.database
import com.google.firebase.firestore.firestore
import org.tensorflow.lite.examples.classification.ml.ModelMbn1
import org.tensorflow.lite.examples.classification.model.SpeedData
import org.tensorflow.lite.examples.classification.ui.RecognitionAdapter
import org.tensorflow.lite.examples.classification.util.YuvToRgbConverter
import org.tensorflow.lite.examples.classification.viewmodel.Recognition
import org.tensorflow.lite.examples.classification.viewmodel.RecognitionListViewModel
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.model.Model
import java.util.Date
import java.util.concurrent.Executors


private const val MAX_RESULT_DISPLAY = 3
private const val TAG = "RS detection"
private const val REQUEST_CODE_PERMISSIONS = 999
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

typealias RecognitionListener = (recognition: List<Recognition>) -> Unit

class MainActivity : AppCompatActivity() {

    private lateinit var preview: Preview
    private lateinit var imageAnalyzer: ImageAnalysis
    private lateinit var camera: Camera
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private val recogViewModel: RecognitionListViewModel by viewModels()

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private var currentSpeed = 0.0

    private var speedInDouble = 0.0

    private val THRESHOLD = 10.0


    private lateinit var outputTextView: TextView

    private var currentLocation: Location? = null

    private lateinit var viewTrafficDetectButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        outputTextView = findViewById(R.id.outputTextView)
        viewTrafficDetectButton = findViewById(R.id.viewSpeedDataButton)

        if (allPermissionsGranted()) {
            startCamera()
            requestLocationPermission()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        val viewAdapter = RecognitionAdapter(this)
        findViewById<RecyclerView>(R.id.recognitionResults).adapter = viewAdapter
        findViewById<RecyclerView>(R.id.recognitionResults).itemAnimator = null

        recogViewModel.recognitionList.observe(this, Observer {
            viewAdapter.submitList(it)
        })

        // Thêm sự kiện cho nút chuyển đến MapsFragment
//        val openMapsButton = findViewById<Button>(R.id.openMapsButton)
//        openMapsButton.setOnClickListener {
//            val mapsFragment = MapsFragment2()
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.fragmentContainer, mapsFragment)
//                .addToBackStack(null)
//                .commit()
//        }

//        val viewTrafficDetectButton = findViewById<Button>(R.id.viewSpeedDataButton)
//        viewTrafficDetectButton.setOnClickListener {
//            val speedDataFragment = SpeedDataFragment()
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.fragmentContainer, speedDataFragment)
//                .addToBackStack(null)
//                .commit()
//        }

    }

    private fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
                requestLocationPermission()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permission_deny_text),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(
                    this,
                    "Ứng dụng cần quyền truy cập vị trí để xác định tốc độ.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder()
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(224, 224))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalyzer(currentSpeed,this@MainActivity) { items ->
                        recogViewModel.updateData(items)
                    })
                }

            val cameraSelector =
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
                    CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                preview.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startLocationUpdates() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                currentSpeed = location.speed * 3.6
                currentLocation = location // Cập nhật giá trị vị trí hiện tại
                updateSpeedUI()
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // Not needed for this example
            }

            override fun onProviderEnabled(provider: String) {
                // Not needed for this example
            }

            override fun onProviderDisabled(provider: String) {
                // Not needed for this example
            }
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                locationListener
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Error requesting location updates: ${e.message}")
        }
    }

    private fun updateSpeedUI() {
        handler.post {
            findViewById<TextView>(R.id.speedTextView).text = "Speed: ${currentSpeed} km/h"
        }
    }

    private fun updateOutputText(output: String) {
        handler.post {
            outputTextView.text = output
        }
    }

    private class ImageAnalyzer(
        private var currentSpeed: Double,
        private val mainActivity: MainActivity,
        private val listener: RecognitionListener
    ) : ImageAnalysis.Analyzer {

        private val flowerModel: ModelMbn1 by lazy {
            val compatList = CompatibilityList()

            val options = if (compatList.isDelegateSupportedOnThisDevice) {
                Log.d(TAG, "This device is GPU Compatible ")
                Model.Options.Builder().setDevice(Model.Device.GPU).build()
            } else {
                Log.d(TAG, "This device is GPU Incompatible ")
                Model.Options.Builder().setNumThreads(4).build()
            }

            ModelMbn1.newInstance(mainActivity, options)
        }

        override fun analyze(imageProxy: ImageProxy) {
            val items = mutableListOf<Recognition>()

            val tfImage = TensorImage.fromBitmap(toBitmap(imageProxy))

            val outputs = flowerModel.process(tfImage)
                .probabilityAsCategoryList.apply {
                    sortByDescending { it.score }
                }.take(MAX_RESULT_DISPLAY)

            var doubleSpeed = 60.0
            var hasScoreOverThreshold = false

            val database = Firebase.database

            val speedRef = database.getReference("Speed")

            val speed = outputs[0].label.substring(0, outputs[0].label.indexOf("K"))

            if (outputs[0].score > 0.8) {
                doubleSpeed = speed.toDouble()
                speedRef.setValue(doubleSpeed)

                val currentTimeStamp = Timestamp(Date())
                val currentLocation = mainActivity.currentLocation
                val currentLatLng = currentLocation?.let { LatLng(it.latitude, it.longitude) }

                if (currentLatLng != null) {
                    val speedDataId = "speed_${doubleSpeed}_kmh" // Thay thế dấu '/' bằng ''
                    val speedData = SpeedData(doubleSpeed, currentTimeStamp, currentLatLng)

                    val db = Firebase.firestore
                    db.collection("speedData")
                        .document(speedDataId)
                        .set(speedData)
                        .addOnSuccessListener {
                            Log.d(TAG, "Speed data added with ID: $speedDataId")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error adding speed data", e)
                        }
                }



                hasScoreOverThreshold = true
            } else if (!hasScoreOverThreshold) {
                doubleSpeed = speed.toDouble()
            }


            val outputTextView = mainActivity.findViewById<TextView>(R.id.outputTextView)
            if (currentSpeed < doubleSpeed ) {
                outputTextView.setBackgroundColor(
                    ContextCompat.getColor(mainActivity, R.color.speed_limit_color)
                )
                mainActivity.updateOutputText("lower than speed limit " + doubleSpeed + " km/h")
            }
            else
            {
                outputTextView.setBackgroundColor(
                    ContextCompat.getColor(mainActivity, R.color.speed_exceed_color)
                )
                mainActivity.updateOutputText("Over the speed limit, please slow down below " + doubleSpeed + " km/h")
            }

            for (output in outputs) {
                items.add(Recognition(output.label, output.score))
            }

            listener(items.toList())

            imageProxy.close()
        }


        private val yuvToRgbConverter = YuvToRgbConverter(mainActivity)
        private lateinit var bitmapBuffer: Bitmap
        private lateinit var rotationMatrix: Matrix

        @SuppressLint("UnsafeExperimentalUsageError")
        private fun toBitmap(imageProxy: ImageProxy): Bitmap? {
            val image = imageProxy.image ?: return null

            if (!::bitmapBuffer.isInitialized) {
                Log.d(TAG, "Initialize toBitmap()")
                rotationMatrix = Matrix()
                rotationMatrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                bitmapBuffer = Bitmap.createBitmap(
                    imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
                )
            }

            yuvToRgbConverter.yuvToRgb(image, bitmapBuffer)

            return Bitmap.createBitmap(
                bitmapBuffer,
                0,
                0,
                bitmapBuffer.width,
                bitmapBuffer.height,
                rotationMatrix,
                false
            )
        }
    }
}
