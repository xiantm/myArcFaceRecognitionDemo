package com.yryp.facehelper

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import com.arcsoft.facerecognition.AFR_FSDKFace
import com.arcsoft.facetracking.AFT_FSDKEngine
import com.arcsoft.facetracking.AFT_FSDKError
import com.arcsoft.facetracking.AFT_FSDKFace
import com.guo.android_extend.java.ExtByteArrayOutputStream
import com.guo.android_extend.widget.CameraFrameData
import com.guo.android_extend.widget.CameraGLSurfaceView
import com.guo.android_extend.widget.CameraSurfaceView
import kotlinx.coroutines.experimental.*
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * 宽高比必须为相机预览宽高比，不然有黑边
 */
class FaceCameraView : RelativeLayout, CameraSurfaceView.OnCameraListener {


    private val mCameraGLSurfaceView: CameraGLSurfaceView = CameraGLSurfaceView(context)
    private val mCameraSurfaceView: CameraSurfaceView = CameraSurfaceView(context)
    private val showFaceImageView: ImageView = ImageView(context)
    private val mSquareView = SquareScanView(context, 1f)

    //人脸跟踪引擎
    private val traceEngine = AFT_FSDKEngine()

    private var mCamera: Camera? = null
    private var screenRotation = 0
    //人脸有效范围
    private var validLeft = 0
    private var validRight = 640
    private var validTop = 0
    private var validBottom = 480
    //扫描人脸有效范围
    private var validScope = 1f

    // 捕获人脸时间，
    private var captureFaceTime = 1000L

    //是否可以匹配人脸的标识，当使用录入人脸时设置为false
    private val canSearch = AtomicBoolean(true)

    private val singleTask = Executors.newSingleThreadExecutor()

    //若设备处理太慢，过滤当前帧
    private val queue = LinkedBlockingDeque<Pair<ByteArray, List<AFT_FSDKFace>>>(1)

