package com.fneb.piibiocampus.data.dao

import android.content.Context
import android.widget.Toast
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportDao {

    suspend fun exportUserDataAsCsv(context: Context, userId: String) {
        val userData = UserDao.getCurrentUserDataForExport()
        val photos = PictureDao.getPicturesByUserEnrichedSortedByDate(userId)

        val sb = StringBuilder()


        sb.appendLine("PROFIL")
        sb.appendLine("email,name,description,profilePictureUrl")
        sb.appendLine(
            listOf(
                userData["email"] ?: "",
                userData["name"] ?: "",
                userData["description"] ?: "",
                userData["profilePictureUrl"] ?: ""
            ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
        )

        sb.appendLine()
        sb.appendLine("PHOTOS")
        sb.appendLine("imageUrl,ordre,famille,genre,espece,adminValidated,recordingStatus,timestamp,latitude,longitude,altitude")

        photos.forEach { photo ->
            val loc = photo["location"] as? Map<*, *>
            val timestamp = when (val ts = photo["timestamp"]) {
                is Timestamp -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(ts.toDate())
                is Date -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(ts)
                else -> ts?.toString() ?: ""
            }
            sb.appendLine(
                listOf(
                    photo["imageUrl"] as? String ?: "",
                    photo["ordre"] as? String ?: "",
                    photo["family"] as? String ?: "",
                    photo["genre"] as? String ?: "",
                    photo["specie"] as? String ?: "",
                    (photo["adminValidated"] as? Boolean)?.toString() ?: "false",
                    (photo["recordingStatus"] as? Boolean)?.toString() ?: "false",
                    timestamp,
                    (loc?.get("latitude") as? Double)?.toString() ?: "",
                    (loc?.get("longitude") as? Double)?.toString() ?: "",
                    (loc?.get("altitude") as? Double)?.toString() ?: ""
                ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
            )
        }

        val fileName = "piibiocampus_export_${System.currentTimeMillis()}.csv"
        val file = File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        file.writeText(sb.toString())

        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "Export sauvegardé dans Téléchargements/$fileName",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}