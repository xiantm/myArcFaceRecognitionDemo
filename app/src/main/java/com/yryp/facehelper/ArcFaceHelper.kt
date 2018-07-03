package com.yryp.facehelper

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.arcsoft.facedetection.AFD_FSDKEngine
import com.arcsoft.facedetection.AFD_FSDKFace
import com.arcsoft.facerecognition.AFR_FSDKEngine
import com.arcsoft.facerecognition.AFR_FSDKError
import com.arcsoft.facerecognition.AFR_FSDKError.MOK
import com.arcsoft.facerecognition.AFR_FSDKFace
import com.arcsoft.facerecognition.AFR_FSDKMatching
import com.arcsoft.facetracking.AFT_FSDKFace
import com.guo.android_extend.image.ImageConverter
import java.util.*

object ArcFaceHelper {
    private val TAG = "FaceHelper"
    private var afrEngine = AFR_FSDKEngine()
    private var afdEngine = AFD_FSDKEngine()

    // 需要检索的人脸
    private var faceCache = mutableListOf<Face>()

    fun init(appid: String, fr_key: String, fd_key: String): Boolean {
        val afrError = afrEngine.AFR_FSDK_InitialEngine(appid, fr_key)
        Log.d(TAG, "人脸对比引擎初始化->${afrError.code}")
        val afdError = afdEngine.AFD_FSDK_InitialFaceEngine(appid, fd_key, AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5)
        Log.d(TAG, "人脸对比引擎初始化->${afrError.code}")
        if (afrError.code == MOK && afdError.code == MOK) {
            Log.d(TAG, "人初始化成功")
            return true
        }
        Log.d(TAG, "初始化失败")
        return false


    }

    fun destory() {
        afrEngine.AFR_FSDK_UninitialEngine()
        afdEngine.AFD_FSDK_UninitialFaceEngine()
    }

    /**
     * 人脸1：1对比
     * @param face1 人脸一
     * @param face2 人脸二
     * @return 置信度
     */
    fun compare11(face1: AFR_FSDKFace, face2: AFR_FSDKFace): Float {
        val score = AFR_FSDKMatching()
        val error = afrEngine.AFR_FSDK_FacePairMatching(face1, face2, score)
        Log.d(TAG, "人脸对比置信度:" + score.score + ", AFR_FSDK_FacePairMatching=" + error.code)
        Log.d(TAG, "AFR_FSDK_FacePairMatching -> errorcode :" + error.code)
        return score.score
    }

