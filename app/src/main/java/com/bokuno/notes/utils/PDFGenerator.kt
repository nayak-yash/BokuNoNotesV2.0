package com.bokuno.notes.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.bokuno.notes.R
import com.bokuno.notes.databinding.PdfLayoutBinding
import com.bokuno.notes.models.Note
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat


class PDFGenerator() {
    private lateinit var binding: PdfLayoutBinding
    private lateinit var fileOutputStream: FileOutputStream
    lateinit var file: File
    var flag : String = "SAVE"
    private lateinit var context: Context

    private fun createBitmapFromView(
        context: Context,
        view: View,
        note: Note,
        activity: Activity
    ): Bitmap {
        this.context = context
        binding = PdfLayoutBinding.bind(view)
        binding.tvHeading.setPaintFlags(binding.tvHeading.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG)
        binding.tvHeading.text = note.title
        binding.tvDetails.text =
            note.location + " " + SimpleDateFormat("dd-MM-yyyy 'at' HH:mm").format(note.createdAt)
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

    private fun createBitmap(context: Context, view: View, activity: Activity): Bitmap {
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
        return Bitmap.createScaledBitmap(bitmap, 1240, 1754, true)
    }

    private fun convertBitmapToPdf(bitmap: Bitmap, context: Context, note: Note) {
        if (flag == "SAVE") {
            val outputDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + "/BokuNoNotes"
            )
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            file = File(outputDir, "${note.title}.pdf")
        } else if (flag == "SHARE") {
            val outputDir = context.cacheDir // context being the Activity pointer
            file = File.createTempFile("${note.title}", ".pdf", outputDir)
            file.deleteOnExit()
        }
        fileOutputStream = FileOutputStream(file,false)
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0F, 0F, null)
        pdfDocument.finishPage(page)
        try {
            pdfDocument.writeTo(fileOutputStream)
            pdfDocument.close()
            fileOutputStream.close()
            if (flag != "SHARE") {
                Toast.makeText(context, "PDF saved successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
