package com.fneb.piibiocampus.data.dao

import android.content.Context
import com.fneb.piibiocampus.data.error.AppException
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportDao {

    /**
     * Génère un fichier CSV dans le dossier Téléchargements et retourne son nom.
     *
     * Le DAO ne fait **aucun Toast** — c'est le ViewModel/Fragment qui affiche le résultat.
     *
     * @return Le nom du fichier créé (ex : "piibiocampus_export_1234567890.csv")
     * @throws AppException.NotAuthenticated si aucun utilisateur n'est connecté
     * @throws AppException.ExportFailed si l'écriture du fichier échoue
     */
    suspend fun exportUserDataAsCsv(context: Context, userId: String): String {
        return try {
            val userData = UserDao.getCurrentUserDataForExport()
            val photos   = PictureDao.getPicturesByUserEnrichedSortedByDate(userId)

            val sb = buildCsvContent(userData, photos)

            val fileName = "piibiocampus_export_${System.currentTimeMillis()}.csv"
            val file = File(
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                ),
                fileName
            )

            withContext(Dispatchers.IO) {
                file.writeText(sb.toString())
            }

            fileName
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw AppException.ExportFailed(e)
        }
    }

    // ── Builders CSV ──────────────────────────────────────────────────────────

    private fun buildCsvContent(
        userData: Map<String, String>,
        photos:   List<Map<String, Any>>
    ): StringBuilder {
        val sb = StringBuilder()

        sb.appendLine("PROFIL")
        sb.appendLine("email,name,description,profilePictureUrl")
        sb.appendLine(
            listOf(
                userData["email"]             ?: "",
                userData["name"]              ?: "",
                userData["description"]       ?: "",
                userData["profilePictureUrl"] ?: ""
            ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
        )

        sb.appendLine()
        sb.appendLine("PHOTOS")
        sb.appendLine("imageUrl,ordre,famille,genre,espece,adminValidated,recordingStatus,timestamp,latitude,longitude,altitude")

        photos.forEach { photo ->
            val loc       = photo["location"] as? Map<*, *>
            val timestamp = when (val ts = photo["timestamp"]) {
                is Timestamp -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(ts.toDate())
                is Date      -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(ts)
                else         -> ts?.toString() ?: ""
            }
            sb.appendLine(
                listOf(
                    photo["imageUrl"]        as? String ?: "",
                    photo["ordre"]           as? String ?: "",
                    photo["family"]          as? String ?: "",
                    photo["genre"]           as? String ?: "",
                    photo["specie"]          as? String ?: "",
                    (photo["adminValidated"]  as? Boolean)?.toString() ?: "false",
                    (photo["recordingStatus"] as? Boolean)?.toString() ?: "false",
                    timestamp,
                    (loc?.get("latitude")  as? Double)?.toString() ?: "",
                    (loc?.get("longitude") as? Double)?.toString() ?: "",
                    (loc?.get("altitude")  as? Double)?.toString() ?: ""
                ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
            )
        }

        return sb
    }
}