    private var onMatch: ((List<Pair<Face, Float>>) -> Unit)? = null


    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, def: Int = 0) : super(context, attrs, def) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.FaceCameraView)
        validScope = ta.getFloat(R.styleable.FaceCameraView_validScope, 1f)
        ta.recycle()
        mSquareView.n = validScope
        setValidateRect()
        val hideSf = ImageView(context)
        hideSf.setBackgroundColor(Color.BLACK)
        val sflp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        val glsflp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        val ivlp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        val svlp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        val afilp = RelativeLayout.LayoutParams(120, 120)
        afilp.setMargins(12, 12, 12, 12)
        addView(mCameraSurfaceView, sflp)
        addView(hideSf, ivlp)
        addView(mCameraGLSurfaceView, glsflp)
        addView(mSquareView, svlp)
        addView(showFaceImageView, afilp)
        val error = traceEngine.AFT_FSDK_InitialFaceEngine(FaceConfig.appid, FaceConfig.ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5)
        if (error.code != AFT_FSDKError.MOK) Toast.makeText(context, "人脸追踪初始化失败", Toast.LENGTH_SHORT).show()
        mCameraSurfaceView.setOnCameraListener(this)
        mCameraSurfaceView.setupGLSurafceView(mCameraGLSurfaceView, true, true, 0)
        mCameraSurfaceView.debug_print_fps(true, false)
        matchFaceLoop()

    }

    fun setOnMatch(onMatch: (List<Pair<Face, Float>>) -> Unit) {
        this.onMatch = onMatch
    }

    /***
     * 获取通过特征提取的人脸
     */
    fun getEncodeFace(callback: (ByteArray, Pair<List<AFR_FSDKFace>, List<Rect>>) -> Unit) {
        canSearch.set(false)
        var data = byteArrayOf()
        singleTask.submit {
            runBlocking {
                val afrFaceList = withTimeoutOrNull(captureFaceTime) {
                    repeat(10000) {
                        val (data1, aftFaceList) = queue.take()
                        data = data1
                        return@withTimeoutOrNull ArcFaceHelper.encodeFace(data1, aftFaceList, FaceConfig.previewWidth, FaceConfig.previewHeight, false)
                    }
                }
                afrFaceList?.let {
                    val list = it as Pair<List<AFR_FSDKFace>, List<Rect>>
                    if (list.first.isNotEmpty()) {
                        callback(data, list)
                    } else {
                        callback(data, Pair<List<AFR_FSDKFace>, List<Rect>>(listOf(), listOf()))
                    }
                } ?: callback(data, Pair<List<AFR_FSDKFace>, List<Rect>>(listOf(), listOf()))
                canSearch.set(true)
            }
        }
    }

    /**
     * 循环找人脸
     */
    private fun matchFaceLoop() {
        thread {
            while (true) {
                if (canSearch.get()) {
                    val (data1, aftFaceList) = queue.take()
                    val list = ArcFaceHelper.searchFace(data1, FaceConfig.previewWidth, FaceConfig.previewHeight, aftFaceList, 0.6f, false)
                    if (list.isEmpty()) {
                        Log.i("facemath", "找到${list.size}张脸")
                    } else {
                        Log.i("facemath", "找到${list.size}张脸")
                        onMatch?.let { it(list) }
                        for (item in list) {
                            Log.i("facemath", "score is ${item.second}")
                        }
                    }

                }
            }
        }
    }


    override fun startPreviewImmediately() = true

    override fun setupChanged(format: Int, width: Int, height: Int) = Unit

    override fun onBeforeRender(data: CameraFrameData?) = Unit


    override fun onPreview(data: ByteArray?, width: Int, height: Int, format: Int, timestamp: Long): Any {
        val aftResult = ArrayList<AFT_FSDKFace>()
        val err = traceEngine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, aftResult)

        //将通过人脸检测的帧拿去提取特征码
        if (aftResult.isNotEmpty()) {
            //只识别一个人
            val sortedAft = aftResult.filter { it.rect.left >= validLeft && it.rect.right <= validRight && it.rect.top >= validTop && it.rect.bottom <= validBottom }
            if (sortedAft.isNotEmpty()) {
                queue.offer(Pair(data!!, sortedAft), 0, TimeUnit.MILLISECONDS)
                val data = data
                val yuv = YuvImage(data, ImageFormat.NV21, width, height, null)
                val ops = ExtByteArrayOutputStream()
                yuv.compressToJpeg(sortedAft[0].rect, 60, ops)
                val bitmap = BitmapFactory.decodeByteArray(ops.byteArray, 0, ops.byteArray.size)
                val matrix = Matrix()
                matrix.setRotate(FaceConfig.rotate.toFloat())
                showFaceImageView.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false))
                val rects = arrayOfNulls<Rect>(sortedAft.size)
                for (i in sortedAft.indices) {
                    rects[i] = Rect(sortedAft[i].rect)
                }
                return rects
            }
        }
        showFaceImageView.setImageBitmap(null)
        return arrayOf<Rect>()
    }

    /**
     * 设置旋转角度
     */
    fun setCameraDisplayOrientation(activity: Activity) {
        screenRotation = activity.windowManager.defaultDisplay.rotation
    }

    override fun setupCamera(): Camera {

        if (Camera.getNumberOfCameras() == 0) {
            Toast.makeText(context, "未检测到摄像头,程序无法启动", Toast.LENGTH_SHORT).show()
        }
        var cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT
        try {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT)
        } catch (e: Exception) {
            cameraId = Camera.CameraInfo.CAMERA_FACING_BACK
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        }
        var degrees = 0
        val info = android.hardware.Camera.CameraInfo()
        android.hardware.Camera.getCameraInfo(cameraId, info)
        when (screenRotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }

        var displayOrientation: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayOrientation = (info.orientation + degrees) % 360
            mCameraGLSurfaceView.setRenderConfig(displayOrientation, true)
        } else {  // back-facing
            displayOrientation = (info.orientation - degrees + 360) % 360
            if (isTabletDevice(context)) displayOrientation = 0
            mCameraGLSurfaceView.setRenderConfig(displayOrientation, false)
        }
        FaceConfig.rotate = displayOrientation
        mCamera?.let {
            try {
                val parameters = it.parameters
                parameters.setPreviewSize(FaceConfig.previewWidth, FaceConfig.previewHeight)
                parameters.setPictureSize(FaceConfig.previewWidth, FaceConfig.previewHeight)
                parameters.previewFormat = ImageFormat.NV21
//                parameters.setPreviewFpsRange(5, 5)
//                parameters.setPreviewFpsRange(15000, 30000)
//                parameters.setRotation(0)
//                it.setDisplayOrientation(270)
                //曝光补偿 -3 到 +3
//                parameters.exposureCompensation = 1
                //支持 auto,incandescent,fluorescent,daylight,cloudy-daylight
//            parameters.whiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO
//            parameters.antibanding = Camera.Parameters.ANTIBANDING_AUTO
//            parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO # 只支持fixed
//            parameters.sceneMode = Camera.Parameters.SCENE_MODE_AUTO # 不支持场景
//            parameters.colorEffect = Camera.Parameters.EFFECT_NONE # 不支持色彩
                mCamera?.parameters = parameters
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return mCamera!!
    }

    //前置相机预览，保存不一致
    private fun setValidateRect() {
        var width = FaceConfig.previewWidth
        var height = FaceConfig.previewHeight
        var edgeWidth = (validScope * width).toInt()
        if (width > height) {
            edgeWidth = (validScope * height).toInt()
        }
        validLeft = (FaceConfig.previewWidth - edgeWidth) / 2
        validRight = FaceConfig.previewWidth - validLeft
        validTop = (FaceConfig.previewHeight - edgeWidth) / 2
        validBottom = FaceConfig.previewHeight - validTop
    }

    override fun onAfterRender(data: CameraFrameData) {
        mCameraGLSurfaceView.gleS2Render.draw_rect(data.params as Array<Rect>, Color.GREEN, 2)
    }

    /**
     * 判断是否平板设备
     * @param context
     * @return true:平板,false:手机
     */
    private fun isTabletDevice(context: Context) =
            context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE


}