    /**
     * 编码图片
     * @param faceBitmap 需要编码的图片
     * @param onlyBiggestFace 是否只取最大人脸
     * @return 人脸特征集合
     */
    fun encodeFace(faceBitmap: Bitmap, onlyBiggestFace: Boolean): List<AFR_FSDKFace> {
        val data = ByteArray(faceBitmap.width * faceBitmap.height * 3 / 2)
        try {
            val convert = ImageConverter()
            convert.initial(faceBitmap.width, faceBitmap.height, ImageConverter.CP_PAF_NV21)
            if (convert.convert(faceBitmap, data)) {
                Log.d(TAG, "convert ok!")
            }
            convert.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        var afdResult: List<AFD_FSDKFace> = ArrayList<AFD_FSDKFace>()
        afdEngine.AFD_FSDK_StillImageFaceDetection(data, faceBitmap.width, faceBitmap.height, AFD_FSDKEngine.CP_PAF_NV21, afdResult)
        val result = mutableListOf<AFR_FSDKFace>()
        if (afdResult.isNotEmpty()) {
            afdResult = afdResult.sortedBy { (it.rect.bottom - it.rect.top) * (it.rect.right - it.rect.left) }
            val tempResult = AFR_FSDKFace()
            if (onlyBiggestFace) {
                val error = afrEngine.AFR_FSDK_ExtractFRFeature(data, faceBitmap.width, faceBitmap.height, AFR_FSDKEngine.CP_PAF_NV21, afdResult[0].rect, afdResult[0].degree, tempResult)
                if (error.code == MOK) result.add(tempResult.clone())
            } else {
                for (face in afdResult) {
                    val error = afrEngine.AFR_FSDK_ExtractFRFeature(data, faceBitmap.width, faceBitmap.height, AFR_FSDKEngine.CP_PAF_NV21, face.rect, face.degree, tempResult)
                    if (error.code == MOK) result.add(tempResult.clone())
                }
            }
        }
        return result
    }

    /**
     * 编码nv21数据
     * @param data 需要编码的图片
     * @param width 图片宽
     * @param height 图片高
     * @param onlyBiggestFace 是否只取最大人脸
     * @return 人脸特征集合
     */
    fun encodeFace(data: ByteArray, width: Int, height: Int, onlyBiggestFace: Boolean): List<AFR_FSDKFace> {
        var afdResult: List<AFD_FSDKFace> = ArrayList<AFD_FSDKFace>()
        afdEngine.AFD_FSDK_StillImageFaceDetection(data, width, height, AFD_FSDKEngine.CP_PAF_NV21, afdResult)
        val result = mutableListOf<AFR_FSDKFace>()
        if (afdResult.isNotEmpty()) {
            afdResult = afdResult.sortedBy { (it.rect.bottom - it.rect.top) * (it.rect.right - it.rect.left) }
            val tempResult = AFR_FSDKFace()
            if (onlyBiggestFace) {
                val error = afrEngine.AFR_FSDK_ExtractFRFeature(data, width, height, AFR_FSDKEngine.CP_PAF_NV21, afdResult[0].rect, afdResult[0].degree, tempResult)
                if (error.code == MOK) result.add(tempResult.clone())
            } else {
                for (face in afdResult) {
                    val error = afrEngine.AFR_FSDK_ExtractFRFeature(data, width, height, AFR_FSDKEngine.CP_PAF_NV21, face.rect, face.degree, tempResult)
                    if (error.code == MOK) result.add(tempResult.clone())
                }
            }
        }
        return result
    }


    /**
     * 编码nv21数据,已知AFT数据
     * @param data 需要编码的图片
     * @param aftFaceList 检测到的人脸集合
     * @param width 图片宽
     * @param height 图片高
     * @param onlyBiggestFace 是否只取最大人脸
     * @return 人脸特征集合
     */
    fun encodeFace(data: ByteArray, aftFaceList: List<AFT_FSDKFace>, width: Int, height: Int, onlyBiggestFace: Boolean): Pair<List<AFR_FSDKFace>, List<Rect>> {
        val afrResult = mutableListOf<AFR_FSDKFace>()
        val rectResult = mutableListOf<Rect>()
        val aftResult = aftFaceList.sortedBy { (it.rect.bottom - it.rect.top) * (it.rect.right - it.rect.left) }
        val tempResult = AFR_FSDKFace()
        if (onlyBiggestFace) {
            val error = afrEngine.AFR_FSDK_ExtractFRFeature(data, width, height, AFR_FSDKEngine.CP_PAF_NV21, aftResult[0].rect, aftResult[0].degree, tempResult)
            if (error.code == MOK) {
                afrResult.add(tempResult.clone())
                rectResult.add(Rect(aftResult[0].rect))
            }
        } else {
            for (face in aftResult) {
                val error = afrEngine.AFR_FSDK_ExtractFRFeature(data, width, height, AFR_FSDKEngine.CP_PAF_NV21, face.rect, face.degree, tempResult)
                if (error.code == MOK) {
                    afrResult.add(tempResult.clone())
                    rectResult.add(Rect(face.rect))
                }
            }
        }
        return Pair(afrResult,rectResult)
    }


    /**
     * 向人脸集合中添加人脸
     * @param face
     */
    fun addFace(face: Face) = faceCache.add(face)

    /**
     * 清除所有人脸
     */
    fun clearFaces() = faceCache.clear()

    /**
     * 搜索人脸
     * @param data 图片
     * @param width 图片宽
     * @param height 图片高
     * @param aftFaceList AFT引擎追踪到的人脸
     * @param minScore 最小匹配分数
     * @return 搜索到的人脸集合
     */
    fun searchFace(data: ByteArray, width: Int, height: Int, aftFaceList: List<AFT_FSDKFace>, minScore: Float, onlyBiggestFace: Boolean): List<Pair<Face, Float>> {
        val afrEngine = AFR_FSDKEngine()
        afrEngine.AFR_FSDK_InitialEngine(FaceConfig.appid, FaceConfig.fr_key)
        var saftFaceList = aftFaceList.sortedBy { (it.rect.bottom - it.rect.top) * (it.rect.right - it.rect.left) }
        if (onlyBiggestFace) {
            saftFaceList = listOf(saftFaceList[0])
        }
        val result = mutableListOf<Pair<Face, Float>>()
        val score = AFR_FSDKMatching()
        for (aftFace in saftFaceList) {
            var param = AFR_FSDKFace()
            var error = afrEngine.AFR_FSDK_ExtractFRFeature(data, width, height, AFD_FSDKEngine.CP_PAF_NV21, aftFace.rect, aftFace.degree, param)
            if (error.code != AFR_FSDKError.MOK)
                continue
            for (face in faceCache) {
                var faces = face.faces
                if (onlyBiggestFace) {
                    faces = listOf(face.faces[0])
                }
                for (item in faces) {
                    val error = afrEngine.AFR_FSDK_FacePairMatching(param, item, score)
                    if (error.code == AFR_FSDKError.MOK && score.score >= minScore) {
                        result.add(Pair(face, score.score))
                    }
                }
            }
        }
        afrEngine.AFR_FSDK_UninitialEngine()
        return result
    }
}

