import tornadofx.*
import kotlin.system.exitProcess


val settings = ApplicationSettings()

fun main(){
    DatabaseAccess.loadEmployees("C:\\Users\\austi\\Google Drive\\Source Code\\cslbuddy\\src\\main\\resources\\employee_ids.csv")
    DatabaseAccess.loadSchedule("C:\\Users\\austi\\Google Drive\\Source Code\\cslbuddy\\src\\main\\resources\\schedule.csv")
    DatabaseAccess.getAllShifts()
    launch<MyApp>()
    DatabaseAccess.getAllShifts()
}
class MyApp: App(MainView::class)

class MainView: View("CSL Buddy"){
    override val root = borderpane {
        top {
            menubar{
                menu("File"){
                    item("Import Schedule...")
                    item("Generate Lineup...")
                    item("Export Lineup")
                    item("Quit").action { println("Quitting Application!").also { exitProcess(0) } }
                }
            }
        }
        center{
            drawer(multiselect = true) {
                item("Schedule", expanded = true) {
                    tableview(DatabaseAccess.getAllShifts().observable()){
                        readonlyColumn("Employee",Shift::employee)
                        readonlyColumn("Date", Shift::date)
                        readonlyColumn("Start Time", Shift::startTime)
                        readonlyColumn("End Time",Shift::endTime)
                        readonlyColumn("Job",Shift::job)
                    }
                }
                item("Lineup") {

                }
                item("People") {
                    tableview(DatabaseAccess.getAllEmployees().observable()) {
                        readonlyColumn("Name", Employee::name)
                        readonlyColumn("ID", Employee::ID)
                    }
                }
                item("Settings") {
                    form{
                        val dbURL = textfield{
                            promptText = settings["db_URL"]
                        }
                        val dbUsername = textfield{
                            promptText = settings["db_username"]
                        }
                        val dbPassword = passwordfield {
                            promptText = "*".repeat(text.length)
                        }
                        val employeeProfileName = textfield {
                            setMaxSize(30.0, 10.0)
                            promptText = settings["employee_profile_name_column"]
                        }
                        val employeeProfileId = textfield {
                            setMaxSize(30.0, 10.0)
                            promptText = settings["employee_profile_id_column"]
                        }
                        val scheduleName = textfield{
                            setMaxSize(30.0, 10.0)
                            promptText = settings["schedule_employee_name_column"]
                        }
                        val scheduleDate = textfield{
                            setMaxSize(30.0, 10.0)
                            promptText = settings["schedule_shift_date_column"]
                        }
                        val scheduleStartTime = textfield {
                            setMaxSize(30.0, 10.0)
                            promptText = settings["schedule_shift_time_start_column"]
                        }
                        val scheduleEndTime = textfield {
                            setMaxSize(30.0, 10.0)
                            promptText = settings["schedule_shift_time_end_column"]
                        }
                        val scheduleJob = textfield {
                            setMaxSize(30.0, 10.0)
                            promptText = settings["schedule_job_column"]
                        }

                        fieldset("Database Configuration:") {
                            field("JDBC URL") { this += dbURL }
                            field("Username"){ this += dbUsername }
                            field("Password"){ this += dbPassword }
                        }
                        fieldset("Data Positions:"){
                            titledpane("Employee Profile Columns") {
                                vbox{
                                    field("Name") { this += employeeProfileName }
                                    field("ID") { this += employeeProfileId }
                                }
                            }
                            titledpane("Schedule Columns") {
                                vbox{
                                    field("Name") { this += scheduleName }
                                    field("Date") { this += scheduleDate }
                                    field("Start Time") { this += scheduleStartTime }
                                    field("End Time") { this += scheduleEndTime }
                                    field("Job") { this += scheduleJob }
                                }
                            }
                        }
                        button("Save"){
                            action {
                                if(!dbURL.text.isEmpty()) {
                                    settings["db_URL"] = dbURL.text
                                    dbURL.promptText = dbURL.text
                                    dbURL.clear()
                                }
                                if(!dbUsername.text.isEmpty()){
                                    settings["db_username"] = dbUsername.text
                                    dbUsername.promptText = dbUsername.text
                                    dbUsername.clear()
                                }
                                if(!dbPassword.text.isEmpty()){
                                    settings["db_password"] = dbPassword.text
                                    dbPassword.promptText = "*".repeat(dbPassword.text.length)
                                    dbPassword.clear()
                                }
                                if(!employeeProfileName.text.isEmpty()){
                                    settings["employee_profile_name_column"] = employeeProfileName.text
                                    employeeProfileName.promptText = employeeProfileName.text
                                    employeeProfileName.clear()
                                }
                                if(!employeeProfileId.text.isEmpty()){
                                    settings["employee_profile_id_column"] = employeeProfileId.text
                                    employeeProfileId.promptText = employeeProfileId.text
                                    employeeProfileId.clear()
                                }
                                if(!scheduleName.text.isEmpty()){
                                    settings["schedule_employee_name_column"] = scheduleName.text
                                    scheduleName.promptText = scheduleName.text
                                    scheduleName.clear()
                                }
                                if(!scheduleDate.text.isEmpty()){
                                    settings["schedule_shift_date_column"] = scheduleDate.text
                                    scheduleDate.promptText = scheduleDate.text
                                    scheduleDate.clear()
                                }
                                if(!scheduleStartTime.text.isEmpty()){
                                    settings["schedule_shift_time_start_column"] = scheduleStartTime.text
                                    scheduleStartTime.promptText = scheduleStartTime.text
                                    scheduleStartTime.clear()
                                }
                                if(!scheduleEndTime.text.isEmpty()) {
                                    settings["schedule_shift_time_end_column"] = scheduleEndTime.text
                                    scheduleEndTime.promptText = scheduleEndTime.text
                                    scheduleEndTime.clear()
                                }
                                if(!scheduleJob.text.isEmpty()){
                                    settings["schedule_job_column"] = scheduleJob.text
                                    scheduleJob.promptText = scheduleJob.text
                                    scheduleJob.clear()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


