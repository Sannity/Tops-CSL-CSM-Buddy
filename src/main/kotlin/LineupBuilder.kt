import com.github.timrs2998.pdfbuilder.*
import com.github.timrs2998.pdfbuilder.style.*
import kotlinx.coroutines.runBlocking

import org.joda.time.Duration
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import java.awt.Color
import java.util.*
import javax.sound.sampled.Line
import javax.swing.text.Position

class InAndOut(val inTime: LocalTime, val outTime: LocalTime){
    override fun toString() = "${inTime.toString("H:mm")}/${outTime.toString("H:mm")}"
}

fun String.toTime() = LocalTime.parse(this, DateTimeFormat.forPattern("H:mm"))
fun String.toDate() = LocalDate.parse(this, DateTimeFormat.forPattern("yyyy-MM-dd"))

class AssignablePosition(var employee: String, var inAndOut: InAndOut){
    override fun toString() = "$employee ($inAndOut)"
}

class Register(val number: Int, val UScanIdentifier: Boolean = false) {
    var firstPosition = AssignablePosition("--", InAndOut(LocalTime.parse("0:00"), LocalTime.parse("0:00")))
    var secondPosition = AssignablePosition("--", InAndOut(LocalTime.parse("0:00"), LocalTime.parse("0:00")))
    var thirdPosition = AssignablePosition("--", InAndOut(LocalTime.parse("0:00"), LocalTime.parse("0:00")))
    var fourthPosition = AssignablePosition("--", InAndOut(LocalTime.parse("0:00"), LocalTime.parse("0:00")))

    fun assign(shift: Shift, position: Int){
        when(position){
            0 -> firstPosition = AssignablePosition(shift.employee, InAndOut(shift.startTime.toTime(), shift.endTime.toTime()))
            1 -> secondPosition = AssignablePosition(shift.employee, InAndOut(shift.startTime.toTime(), shift.endTime.toTime()))
            2 -> thirdPosition = AssignablePosition(shift.employee, InAndOut(shift.startTime.toTime(), shift.endTime.toTime()))
            3 -> fourthPosition = AssignablePosition(shift.employee, InAndOut(shift.startTime.toTime(), shift.endTime.toTime()))
        }
    }
    fun firstOpenPosition(): Int{
        var result = -1
        when{
            firstPosition.employee == "--" -> result = 0
            secondPosition.employee == "--" -> result = 1
            thirdPosition.employee == "--" -> result = 2
            fourthPosition.employee == "--" -> result = 3
        }
        return result
    }
    fun lastUsedPosition(): Int{
        var result = -1
        when{
            fourthPosition.employee != "--" -> result = 0
            thirdPosition.employee != "--" -> result = 1
            secondPosition.employee != "--" -> result = 2
            firstPosition.employee != "--" -> result = 3
        }
        return result
    }
    operator fun get(position: Int): AssignablePosition{
        when(position){
            0 -> return firstPosition
            1 -> return secondPosition
            2 -> return thirdPosition
            3 -> return fourthPosition
            else -> throw InputMismatchException()
        }
    }


    override fun toString() = "$firstPosition|$secondPosition|$thirdPosition|$fourthPosition|\n"
}

