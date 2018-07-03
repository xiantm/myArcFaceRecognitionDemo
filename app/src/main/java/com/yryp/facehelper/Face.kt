package com.yryp.facehelper

import android.graphics.Rect
import com.arcsoft.facerecognition.AFR_FSDKFace

class Face(val faceId: String, var faces: List<AFR_FSDKFace>, var rects: List<Rect>, var name: String, var facePath: String)