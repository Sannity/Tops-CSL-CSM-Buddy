import com.opencsv.CSVReader
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.LocalTime
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat


data class Shift(val employee: String, val date: String, val startTime: String, val endTime: String, val job: String)
data class Employee(val name: String, val ID: Int)

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
fun ResultRow.toEmployee() = Employee(this[Employees.name], this[Employees.id])

class DatabaseAccess{
    companion object {
        fun getAllShifts(): MutableList<Shift> {
            var rows: MutableList<ResultRow>
            val shifts = mutableListOf<Shift>()
            Database.connect(
                settings["db_URL"],
                driver = "com.mysql.jdbc.Driver",
                user = settings["db_username"],
                password = settings["db_password"]
            )
            transaction {
                rows = Schedule.selectAll().toMutableList()
                for (row in rows) {
                    shifts.add(row.toShift())
                }
            }
            return shifts
        }
        fun getAllEmployees(): MutableList<Employee>{
            var rows: MutableList<ResultRow>
            val people = mutableListOf<Employee>()
            Database.connect(
                settings["db_URL"],
                driver = "com.mysql.jdbc.Driver",
                user = settings["db_username"],
                password = settings["db_password"]
            )
            transaction {
                rows = Employees.selectAll().toMutableList()
                for (row in rows) {
                    people.add(row.toEmployee())
                }
            }
            return people
        }
        fun loadSchedule(filepath: String){
            val csvReader = CSVReader(Files.newBufferedReader(Paths.get(filepath)))
            val records = csvReader.readAll()
            try {
                //read all employees from csv
                //Connect to database and write all employees
                Database.connect(settings["db_URL"], driver = "com.mysql.jdbc.Driver", user = settings["db_username"], password = settings["db_password"])
                transaction {
                    if(!TransactionManager.current().db.dialect.allTablesNames().contains("schedule")) {
                        println("entering")
                        SchemaUtils.create(Schedule)
                        for (record in records) {
                            Schedule.insert {
                                it[employee] = record[settings["schedule_employee_name_column"].toColumnNumber()]
                                it[date] = DateTime.parse(record[settings["schedule_shift_date_column"].toColumnNumber()])
                                it[start] = DateTime.parse(record[settings["schedule_shift_date_column"].toColumnNumber()])
                                    .withTime(LocalTime.parse(record[settings["schedule_shift_time_start_column"].toColumnNumber()]))
                                if (LocalTime.parse(record[settings["schedule_shift_time_end_column"].toColumnNumber()]) == LocalTime.MIDNIGHT)
                                    it[end] = DateTime.parse(record[settings["schedule_shift_date_column"].toColumnNumber()]).plusDays(1)
                                        .withTime(LocalTime.parse(record[settings["schedule_shift_time_end_column"].toColumnNumber()]))
                                else
                                    it[end] = DateTime.parse(record[settings["schedule_shift_date_column"].toColumnNumber()])
                                        .withTime(LocalTime.parse(record[settings["schedule_shift_time_end_column"].toColumnNumber()]))
                                it[job] = record[settings["schedule_job_column"].toColumnNumber()]
                            }
                        }
                    }
                    else{
                        println("Schedule Already Exists, Loading...")
                    }
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
        fun loadEmployees(filepath: String){
            try {
                //read all employees from csv
                val csvReader = CSVReader(Files.newBufferedReader(Paths.get(filepath)))
                val records = csvReader.readAll()

                //Connect to database and write all employees
                Database.connect(settings["db_URL"], driver = "com.mysql.jdbc.Driver", user = settings["db_username"], password = settings["db_password"])
                transaction {
                    SchemaUtils.create(Employees)
                    for(record in records) {
                        Employees.insertIgnore {
                            it[id] = record[settings["employee_profile_id_column"].toColumnNumber()].toInt()
                            it[name] =  record[settings["employee_profile_name_column"].toColumnNumber()]
                        }
                    }
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }
}