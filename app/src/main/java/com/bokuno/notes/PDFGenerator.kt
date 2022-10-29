package com.bokuno.notes

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bokuno.notes.databinding.PdfLayoutBinding
import com.bokuno.notes.models.Note
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat

class PDFGenerator {
    private lateinit var binding: PdfLayoutBinding
    private fun createBitmapFromView(
        context: Context,
        view: View,
        note: Note,
        activity: Activity
    ): Bitmap {

        binding = PdfLayoutBinding.bind(view)
        binding.tvHeading.setPaintFlags(binding.tvHeading.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG)
        binding.tvHeading.text = note.title
        binding.tvDetails.text = note.location + " " +SimpleDateFormat("dd-MM-yyyy 'at' HH:mm").format(note.createdAt)
        binding.tvNote.text = note.text
        return createBitmap(context, binding.root, activity)
    }

    fun createPdf(
        context: Context, note: Note, activity: Activity
    ) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.pdf_layout, null)
        val bitmap = createBitmapFromView(context, view, note, activity)
        convertBitmapToPdf(bitmap, activity, note)
    }

    private fun createBitmap(context: Context, view: View, activity: Activity, ): Bitmap {
        val displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.getRealMetrics(displayMetrics)
            displayMetrics.densityDpi
        } else {
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        }
        view.measure(
            View.MeasureSpec.makeMeasureSpec(
                displayMetrics.widthPixels, View.MeasureSpec.EXACTLY
            ),
            View.MeasureSpec.makeMeasureSpec(
                displayMetrics.heightPixels, View.MeasureSpec.EXACTLY
            )
        )
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        val bitmap = Bitmap.createBitmap(
            view.measuredWidth,
            view.measuredHeight, Bitmap.Config.ARGB_8888
        )



        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return Bitmap.createScaledBitmap(bitmap, 1240 , 1754, true)
    }

    private fun convertBitmapToPdf(bitmap: Bitmap, context: Context, note: Note) {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/BokuNoNotes"
        )
        if (!dir.exists())
            dir.mkdirs()

        val file = File(dir, "${note.title}.pdf")
        file.createNewFile()
        val fileOutputStream = FileOutputStream(file)
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0F, 0F, null)
        pdfDocument.finishPage(page)
        try {
            pdfDocument.writeTo(fileOutputStream)
            pdfDocument.close()
            Toast.makeText(context, "PDF saved successfully", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}