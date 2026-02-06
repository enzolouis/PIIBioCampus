package com.example.piibiocampus

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.piibiocampus.databinding.ActivityPictureBinding
import com.example.piibiocampus.utils.ImageUtils.resizeAndCompress
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
class PictureActivity  : AppCompatActivity() {
    private lateinit var viewBinding: ActivityPictureBinding


    private lateinit var cameraController : LifecycleCameraController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityPictureBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        askCameraPermission()

        viewBinding.btnTakePicture.setOnClickListener {
            takePhoto()
        }

    }

    private fun startCamera() {
        Log.d("CAMERA", "lancement de la caméra")
        val previewView: PreviewView = viewBinding.viewFinder
        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraController.setEnabledUseCases(
           CameraController.IMAGE_CAPTURE
        )
        previewView.controller = cameraController
    }

    private fun takePhoto() {
        cameraController.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CAMERA", "La photo a échoué: ${exc.message}", exc)
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()

                    // 🔹 Resize + compress avant de passer à l'activité suivante
                    val compressedBytes = resizeAndCompress(bytes, maxWidth = 1080, maxHeight = 1920, quality = 80)

                    // 🔹 Passer le ByteArray compressé ou mieux : sauvegarder dans un fichier temporaire
                    val intent = Intent(this@PictureActivity, PreviewPictureActivity::class.java)
                    intent.putExtra("imageBytes", compressedBytes)
                    startActivity(intent)
                }
            }
        )
    }


    private fun askCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            startCamera()
        }
    }

    // fonction appellé automatiquement après askCameraPermission
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startCamera()
            } else {
                Log.d("CAMERA", "Permission refusée")
                finish()
            }
        }
    }

}