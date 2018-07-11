package com.yryp.facehelper

import android.graphics.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.guo.android_extend.java.ExtByteArrayOutputStream

import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {


    private var faceResultList = listOf<Pair<Face, Float>>()
    lateinit var faceResult: ListView
    lateinit var addFace: Button
    lateinit var faceCameraView: FaceCameraView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ArcFaceHelper.init(FaceConfig.appid, FaceConfig.fr_key, FaceConfig.fd_key)
        val imageFilePath = File(Environment.getExternalStorageDirectory(), "faceImages")
        faceCameraView = findViewById(R.id.faceCameraView)
        faceCameraView.setCameraDisplayOrientation(this)
        addFace = findViewById(R.id.addFace)
        faceResult = findViewById(R.id.faceResult)
        faceResult.adapter = Adpter()
        if (!imageFilePath.exists()) {
            imageFilePath.mkdirs()
        }
        else{
            imageFilePath.listFiles().forEach { it.delete() }
        }
        faceCameraView.setOnMatch {
            if (it.isNotEmpty()) {
                faceResultList = it
                runOnUiThread { (faceResult.adapter as Adpter).notifyDataSetChanged() }
            }
        }
        addFace.setOnClickListener {
            faceCameraView.getEncodeFace { data, pair ->
                if (pair.first.isEmpty()) {
                    runOnUiThread { Toast.makeText(this, "未检测到人脸", Toast.LENGTH_SHORT).show() }
                } else {
                    val filename = "$imageFilePath/${System.currentTimeMillis()}.jpeg"
                    val yuv = YuvImage(data, ImageFormat.NV21, FaceConfig.previewWidth, FaceConfig.previewHeight, null)
                    val ops = ExtByteArrayOutputStream()
                    yuv.compressToJpeg(Rect(0, 0, FaceConfig.previewWidth, FaceConfig.previewHeight), 100, ops)
                    FileOutputStream(filename).use { it.write(ops.byteArray) }
                    runOnUiThread { Toast.makeText(this, "录入人脸成功", Toast.LENGTH_SHORT).show() }
                    ArcFaceHelper.addFace(Face("x", pair.first, pair.second, "xtm", filename))
                }
            }
        }
    }

    inner class Adpter : BaseAdapter() {
        val inflateLayout = LayoutInflater.from(this@MainActivity)
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val item = getItem(position)
            val view = inflateLayout.inflate(R.layout.item_match, null)
            val face = view.findViewById<ImageView>(R.id.ivFace)
            val score = view.findViewById<TextView>(R.id.score)
            score.text = item.second.toString()
            val bitmap = BitmapFactory.decodeFile(item.first.facePath)
            val rect = item.first.rects[0]
            val matrix = Matrix()
            matrix.setRotate(FaceConfig.rotate.toFloat())
            face.setImageBitmap(Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.right - rect.left,
                    rect.bottom - rect.top,matrix,false))
            return view
        }

        override fun getItem(position: Int): Pair<Face, Float> = faceResultList[position]

        override fun getItemId(position: Int) = position.toLong()
        override fun getCount() = faceResultList.size

    }
}