class Lineup{
    val accessor = DatabaseAccessor()
    val date = "2019-01-01".toDate()
    var registers = mutableListOf<Register>()
    var availableShifts = mutableListOf<Shift>()
    init {
        for (i in 1..9)
            if(i == 9)
                registers.add(Register(i, true))
            else
                registers.add(Register(i))
    }
    fun addShifts(shifts: List<Shift>){
        availableShifts.addAll(shifts)
    }
    private fun TableElement.addHeaderRow(){
        this.row {
            text("#"){
                border= Border(1f,1f,1f,1f, Color.BLACK)
                horizontalAlignment = Alignment.CENTER
                padding = Padding(bottom = 5f)
            }
            repeat(4){
                text("Cashier"){
                    border= Border(1f,1f,1f,1f, Color.BLACK)
                    horizontalAlignment = Alignment.CENTER
                    padding = Padding(bottom = 5f)
                }
                text("In/Out"){
                    border= Border(1f,1f,1f,1f, Color.BLACK)
                    horizontalAlignment = Alignment.CENTER
                    padding = Padding(bottom = 5f)
                }
            }
            text("+/-"){
                border= Border(1f,1f,1f,1f, Color.BLACK)
                horizontalAlignment = Alignment.CENTER
                padding = Padding(bottom = 5f)
            }
        }
    }
    private fun TableElement.addBreaks(employee: Map.Entry<String, List<String>>){
        this.row {
            text(employee.key){
                //border= Border(1f,1f,1f,1f, Color.BLACK)
                horizontalAlignment = Alignment.RIGHT
                padding = Padding(bottom = 5f)
            }
            text(employee.value.toString()){
                //border= Border(1f,1f,1f,1f, Color.BLACK)
                horizontalAlignment = Alignment.LEFT
                padding = Padding(bottom = 5f)
            }
        }
    }
    fun toDocument(date: LocalDate){
        val builder = LineupBuilder()
        this.registers = builder.build(date).registers
        val document = document {
            orientation = Orientation.LANDSCAPE
            margin = Margin(20f, 20f, 20f, 20f)

            //Title
            text("Daily Lane Assignment"){
                horizontalAlignment = Alignment.CENTER
            }

            //Day/Date
            text("Day: ${LocalDate.parse(date.toString()).dayOfWeek}         Date: ${LocalDate.parse(date.toString())}"){
                horizontalAlignment = Alignment.CENTER
            }

            //Break Assignments
            var breakAssignments = mapOf<String, List<String>>()
            runBlocking {
                breakAssignments = builder.assignBreaks(accessor.exportShifts(date))
            }
            val breakTable = table {
                header {
                    text("Name----"){
                        horizontalAlignment = Alignment.RIGHT
                        padding = Padding(bottom = 5f)
                    }
                    text("Break(s)"){
                        horizontalAlignment = Alignment.LEFT
                        padding = Padding(bottom = 5f)
                    }
                }
            }
            for(employee in breakAssignments){
                breakTable.addBreaks(employee)
            }

            //Lane Assignments

            val laneTable = table { addHeaderRow() }

            for(register in registers) {
                laneTable.row {
                    if (register.number <= 8)
                        text(register.number.toString()){
                            border= Border(1f,1f,1f,1f, Color.BLACK)
                            horizontalAlignment = Alignment.CENTER
                            padding = Padding(bottom = 5f)
                        }
                    else
                        text("U"){
                            border= Border(1f,1f,1f,1f, Color.BLACK)
                            horizontalAlignment = Alignment.CENTER
                            padding = Padding(bottom = 5f)
                        }
                    for (i in 0 until 4) {
                        text(register[i].employee) {
                            border= Border(1f,1f,1f,1f, Color.BLACK)
                            horizontalAlignment = Alignment.CENTER
                            padding = Padding(bottom = 5f)
                        }
                        text(register[i].inAndOut.toString()){
                            border= Border(1f,1f,1f,1f, Color.BLACK)
                            horizontalAlignment = Alignment.CENTER
                            padding = Padding(bottom = 5f)
                        }
                    }
                    text(""){
                        border= Border(1f,1f,1f,1f, Color.BLACK)
                        horizontalAlignment = Alignment.CENTER
                        padding = Padding(bottom = 5f)
                    }
                }
            }

        }
        document.save("lineup.pdf")

    }
    operator fun get(register: Int) = registers[register]
    override fun toString() = registers.toString()
}

class LineupBuilder{
    val lineup = Lineup()
    fun List<Shift>.findClosestTo(time: LocalTime): Shift{
        var closest = this[0]
        for(shift in this){
            //print("Current: ${closest.employee} ${(Math.abs(Duration.between(LocalTime.parse(closest.startTime, DateTimeFormatter.ofPattern("H:mm")), LocalTime.parse(shift.startTime, DateTimeFormatter.ofPattern("H:mm"))).toMinutes()))} Employee: ${shift.employee} Start: ${shift.startTime} Distance From Time: ${(Math.abs(Duration.between(time, LocalTime.parse(shift.startTime, DateTimeFormatter.ofPattern("H:mm"))).toMinutes()))}\n")
            if((Math.abs(Duration(time.toDateTimeToday(), LocalTime.parse(shift.startTime, DateTimeFormat.forPattern("H:mm")).toDateTimeToday()).standardMinutes)) <
                (Math.abs(Duration(time.toDateTimeToday(), LocalTime.parse(closest.startTime, DateTimeFormat.forPattern("H:mm")).toDateTimeToday()).standardMinutes)))
                closest = shift
        }
        return closest
    }
    fun Shift.hasReplacement(shifts: List<Shift>) = shifts.any { it.startTime == this.endTime }
    fun Shift.getReplacement(shifts: List<Shift>) = shifts.first { it.startTime == this.endTime }

