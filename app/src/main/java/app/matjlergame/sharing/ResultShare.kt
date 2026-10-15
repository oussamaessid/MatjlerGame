package app.matjlergame.sharing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object ResultShare {
    fun textIntent(result: ShareResult) = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, result.text)
        putExtra(Intent.EXTRA_SUBJECT, result.game)
    }

    /** Only this generated PNG is exposed. No game state, preferences or credentials are shared. */
    fun imageIntent(context: Context, result: ShareResult): Intent {
        val directory = File(context.cacheDir, "shared_results").apply { mkdirs() }
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86400000 }?.forEach { it.delete() }
        val file = File(directory, "result-${UUID.randomUUID()}.png")
        val bitmap = render(context, result)
        try { file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
        finally { bitmap.recycle() }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.results", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, result.text)
            putExtra(Intent.EXTRA_SUBJECT, result.game)
            putExtra(Intent.EXTRA_TITLE, result.game)
            clipData = ClipData.newUri(context.contentResolver, result.game, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun render(context: Context, result: ShareResult): Bitmap {
        val bitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val accent = if (result.wordGame) Color.rgb(23,105,170) else Color.rgb(81,64,171)
        val ink = Color.rgb(35,32,61)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f,0f,1080f,1350f,Color.rgb(30,24,65),accent,Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,1080f,1350f,paint);paint.shader=null
        fun line(text: String, y: Float, size: Float, color: Int, bold: Boolean = false) {
            paint.color=color;paint.typeface=Typeface.create("sans-serif",if (bold) Typeface.BOLD else Typeface.NORMAL)
            paint.textSize=size
            while (paint.measureText(text)>870 && paint.textSize>18) paint.textSize-=1
            paint.textAlign=Paint.Align.CENTER
            canvas.drawText(text,540f,y,paint)
        }
        line(result.game.uppercase(java.util.Locale.ROOT),85f,42f,Color.WHITE,true)
        line(result.dateLabel,134f,27f,Color.rgb(225,227,247))
        paint.color=Color.WHITE;canvas.drawRoundRect(50f,175f,1030f,1285f,44f,44f,paint)
        if (result.avatarResource!=0) {
            ContextCompat.getDrawable(context,result.avatarResource)?.let { drawable ->
                canvas.save();val clip=Path().apply { addCircle(540f,260f,51f,Path.Direction.CW) };canvas.clipPath(clip)
                drawable.setBounds(489,209,591,311);drawable.draw(canvas);canvas.restore()
            }
        }
        line(result.player,355f,31f,ink,true)
        val heading = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize=48f;color=ink;typeface=Typeface.create("sans-serif",Typeface.BOLD) }
        val layout = StaticLayout.Builder.obtain(result.headline,0,result.headline.length,heading,860)
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).build()
        canvas.save();canvas.translate(110f,390f);layout.draw(canvas);canvas.restore()
        line(result.category,532f,28f,accent,true)
        paint.color=Color.rgb(242,244,250);canvas.drawRoundRect(175f,565f,905f,730f,28f,28f,paint)
        line(if(result.english) "TIME" else "TEMPS",610f,21f,Color.rgb(115,118,138),true)
        line(result.timeLabel,691f,66f,accent,true)
        if (result.grid.isNotEmpty()) {
            val cell=48f;val gap=10f
            result.grid.take(6).forEachIndexed { rowIndex,row ->
                val left=(1080f-(row.size*cell+(row.size-1)*gap))/2
                row.forEachIndexed { column,tile ->
                    paint.color=when(tile) { ShareTile.CORRECT->Color.rgb(65,145,89);ShareTile.PRESENT->Color.rgb(205,167,57);ShareTile.ABSENT->Color.rgb(80,86,99) }
                    val x=left+column*(cell+gap);val y=770f+rowIndex*(cell+gap)
                    canvas.drawRoundRect(x,y,x+cell,y+cell,7f,7f,paint)
                }
            }
            line("${if(result.english) "Attempts" else "Essais"} : ${result.attemptsLabel}",1160f,29f,ink,true)
        } else {
            line(result.attemptsLabel,935f,120f,if(result.won) Color.rgb(52,139,82) else accent,true)
            line(if(result.english) "ATTEMPTS" else "ESSAIS",994f,25f,Color.rgb(115,118,138),true)
        }
        line(if(result.english) "Your turn!" else "À vous de jouer !",1232f,31f,accent,true)
        return bitmap
    }
}
