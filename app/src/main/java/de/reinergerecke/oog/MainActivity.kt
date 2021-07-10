package de.reinergerecke.oog

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Camera
import android.hardware.camera2.CameraManager
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowInsets

private const val PERMISSIONS_REQUEST_CODE = 10
private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)


class MainActivity : AppCompatActivity() {
    private lateinit var cameraManager: CameraManager
    private lateinit var preview: SurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.insetsController?.hide(WindowInsets.Type.statusBars())

        preview = findViewById(R.id.preview)
        preview.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) = Unit

            override fun surfaceCreated(holder: SurfaceHolder) {
                holder.setFixedSize(1920, 200)
                requestPermissions(
                    PERMISSIONS_REQUIRED,
                    PERMISSIONS_REQUEST_CODE
                )
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera()
            } else {
                Log.e(TAG, "No permissions.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initializeCamera() {
        cameraManager = applicationContext.getSystemService(CAMERA_SERVICE) as CameraManager

        // TODO Don't hardcode this
        val cameraId = "1"
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                Log.d(TAG, "Device $device")
                startCamera(device)
            }

            override fun onDisconnected(device: CameraDevice) {
                Log.w(TAG, "Camera $cameraId has been disconnected")
            }

            override fun onError(device: CameraDevice, error: Int) {
                val exc = RuntimeException("Camera $cameraId error: ($error)")
                Log.e(TAG, exc.message, exc)
            }
        }, null)
    }

    private fun startCamera(device: CameraDevice) {
        device.createCaptureSession(
            SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(OutputConfiguration(preview.holder.surface)),
                applicationContext.mainExecutor,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        val request =
                            session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                .apply {
                                    addTarget(preview.holder.surface)
                                }.build()

                        session.setRepeatingRequest(request, null, null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        val exc =
                            RuntimeException("Camera ${device.id} session configuration failed")
                        Log.e(TAG, exc.message, exc)
                    }

                })

        )
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}