    private fun placeCashiers(workers: List<Shift>): List<Shift>{
        var cashiers = workers.filter { it.job == "EZ-SCANS" || it.job == "CASHIER" }.sortedBy { it.startTime.toTime() }
        lineup[7].assign(cashiers.first(),0).also { cashiers = cashiers - cashiers.first() }
        if(cashiers.first().endTime.toTime() > "12:00".toTime() && lineup[7][0].inAndOut.outTime == "12:00".toTime()){
            lineup[8][0].employee = cashiers.first().employee
            lineup[8][0].inAndOut = InAndOut(cashiers.first().startTime.toTime(), "12:00".toTime())
            lineup[7][1].employee = cashiers.first().employee
            lineup[7][1].inAndOut = InAndOut("12:00".toTime(), cashiers.first().endTime.toTime())
            cashiers = cashiers - cashiers.first()
        }
        if(cashiers.first().endTime.toTime() < "12:00".toTime()) {
            lineup[8].assign(cashiers.first(), 0)
            cashiers = cashiers - cashiers.first()
        }
        if(cashiers.filter { it.startTime.toTime() == "12:00".toTime() && it.endTime.toTime() == "18:00".toTime() } .count() >= 1){
            lineup[8].assign(cashiers.filter { it.startTime.toTime() == "12:00".toTime() && it.endTime.toTime() == "18:00".toTime() } .first(), 1)
            cashiers = cashiers - cashiers.filter { it.startTime.toTime() == "12:00".toTime() && it.endTime.toTime() == "18:00".toTime() } .first()
        }
        if(cashiers.filter { it.startTime.toTime() == "18:00".toTime() && it.endTime.toTime() == "0:00".toTime() } .count() >= 1){
            lineup[8].assign(cashiers.filter { it.startTime.toTime() == "18:00".toTime() && it.endTime.toTime() == "0:00".toTime() } .first(), 2)
            cashiers = cashiers - cashiers.filter { it.startTime.toTime() == "18:00".toTime() && it.endTime.toTime() == "0:00".toTime() } .first()
        }

        lineup[0].assign(cashiers.findClosestTo("9:00".toTime()), 0)
        if(cashiers.findClosestTo("9:00".toTime()).hasReplacement(cashiers)){
            lineup[0].assign(cashiers.findClosestTo("9:00".toTime()).getReplacement(cashiers), 1)
            cashiers = cashiers - cashiers.findClosestTo("9:00".toTime()).getReplacement(cashiers)
            cashiers = cashiers - cashiers.findClosestTo("9:00".toTime())
        }

        val isEven: Boolean = workers[0].date.toDate().dayOfMonth%2 == 0
        val priorityRegisters = if(isEven) listOf(1, 3, 5, 7) else listOf(2, 4, 6)
        println(priorityRegisters)

       //TRy to place them 3 times, after that they just get thrown out
        repeat(2) {
            for (register in priorityRegisters) {
                //does the current have areplacment
                // yes, put them there
                // no, see if anyone will fit
                if (lineup[register].lastUsedPosition() == -1)
                    lineup[register].assign(cashiers.first(), 0).also { cashiers = cashiers - cashiers.first() }
                else if (cashiers.any { it.startTime.toTime() >= lineup[register][lineup[register].lastUsedPosition()].inAndOut.outTime })
                    lineup[register].assign(
                        cashiers.first { it.startTime.toTime() >= lineup[register][lineup[register].lastUsedPosition()].inAndOut.outTime },
                        lineup[register].firstOpenPosition()
                    ).also { cashiers = cashiers - cashiers.first() }


            }
        }
        return cashiers
    }
    fun assignBreaks(workers: List<Shift>): Map<String, List<String>>{
        val workersSorted = workers.sortedBy { LocalTime.parse(it.startTime, DateTimeFormat.forPattern("H:mm")) }.filter { it.job == "CASHIER" || it.job == "EZ-SCANS" }
        val breakMap = mutableMapOf<String, List<String>>()
        for(worker in workersSorted){
            val start = LocalTime.parse(worker.startTime, DateTimeFormat.forPattern("H:mm"))
            val end = LocalTime.parse(worker.endTime, DateTimeFormat.forPattern("H:mm"))
            val length = Duration(start.toDateTimeToday(), end.toDateTimeToday()).standardMinutes/60f
            //var breaks = listOf<Int>()
            val breaks = mutableListOf<Pair<LocalTime, LocalTime>>()
            //breaks.add(Pair(LocalTime.parse(worker.startTime, DateTimeFormatter.ofPattern("H:mm")), LocalTime.parse(worker.endTime, DateTimeFormatter.ofPattern("H:mm"))))
            when{
                length < 6f ->{
                    breaks.add(Pair(start.plusMinutes((Duration(start.toDateTimeToday(), end.toDateTimeToday()).standardMinutes/2).toInt()), start.plusMinutes((Duration(start.toDateTimeToday(), end.toDateTimeToday()).standardMinutes/2).toInt()).plusMinutes(15)))
                }
                length == 6f ->{
                    breaks.add(Pair(start.plusMinutes((Duration(start.toDateTimeToday(), end.toDateTimeToday()).standardMinutes/2).toInt()), start.plusMinutes((Duration(start.toDateTimeToday(), end.toDateTimeToday()).standardMinutes/2).toInt()).plusMinutes(20)))
                }
                length > 6f -> {
                    val middle = start.plusMinutes((Duration(start.toDateTimeToday(), end.toDateTimeToday()).standardMinutes/2).toInt())
                    breaks.add(Pair(middle.minusHours(length.toInt()/4), middle.minusHours(length.toInt()/4).plusMinutes(15)))
                    breaks.add(Pair(middle, middle.plusMinutes(30)))
                    breaks.add(Pair(middle.plusHours(length.toInt()/4), middle.plusHours(length.toInt()/4).plusMinutes(15)))
                }
            }
            val output = mutableListOf<String>()
            for(shiftBreak in breaks){
                output.add(shiftBreak.first.toString("H:mm"))
            }
            breakMap.put(worker.employee, output.toList())
        }

        return breakMap
    }
    fun build(date: LocalDate): Lineup{
        val dataAccess = DatabaseAccessor()
        runBlocking {
            lineup.addShifts(dataAccess.exportShifts(date))
        }
        println("Couldnt place: ${placeCashiers(lineup.availableShifts)}")

        return lineup
    }
}


