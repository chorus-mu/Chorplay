package io.github.pushkar.chorplay.data.video.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloadLocationTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val staging by lazy { temp.newFolder("staging") }
    private val defaultFolder by lazy { temp.newFolder("Download", "Chorplay") }
    private val privateFolder by lazy { temp.newFolder("private") }
    private val blocked = mutableSetOf<File>()
    private val grantedTrees = mutableSetOf<String>()

    private fun resolve(chosen: DownloadLocation) =
        resolveDownloadDestination(
            chosen = chosen,
            defaults = listOf(defaultFolder, privateFolder),
            staging = staging,
            isUsableDirectory = { it !in blocked && (it.mkdirs() || it.isDirectory) },
            hasTreeAccess = { it in grantedTrees },
        )

    @Test
    fun `a writable chosen folder is written to directly`() {
        val chosen = File(temp.root, "Custom")
        val destination = resolve(DownloadLocation(path = chosen.path))
        assertEquals(chosen, destination.directory)
        assertNull(destination.exportTreeUri)
        assertFalse(destination.fellBack)
    }

    @Test
    fun `an unwritable chosen folder falls back to the default and says so`() {
        val chosen = File(temp.root, "ReadOnly").also { blocked += it }
        val destination = resolve(DownloadLocation(path = chosen.path))
        assertEquals(defaultFolder, destination.directory)
        assertTrue(destination.fellBack)
    }

    @Test
    fun `an unwritable chosen folder picked through the system picker is exported to`() {
        val chosen = File(temp.root, "Picked").also { blocked += it }
        val tree = "content://com.android.externalstorage.documents/tree/primary%3APicked"
        grantedTrees += tree
        val destination = resolve(DownloadLocation(path = chosen.path, treeUri = tree))
        assertEquals(staging, destination.directory)
        assertEquals(tree, destination.exportTreeUri)
        assertFalse(destination.fellBack)
    }

    @Test
    fun `a picked folder whose access was revoked falls back and says so`() {
        val tree = "content://com.android.externalstorage.documents/tree/primary%3AGone"
        val destination = resolve(DownloadLocation(treeUri = tree))
        assertEquals(defaultFolder, destination.directory)
        assertNull(destination.exportTreeUri)
        assertTrue(destination.fellBack)
    }

    @Test
    fun `no choice uses the first usable default without a fallback notice`() {
        blocked += defaultFolder
        val destination = resolve(DownloadLocation.DEFAULT)
        assertEquals(privateFolder, destination.directory)
        assertFalse(destination.fellBack)
    }

    @Test
    fun `document ids map to their storage paths`() {
        val root = "/storage/emulated/0"
        assertEquals("/storage/emulated/0/Movies/Chorplay", documentIdToPath("primary:Movies/Chorplay", root))
        assertEquals("/storage/emulated/0/Chorplay/a b.mp4", documentIdToPath("primary:Chorplay/a b.mp4", root))
        assertEquals("/storage/emulated/0", documentIdToPath("primary:", "$root/"))
        assertEquals("/storage/1A2B-3C4D/Music", documentIdToPath("1A2B-3C4D:Music", root))
        assertEquals("/storage/emulated/0/Download/x.mp4", documentIdToPath("raw:/storage/emulated/0/Download/x.mp4", root))
        assertNull(documentIdToPath("msf:1234", root))
        assertNull(documentIdToPath("1234", root))
    }
}
