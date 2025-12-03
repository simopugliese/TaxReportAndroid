package it.simonepugliese.taxreport.util

import android.content.Context
import pugliesesimone.taxreport.AppFactory
import pugliesesimone.taxreport.model.Document
import pugliesesimone.taxreport.service.TaxReportService
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ServiceManager {
    private const val PREFS_NAME = "taxreport_config"
    private var serviceInstance: TaxReportService? = null

    const val KEY_HOST = "host"
    const val KEY_DB_PORT = "db_port"
    const val KEY_DB_NAME = "db_name"
    const val KEY_DB_USER = "db_user"
    const val KEY_DB_PASS = "db_pass"
    const val KEY_SMB_SHARE = "smb_share"
    const val KEY_SMB_USER = "smb_user"
    const val KEY_SMB_PASS = "smb_pass"

    fun init(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val host = prefs.getString(KEY_HOST, "")

        if (host.isNullOrEmpty()) return false

        try {
            serviceInstance = AppFactory.buildAndroidService(
                host,
                prefs.getString(KEY_DB_PORT, "3306")?.toIntOrNull() ?: 3306,
                prefs.getString(KEY_DB_NAME, "taxreport") ?: "taxreport",
                prefs.getString(KEY_DB_USER, "root") ?: "",
                prefs.getString(KEY_DB_PASS, "") ?: "",
                host,
                prefs.getString(KEY_SMB_SHARE, "TaxData") ?: "TaxData",
                prefs.getString(KEY_SMB_USER, "pi") ?: "",
                prefs.getString(KEY_SMB_PASS, "") ?: ""
            )
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            throw IOException("Errore inizializzazione backend: ${e.message}", e)
        }
    }

    fun get(): TaxReportService {
        return serviceInstance ?: throw IllegalStateException("ServiceManager non inizializzato.")
    }

    fun isReady(): Boolean = serviceInstance != null

    fun saveConfigAndConnect(context: Context, config: Map<String, String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        config.forEach { (key, value) -> editor.putString(key, value) }
        editor.apply()
        serviceInstance = null
        init(context)
    }

    fun downloadDocument(context: Context, document: Document): File {
        val path = document.relativePath ?: throw IOException("Path documento nullo")
        val fileOnServer = File(path)
        val fileName = fileOnServer.name

        // File di destinazione
        val cacheFile = File(context.cacheDir, fileName)

        // 3. SMART CACHING: Se c'è già, usa quello locale
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return cacheFile
        }

        // Se siamo qui, dobbiamo scaricare
        val service = get()
        val parentPath = fileOnServer.parent ?: ""

        // 1. ACCESSO DIRETTO (Grazie alla tua modifica al backend)
        // Kotlin accede a getStorage() o al campo public 'storage' tramite questa property
        val storage = service.storage

        val inputStream = storage.loadFile(parentPath, fileName)
            ?: throw IOException("File non trovato sul server: $path")

        FileOutputStream(cacheFile).use { output ->
            inputStream.copyTo(output)
        }

        return cacheFile
    }
}