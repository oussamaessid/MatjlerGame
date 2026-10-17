package app.matjlergame
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.matjlergame.sharing.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class ResultShareAndroidTest {
    @Test fun imageAndTextCanBeReadAndOnlyGrantTemporaryReadAccess() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val result=ShareResult("Equal To","Joueur Test","2026-10-15","Mode Difficile",true,3,5,83400,
            english=false,wordGame=false,
            grid=emptyList(),avatarResource=R.drawable.avatar_ludo_04)
        val intent=ResultShare.imageIntent(context,result)
        assertEquals(Intent.ACTION_SEND,intent.action);assertEquals("image/png",intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(0,intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        @Suppress("DEPRECATION") val uri=intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content",uri.scheme);assertEquals(context.packageName+".results",uri.authority)
        assertEquals(uri,intent.clipData!!.getItemAt(0).uri)
        val bytes=context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size)
        assertEquals(1080,bitmap.width);assertEquals(1350,bitmap.height);bitmap.recycle()
        assertTrue(intent.getStringExtra(Intent.EXTRA_TEXT)!!.contains("01:23"))
        File(context.getExternalFilesDir(null),"result-share-preview.png").writeBytes(bytes)
        assertEquals("text/plain",ResultShare.textIntent(result).type)
    }
    @Test fun privateFilesCannotBeExposedByTheShareProvider() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val privateFile=File(context.filesDir,"not-a-share.txt")
        try {
            FileProvider.getUriForFile(context,context.packageName+".results",privateFile)
            fail("Private files must not be shareable")
        } catch (_: IllegalArgumentException) { }
    }
}
