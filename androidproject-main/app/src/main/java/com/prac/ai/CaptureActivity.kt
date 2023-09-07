package com.prac.ai

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import androidx.camera.core.ImageCaptureException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import com.prac.ai.databinding.ActivityCaptureBinding
import java.io.ByteArrayInputStream
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.ListenerRegistration

class CaptureActivity : AppCompatActivity() {
    private var user: FirebaseUser? = null
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null
    private var uid:String? = null
    private var storage: FirebaseStorage? = null
    private var path:String? = null
    private lateinit var activityCaptureBinding: ActivityCaptureBinding
    private var imageCapture: ImageCapture? = null
    private var registration: ListenerRegistration? = null

    private lateinit var cameraExecutor: ExecutorService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityCaptureBinding = ActivityCaptureBinding.inflate(layoutInflater)
        user = Firebase.auth.currentUser!!
        auth = Firebase.auth
        db = Firebase.firestore
        uid = user!!.uid
        storage = Firebase.storage
        setContentView(activityCaptureBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        activityCaptureBinding.imageCaptureButton.setOnClickListener(){
            takePhoto()
        }
        //        // Set up the listeners for take photo and video capture buttons

        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(activityCaptureBinding.viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.KOREAN)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"

                    val savedUri = output.savedUri ?: return
                    val contentResolver = baseContext.contentResolver
                    val inputStream = contentResolver.openInputStream(savedUri)
                    val byteArray = inputStream?.readBytes()
                    inputStream?.close()  // Close the inputStream
                    byteArray ?: return

                    val fileName = savedUri.lastPathSegment ?: return
                    sendImageToServer(byteArray, fileName)

                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }
    private fun sendImageToServer(imageData: ByteArray, fileName: String) {
        var storeRef = storage!!.reference
        path = "${uid}/images"
        var userImageRef = storeRef.child(path!!)
        val byteArrayInputStream = ByteArrayInputStream(imageData)
        var uploadTask = userImageRef.putStream(byteArrayInputStream)
        uploadTask.addOnFailureListener {
            Toast.makeText(this@CaptureActivity, "$title: 사진업로드실패", Toast.LENGTH_SHORT).show()
            // Handle unsuccessful uploads
            finish()
        }.addOnSuccessListener { taskSnapshot ->
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            // ...
            Toast.makeText(this@CaptureActivity, "$title: 사진업로드성공", Toast.LENGTH_SHORT).show()
            val data = hashMapOf(
                "uid" to uid!!
            )
            db!!.collection("User").document(uid!!).collection("Capture").add(data).addOnSuccessListener { documentReference ->
                val docRef = db!!.collection("User").document(uid!!).collection("Medication").document(/*특정 문서 ID*/)
                registration = docRef.addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        return@addSnapshotListener

                    }

                    if (snapshot != null && snapshot.exists()) {
                        Log.d(TAG, "Current data: ${snapshot.data}")
                        registration!!.remove()
                        finish()
                        // 여기에 문서 데이터가 업데이트될 때마다 수행할 로직을 작성합니다.
                    } else {
                        Log.d(TAG, "Current data: null")
                        registration!!.remove()
                        finish()
                    }
                }
                }

        }
    }



    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }



    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }


}