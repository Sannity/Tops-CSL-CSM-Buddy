import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.Properties

fun String.toColumnNumber(): Int{
    val alphabet = mutableListOf<Char>()
    var c = 'a'
    while(c <= 'z')
        alphabet.add(c++)
    return alphabet.indexOf(this[0].toLowerCase())
}

class ApplicationSettings{
    //Default Application Settings
    private companion object {
        val defaultSettings = Properties()
        init {
            defaultSettings.setProperty("db_URL", "")
            defaultSettings.setProperty("db_username", "")
            defaultSettings.setProperty("db_password", "")
            defaultSettings.setProperty("employee_profile_name_column", "A")
            defaultSettings.setProperty("employee_profile_id_column", "B")
            defaultSettings.setProperty("schedule_employee_name_column", "A")
            defaultSettings.setProperty("schedule_shift_date_column", "B")
            defaultSettings.setProperty("schedule_shift_time_start_column", "C")
            defaultSettings.setProperty("schedule_shift_time_end_column", "D")
            defaultSettings.setProperty("schedule_job_column", "E")
        }
    }
    //Hidden Properties File containing settings
    private var settings = Properties()
    init {
        val settingsFileInput: FileInputStream
        val settingsFileOutput: FileOutputStream
        try {
            // Try to load a config
            settingsFileInput = FileInputStream("config.properties")
            settings.load(settingsFileInput)
        } catch (e: FileNotFoundException) {
            //If there is no config, load the defaults
            settingsFileOutput = FileOutputStream("config.properties")
            settings = ApplicationSettings.defaultSettings
            settings.store(settingsFileOutput, "CSL-Buddy Application Settings")

        }
    }
    operator fun get(key: String): String = settings[key].toString()
    operator fun set(key: String, value: String){
        settings.setProperty(key, value)
        val settingsFileOutput = FileOutputStream("config.properties")
        settings.store(settingsFileOutput, "CSL-Buddy Application Settings")
    }
}