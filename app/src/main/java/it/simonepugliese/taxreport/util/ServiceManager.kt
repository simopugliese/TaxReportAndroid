package it.simonepugliese.taxreport.util

import android.content.Context
import android.content.SharedPreferences
import pugliesesimone.taxreport.AppFactory
import pugliesesimone.taxreport.service.TaxReportService
import java.io.IOException

/**
 * Singleton che gestisce l'istanza del backend (TaxReportService).
 * Si occupa di caricare la configurazione dalle SharedPreferences e
 * inizializzare la connessione a MariaDB e Samba.
 */
object ServiceManager {
    private const val PREFS_NAME = "taxreport_config"
    private var serviceInstance: TaxReportService? = null

    // Chiavi per il salvataggio (uguali a quelle Desktop per coerenza mentale)
    const val KEY_HOST = "host"
    const val KEY_DB_PORT = "db_port"
    const val KEY_DB_NAME = "db_name"
    const val KEY_DB_USER = "db_user"
    const val KEY_DB_PASS = "db_pass"
    const val KEY_SMB_SHARE = "smb_share"
    const val KEY_SMB_USER = "smb_user"
    const val KEY_SMB_PASS = "smb_pass"

    /**
     * Tenta di inizializzare il servizio leggendo la config salvata.
     * @return true se la connessione ha successo, false se manca la config.
     * @throws IOException se la config c'è ma la connessione fallisce.
     */
    fun init(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val host = prefs.getString(KEY_HOST, "")

        // Se non c'è l'host salvato, l'utente deve ancora configurare l'app
        if (host.isNullOrEmpty()) return false

        try {
            // [IMPORTANTE] Qui chiamiamo la tua Factory del JAR Backend.
            // Nota: AppFactory.buildAndroidService crea le connessioni JDBC e SMB.
            // Se i parametri sono errati o il server è giù, lancerà un'eccezione.
            serviceInstance = AppFactory.buildAndroidService(
                host,
                prefs.getString(KEY_DB_PORT, "3306")?.toIntOrNull() ?: 3306,
                prefs.getString(KEY_DB_NAME, "taxreport") ?: "taxreport",
                prefs.getString(KEY_DB_USER, "root") ?: "",
                prefs.getString(KEY_DB_PASS, "") ?: "",
                host, // Assumiamo che Samba sia sullo stesso IP del DB
                prefs.getString(KEY_SMB_SHARE, "TaxData") ?: "TaxData",
                prefs.getString(KEY_SMB_USER, "pi") ?: "",
                prefs.getString(KEY_SMB_PASS, "") ?: ""
            )
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            // Rilanciamo l'errore per mostrarlo nella UI (es. "Connessione rifiutata")
            throw IOException("Errore inizializzazione backend: ${e.message}", e)
        }
    }

    /**
     * Restituisce l'istanza del servizio.
     * Da chiamare SOLO dopo che isReady() ha ritornato true.
     */
    fun get(): TaxReportService {
        return serviceInstance ?: throw IllegalStateException("ServiceManager non inizializzato. Hai chiamato init()?")
    }

    /**
     * Verifica veloce se il servizio è attivo senza lanciare eccezioni.
     */
    fun isReady(): Boolean = serviceInstance != null

    /**
     * Salva la nuova configurazione e tenta subito la connessione.
     * Usato dalla schermata Settings.
     */
    fun saveConfigAndConnect(context: Context, config: Map<String, String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        config.forEach { (key, value) ->
            editor.putString(key, value)
        }
        editor.apply()

        // Forza la reinizializzazione con i nuovi dati
        serviceInstance = null
        init(context)
    }
}