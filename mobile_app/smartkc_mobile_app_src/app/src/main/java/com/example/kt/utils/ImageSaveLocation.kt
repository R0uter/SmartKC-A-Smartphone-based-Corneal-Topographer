package com.example.kt.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream

object ImageSaveLocation {
    private const val TAG = "ImageSaveLocation"

    fun defaultSessionDirectory(context: Context, baseDir: String?, dirName: String?): File {
        val root = File(context.getExternalFilesDir(null), baseDir ?: "KT")
        if (!root.exists()) {
            root.mkdirs()
        }

        val sessionDir = File(root, dirName ?: "")
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        return sessionDir
    }

    fun folderLabel(uriString: String?): String {
        if (uriString.isNullOrEmpty()) {
            return "Default app folder"
        }
        return Uri.decode(Uri.parse(uriString).lastPathSegment ?: uriString)
    }

    fun copyToSelectedFolder(
        context: Context,
        treeUriString: String?,
        baseDir: String?,
        dirName: String?,
        sourceFile: File,
        mimeType: String = "image/jpeg"
    ): Uri? {
        if (treeUriString.isNullOrEmpty() || !sourceFile.exists()) {
            return null
        }

        return try {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(treeUriString)) ?: return null
            val baseFolder = findOrCreateDirectory(tree, baseDir ?: "KT") ?: return null
            val sessionFolder = if (dirName.isNullOrEmpty()) {
                baseFolder
            } else {
                findOrCreateDirectory(baseFolder, dirName) ?: return null
            }
            sessionFolder.findFile(sourceFile.name)?.delete()
            val outputFile = sessionFolder.createFile(mimeType, sourceFile.name) ?: return null

            context.contentResolver.openOutputStream(outputFile.uri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            }
            outputFile.uri
        } catch (exc: Exception) {
            Log.e(TAG, "Failed to copy image to selected folder", exc)
            null
        }
    }

    private fun findOrCreateDirectory(parent: DocumentFile, name: String): DocumentFile? {
        return parent.findFile(name)?.takeIf { it.isDirectory } ?: parent.createDirectory(name)
    }
}
