package com.example.kt.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ImageSaveLocation {
    private const val TAG = "ImageSaveLocation"
    const val DOWNLOADS_URI = "smartkc://downloads"

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
        if (uriString == DOWNLOADS_URI) {
            return "Downloads/KT"
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
        if (treeUriString == DOWNLOADS_URI) {
            return copyToDownloads(context, baseDir, dirName, sourceFile, mimeType)
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

    private fun copyToDownloads(
        context: Context,
        baseDir: String?,
        dirName: String?,
        sourceFile: File,
        mimeType: String
    ): Uri? {
        val relativePath = listOfNotNull(
            Environment.DIRECTORY_DOWNLOADS,
            baseDir ?: "KT",
            dirName?.takeIf { it.isNotEmpty() }
        ).joinToString(File.separator) + File.separator

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            copyToMediaStoreDownloads(context, relativePath, sourceFile, mimeType)
        } else {
            copyToLegacyDownloads(relativePath, sourceFile)
        }
    }

    private fun copyToMediaStoreDownloads(
        context: Context,
        relativePath: String,
        sourceFile: File,
        mimeType: String
    ): Uri? {
        deleteExistingMediaStoreDownload(context, relativePath, sourceFile.name)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            }
            uri
        } catch (exc: Exception) {
            resolver.delete(uri, null, null)
            Log.e(TAG, "Failed to copy file to Downloads", exc)
            null
        }
    }

    private fun deleteExistingMediaStoreDownload(context: Context, relativePath: String, displayName: String) {
        val resolver = context.contentResolver
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
            arrayOf(displayName, relativePath),
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn))
                resolver.delete(uri, null, null)
            }
        }
    }

    private fun copyToLegacyDownloads(relativePath: String, sourceFile: File): Uri? {
        return try {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val relativeDir = relativePath.removePrefix(Environment.DIRECTORY_DOWNLOADS + File.separator)
            val outputDir = File(downloads, relativeDir)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val outputFile = File(outputDir, sourceFile.name)
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(outputFile)
        } catch (exc: Exception) {
            Log.e(TAG, "Failed to copy file to legacy Downloads", exc)
            null
        }
    }

    private fun findOrCreateDirectory(parent: DocumentFile, name: String): DocumentFile? {
        return parent.findFile(name)?.takeIf { it.isDirectory } ?: parent.createDirectory(name)
    }
}
