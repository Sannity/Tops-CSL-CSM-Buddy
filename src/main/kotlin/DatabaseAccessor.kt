import com.opencsv.CSVReader
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.transaction
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import org.vandeseer.easytable.structure.TableNotYetBuiltException
import java.lang.IndexOutOfBoundsException
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time.DayOfWeek

data class Employee(val name: String, val ID: Long)
class Shift(val employee: String, val date: String, val startTime: String, val endTime: String, val job: String){
    override fun toString() = "$employee ($startTime, $endTime)"
}
object Schedule: Table(){
    val employee = varchar("employee", length = 50)
    val date = date("date")
    val start = datetime("start")
    val end = datetime("end")
    val job = varchar("job", length = 50)
}
object Employees: Table(){
    val id = integer("id").primaryKey().uniqueIndex()
    val name = varchar("name", length = 50).primaryKey()
}
fun ResultRow.toShift() = Shift(this[Schedule.employee], SimpleDateFormat("yyyy-MM-dd").format(this[Schedule.date].toDate()), SimpleDateFormat("H:mm").format(this[Schedule.start].toDate()), SimpleDateFormat("H:mm").format(this[Schedule.end].toDate()), this[Schedule.job])
fun ResultRow.toEmployee() = Employee(this[Employees.name], this[Employees.id].toLong())
fun Char.toColumnNumber(): Int{
    val alphabet = mutableListOf<Char>()
    var c = 'a'
    while(c <= 'z')
        alphabet.add(c++)
    return alphabet.indexOf(this.toLowerCase())
}

class DatabaseAccessor{
    companion object {
        var URL = "jdbc:mysql://localhost:3306/cslbuddy"
        var driver = "com.mysql.jdbc.Driver"
        var userName = "t00585c7"
        var password = "Martinez1"

        var employee_id_column = 'B'
        var employee_name_column = 'A'
        var schedule_name_column = 'A'
        var schedule_date_column = 'B'
        var schedule_start_column = 'C'
        var schedule_end_column = 'D'
        var schedule_job_column = 'E'
    }
    private fun connect(): Database{
        return Database.connect(URL, driver, userName, password)
    }

    suspend fun exportEmployees(): List<Employee>{
        val employees = mutableListOf<Employee>()
        transaction(connect()){
            if(TransactionManager.current().db.dialect.allTablesNames().contains("employees"))
                for(row in Employees.selectAll().toMutableList())
                    employees.add(row.toEmployee())
            else
                throw TableNotYetBuiltException()
        }
        return employees
    }
    suspend fun exportShifts(): List<Shift>{
        val shifts = mutableListOf<Shift>()
        transaction(connect()){
            if(TransactionManager.current().db.dialect.allTablesNames().contains("schedule"))
                for(row in Schedule.selectAll().toMutableList())
                    shifts.add(row.toShift())
            else
                TODO("TABLE NOT FOUND, SCHEDULE, ERROR")
        }
        return shifts
    }
    suspend fun exportShifts(date: LocalDate):List<Shift> = exportShifts().filter { it.date.toDate() == date }

    suspend fun importEmployees(path: String){
        val csvReader: CSVReader
        try {
            csvReader = CSVReader(Files.newBufferedReader(Paths.get(path)))
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        transaction(connect()) {
            if(TransactionManager.current().db.dialect.allTablesNames().contains("employees"))
                return@transaction
            SchemaUtils.create(Employees)
            for(record in csvReader.readAll()){
                Employees.insertIgnore {
                    it[id] = record[employee_id_column.toColumnNumber()].toInt()
                    it[name] =  record[employee_name_column.toColumnNumber()]
                }
            }
        }
    }
    fun generateSchedulePeriod(): List<LocalDate>{
        val week = mutableListOf<LocalDate>()
        var oneDate = "2019-01-01"
        runBlocking {
            try {
                oneDate = exportShifts()[0].date
            } catch (e: IndexOutOfBoundsException) {
                oneDate = LocalDate.now().toString()
            }
        }
        var day = LocalDate.parse(oneDate)
        while(day.dayOfWeek != DayOfWeek.SUNDAY.value)
            day = day.minusDays(1)
        repeat(7){
            week.add(day)
            day = day.plusDays(1)
        }
        return week
    }
    suspend fun importShifts(path: String){
        val csvReader: CSVReader
        try {
            csvReader = CSVReader(Files.newBufferedReader(Paths.get(path)))
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        transaction(connect()) {
            if(TransactionManager.current().db.dialect.allTablesNames().contains("schedule"))
                return@transaction
            SchemaUtils.create(Schedule)
            for(record in csvReader.readAll()){
                Schedule.insert {
                    it[employee] = record[schedule_name_column.toColumnNumber()]
                    it[date] = DateTime.parse(record[schedule_date_column.toColumnNumber()], DateTimeFormat.forPattern("yyyy-MM-dd"))
                    it[start] = DateTime.parse(record[schedule_date_column.toColumnNumber()], DateTimeFormat.forPattern("yyyy-MM-dd"))
                        .withTime(LocalTime.parse(record[schedule_start_column.toColumnNumber()], DateTimeFormat.forPattern("H:mm")))
                    if (LocalTime.parse(record[schedule_end_column.toColumnNumber()]) == LocalTime.MIDNIGHT)
                        it[end] = DateTime.parse(record[schedule_date_column.toColumnNumber()], DateTimeFormat.forPattern("yyyy-MM-dd")).plusDays(1)
                            .withTime(LocalTime.parse(record[schedule_end_column.toColumnNumber()], DateTimeFormat.forPattern("H:mm")))
                    else
                        it[end] = DateTime.parse(record[schedule_date_column.toColumnNumber()], DateTimeFormat.forPattern("yyyy-MM-dd"))
                            .withTime(LocalTime.parse(record[schedule_end_column.toColumnNumber()], DateTimeFormat.forPattern("H:mm")))
                    it[job] = record[schedule_job_column.toColumnNumber()]
                }
            }
        }
    }

}