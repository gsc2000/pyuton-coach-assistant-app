package com.example.swimminganalysisapplication

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import org.opencv.android.OpenCVLoader
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio
import org.opencv.core.Mat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.ArrayList

private const val TAG = "VideoProcessingService"

class VideoProcessingService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var isOpenCvInitialized = false // OpenCV初期化状態フラグ

    // 動画処理リクエストを保持するデータクラスとキュー
    private data class VideoRequest(val videoUri: Uri, val startId: Int)
    private val pendingRequests = mutableListOf<VideoRequest>()

    // BaseLoaderCallback と LoaderCallbackInterface は initLocal() を使うため不要

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate.")
        initializeOpenCV() // onCreateでOpenCVを初期化
    }

    private fun initializeOpenCV() {
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully (initLocal).")
            isOpenCvInitialized = true
            processPendingRequests() // 初期化が成功したら保留中のリクエストを処理
        } else {
            Log.e(TAG, "OpenCV (initLocal) failed! Trying initDebug().")
            // initLocalが失敗した場合、開発中に詳細なログが出力されるinitDebugを試す
            if (OpenCVLoader.initDebug()) {
                Log.i(TAG, "OpenCV loaded successfully (initDebug).")
                isOpenCvInitialized = true
                processPendingRequests() // 初期化が成功したら保留中のリクエストを処理
            } else {
                Log.e(TAG, "OpenCV (initDebug) failed!")
                isOpenCvInitialized = false
                // OpenCVの初期化に失敗した場合、保留中のリクエストを全てキャンセルし、サービスを停止
                synchronized(pendingRequests) {
                    pendingRequests.forEach { stopSelf(it.startId) }
                    pendingRequests.clear()
                }
                stopSelf() // サービス自体も停止させる
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received. Start ID: $startId")

        val videoUriString = intent?.getStringExtra(EXTRA_VIDEO_URI)
        if (videoUriString == null) {
            Log.e(TAG, "Video URI not provided in intent for startId: $startId.")
            stopSelf(startId) // URIがなければこのstartIdのリクエストは終了
            return START_NOT_STICKY
        }
        val videoUri = Uri.parse(videoUriString)

        if (isOpenCvInitialized) {
            Log.d(TAG, "OpenCV is initialized. Processing video URI: $videoUri for startId: $startId")
            serviceScope.launch {
                processVideo(videoUri, startId)
            }
        } else {
            Log.d(TAG, "OpenCV not initialized yet. Queuing video URI: $videoUri for startId: $startId")
            synchronized(pendingRequests) {
                pendingRequests.add(VideoRequest(videoUri, startId))
            }
            // initializeOpenCV() は onCreate で既に呼ばれている。
            // もしこのタイミングで再度初期化を試みたい場合は、initializeOpenCV()を呼ぶか、
            // initLocal()が失敗した場合に備えたロジックを検討する必要があるが、
            // 通常はonCreateでの一度の試みで十分。
        }

        return START_NOT_STICKY // 処理が終わったらサービスは停止してほしいのでNOT_STICKY
    }

    private fun processPendingRequests() {
        synchronized(pendingRequests) {
            if (pendingRequests.isNotEmpty()) {
                Log.i(TAG, "Processing ${pendingRequests.size} pending video requests.")
                val requestsToProcess = ArrayList(pendingRequests) // イテレーション中の変更を避けるためコピー
                pendingRequests.clear()

                requestsToProcess.forEach { request ->
                    Log.d(TAG, "Processing queued video URI: ${request.videoUri} for startId: ${request.startId}")
                    serviceScope.launch {
                        processVideo(request.videoUri, request.startId)
                    }
                }
            } else {
                Log.d(TAG, "No pending video requests to process.")
            }
        }
    }

    private fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to get InputStream from URI: $uri")
                return null
            }
            // ファイル拡張子をMIMEタイプから取得しようと試みる
            val extension = context.contentResolver.getType(uri)?.substringAfterLast('/') ?: "mp4"
            val tempFile = File.createTempFile("video_", ".${extension}", context.cacheDir)
            tempFile.deleteOnExit() // JVM終了時に削除されるようにマーク

            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            Log.d(TAG, "URI copied to temporary file: ${tempFile.absolutePath}")
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to temp file: ${e.message}", e)
            null
        }
    }

    private suspend fun processVideo(videoUri: Uri, startId: Int) {
        Log.d(TAG, "Starting video processing for URI: $videoUri (startId: $startId)")
        if (!isOpenCvInitialized) {
            Log.e(TAG, "OpenCV not initialized during processVideo call. Aborting for startId: $startId.")
            stopSelf(startId)
            return
        }

        var tempFile: File? = null
        try {
            tempFile = copyUriToTempFile(this, videoUri)

            if (tempFile != null && tempFile.exists()) {
                Log.d(TAG, "Video copied to temporary file: ${tempFile.absolutePath}")
                val capture = VideoCapture()
                val filePath = tempFile.absolutePath
                if (capture.open(filePath)) {
                    val frameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT)
                    val fps = capture.get(Videoio.CAP_PROP_FPS)
                    val width = capture.get(Videoio.CAP_PROP_FRAME_WIDTH)
                    val height = capture.get(Videoio.CAP_PROP_FRAME_HEIGHT)
                    Log.i(TAG, "OpenCV: Video properties: $frameCount frames, $fps FPS, ${width}x${height}")

                    val mat = Mat()
                    var framesRead = 0
                    val maxFramesToTest = 5 // 元のコードのテストフレーム数を維持
                    for (i in 0 until maxFramesToTest) {
                        if (capture.read(mat)) {
                            framesRead++
                            Log.d(TAG, "OpenCV: Successfully read frame ${i + 1}. Mat empty: ${mat.empty()}, size: ${mat.size()}")
                        } else {
                            Log.w(TAG, "OpenCV: Failed to read frame ${i + 1}")
                            break
                        }
                    }
                    Log.i(TAG, "OpenCV: Attempted to read $maxFramesToTest frames, successfully read $framesRead frames.")
                    if (!mat.empty()) {
                        mat.release()
                    }

                    capture.release()
                    Log.i(TAG, "OpenCV: VideoCapture released.")
                } else {
                    Log.e(TAG, "OpenCV: Could not open video capture for $filePath")
                }
            } else {
                Log.e(TAG, "Failed to copy URI to a temporary file, or temp file is null/does not exist for OpenCV processing.")
            }

            Log.i(TAG, "Video processing finished for URI: $videoUri (startId: $startId)")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing video for startId $startId: ${e.message}", e)
        } finally {
            tempFile?.let {
                if (it.exists()) {
                    if (it.delete()) {
                        Log.d(TAG, "Temporary file deleted: ${it.absolutePath}")
                    } else {
                        Log.w(TAG, "Failed to delete temporary file: ${it.absolutePath}")
                    }
                }
            }
            Log.d(TAG, "Stopping service for startId: $startId")
            stopSelf(startId) // このリクエストに対応するサービスインスタンスを停止
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // クライアントがこのサービスにバインドすることは想定していないためnullを返す
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed. Cancelling serviceJob.")
        serviceJob.cancel() // コルーチンスコープをキャンセル
    }

    companion object {
        const val EXTRA_VIDEO_URI = "com.example.swimminganalysisapplication.EXTRA_VIDEO_URI"

        fun startService(context: Context, videoUri: Uri) {
            val intent = Intent(context, VideoProcessingService::class.java).apply {
                putExtra(EXTRA_VIDEO_URI, videoUri.toString())
                // Intent.FLAG_GRANT_READ_URI_PERMISSION は、ContentResolver を使用してURIにアクセスする際に
                // Activity側でContext.startActivityまたはContext.startServiceのIntentに付与することで一時的な権限を与えるものです。
                // Service側でこのフラグを立てる必要は通常ありませんが、呼び出し元で適切に設定されていれば問題ありません。
                // 今回は元のコードの呼び出し側に任せる形とします。
            }
            context.startService(intent)
        }
    }
}
