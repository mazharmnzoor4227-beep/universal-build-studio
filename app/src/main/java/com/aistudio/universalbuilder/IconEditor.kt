package com.aistudio.universalbuilder
import android.content.Context
import android.net.Uri
import android.graphics.*
import java.io.File
import androidx.core.content.FileProvider
object IconEditor {
    fun create(context:Context,source:Uri,crop:Boolean,black:Boolean):Uri {
        val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        context.contentResolver.openInputStream(source)!!.use { BitmapFactory.decodeStream(it,null,options) }
        require(options.outWidth>0 && options.outHeight>0) { "Unsupported image" }
        var sample=1;while(options.outWidth/sample>1024 || options.outHeight/sample>1024)sample*=2
        options.inJustDecodeBounds=false;options.inSampleSize=sample
        val input=context.contentResolver.openInputStream(source)!!.use { BitmapFactory.decodeStream(it,null,options) } ?: error("Cannot decode icon")
        val output=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);val canvas=Canvas(output);canvas.drawColor(if(black)Color.BLACK else Color.WHITE)
        val factor=if(crop) maxOf(512f/input.width,512f/input.height) else minOf(512f/input.width,512f/input.height)
        val w=input.width*factor;val h=input.height*factor
        canvas.drawBitmap(input,null,RectF((512-w)/2,(512-h)/2,(512+w)/2,(512+h)/2),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        val file=File(context.cacheDir,"icon-${System.nanoTime()}.png");file.outputStream().use { output.compress(Bitmap.CompressFormat.PNG,100,it) };input.recycle();output.recycle()
        return FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
    }
}
