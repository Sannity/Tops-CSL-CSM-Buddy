data class Employee(val firstName: String, val lastName: String){
    fun getFullName() = "$firstName $lastName"
}
data class Shift(val begin: Int, val end: Int, val role: String){
    fun length() = end - begin
}
class Lineup{
    val lineup = mutableListOf<Pair<Employee, Shift>>()
    constructor()
    constructor(lineupTemplate: String) {
        val test= lineupTemplate.split("\n")
        println(test)
        for (i in test)
            add(i.toEmployeeShift())
    }
    fun add(input: Pair<Employee, Shift>){
        lineup.add(input)
    }
    override fun toString() = lineup.toString()
}
class Schedule(){
    var schedule = arrayOf<Array<Pair<Employee, Shift>>>()

}

fun String.toEmployeeShift(): Pair<Employee, Shift>{
    val words = this.split(" ")
    if(words.isEmpty() || (words.count() != 5))
        TODO("Error Checking")
    fun List<String>.toEmployeeShift() = Pair(Employee(this[0], this[1]), Shift(this[2].toInt(), this[3].toInt(), this[4]))
    return words.toEmployeeShift()
}

fun main() {
    //val austin = Employee(firstName = "Austin", lastName = "Monson", trained = listOf("Cashier", "Office", "CSL", "CC"))
    val employ = "Austin Monson 0900 1500 C\nAudrey Monson 0900 1500 C"
    //println(employ.toEmployeeShift())
    val lineup = Lineup(employ)
    println(lineup)
}