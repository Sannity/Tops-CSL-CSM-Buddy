import com.github.timrs2998.pdfbuilder.document
import javafx.collections.FXCollections
import javafx.collections.ObservableArray
import javafx.geometry.Side
import javafx.stage.FileChooser

import kotlinx.coroutines.runBlocking

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.vandeseer.easytable.structure.TableNotYetBuiltException
import tornadofx.*
import kotlin.system.exitProcess

fun main(){
    launch<MyApp>()
}

class MyApp: App(MainView::class)

class MainView: View("CSL Buddy"){
    val accessor = DatabaseAccessor()
    init {
        runBlocking {
            try {
                accessor.exportEmployees()
                accessor.exportShifts()
            } catch (e: TableNotYetBuiltException) {
                var fileChooser = FileChooser()
                fileChooser.title = "Open Schedule"
                var fileFilter = FileChooser.ExtensionFilter("Comma Separated Values File", "*.csv")
                fileChooser.extensionFilters.add(fileFilter)
                var chooser = chooseFile("Choose Schedule CSV", fileChooser.extensionFilters.toTypedArray())
                runBlocking {
                    accessor.importShifts(chooser[0].path)
                }
                warning("WARNING", "You must reload the application to view imported values")
                fileChooser.title = "Open Employees"
                fileFilter = FileChooser.ExtensionFilter("Comma Separated Values File", "*.csv")
                fileChooser.extensionFilters.add(fileFilter)
                chooser = chooseFile("Choose Employees CSV", fileChooser.extensionFilters.toTypedArray())
                runBlocking {
                    accessor.importEmployees(chooser[0].path)
                }
                warning("WARNING", "You must reload the application to view imported values")
            }

        }
    }
    override val root = borderpane{

        top{
            menubar{
                menu("File"){
                    menu("Import"){
                        item("Schedule").action {
                            val fileChooser = FileChooser()
                            fileChooser.title = "Open Schedule"
                            val fileFilter = FileChooser.ExtensionFilter("Comma Separated Values File", "*.csv")
                            fileChooser.extensionFilters.add(fileFilter)
                            val chooser = chooseFile("Choose Schedule CSV", fileChooser.extensionFilters.toTypedArray())
                            runBlocking {
                                accessor.importShifts(chooser[0].path)
                            }
                            warning("WARNING", "You must reload the application to view imported values")
                        }
                        item("Employee List").action {
                            val fileChooser = FileChooser()
                            fileChooser.title = "Open Employees"
                            val fileFilter = FileChooser.ExtensionFilter("Comma Separated Values File", "*.csv")
                            fileChooser.extensionFilters.add(fileFilter)
                            val chooser = chooseFile("Choose Employees CSV", fileChooser.extensionFilters.toTypedArray())
                            runBlocking {
                                accessor.importEmployees(chooser[0].path)
                            }
                            warning("WARNING", "You must reload the application to view imported values")
                        }
                    }
                    item("Quit").action { exitProcess(0) }
                }
            }
        }
        center {
            drawer {
                dockingSide = Side.TOP
                //multiselect = true

                item("Employees") {
                    var employees = FXCollections.observableArrayList(listOf<Employee>())
                    runBlocking {
                        employees = FXCollections.observableArrayList(accessor.exportEmployees())
                    }
                    val table = tableview(employees) {
                        readonlyColumn("Employee", Employee::name)
                        readonlyColumn("ID", Employee::ID)
                    }
                    button("View Statistics").action {
                        information("Statistics", "Number of Employees: ${employees.count()}")
                    }
                }
                item("Schedule") {
                    expanded = true
                    var schedule = FXCollections.observableArrayList(listOf<Shift>())
                    runBlocking {
                        schedule = FXCollections.observableArrayList(accessor.exportShifts())
                    }
                    tableview(schedule) {
                        readonlyColumn("Employee", Shift::employee)
                        readonlyColumn("Date", Shift::date)
                        readonlyColumn("Start Time", Shift::startTime)
                        readonlyColumn("End Time", Shift::endTime)
                        readonlyColumn("Job", Shift::job)
                        smartResize()
                    }
                    button("Shuffle").action { schedule.shuffle() }
                }
                item("Lineup") {
                    val builder = LineupBuilder()
                    val chooser = combobox<LocalDate> {
                        items = accessor.generateSchedulePeriod().observable()
                        selectionModel.selectFirst()
                    }
                    button("Generate") {
                        action {
                            builder.lineup.toDocument(chooser.selectedItem!!)
                        }
                    }
                }
            }
        }
    }
}