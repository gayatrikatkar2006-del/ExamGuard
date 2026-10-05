package com.examguard.controller

import com.examguard.exception.ConflictException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class DemoLine(val code: String, val result: String)
data class DemoSection(val title: String, val concept: String, val lines: List<DemoLine>)

// Inheritance demo: open base class, two subclasses overriding behaviour.
open class Person(val name: String) {
    open fun describe(): String = "$name is a person"
}

class StudentPerson(name: String, val rollNo: String) : Person(name) {
    override fun describe(): String = "$name ($rollNo) sits the exam"
}

class FacultyPerson(name: String, val dept: String) : Person(name) {
    override fun describe(): String = "$name ($dept) invigilates the exam"
}

/** Every line below is executed live when the page loads, so the output is real. */
@RestController
@RequestMapping("/api")
class KotlinDemoController {

    @GetMapping("/kotlin-demo")
    fun demo(): List<DemoSection> = listOf(dataTypes(), collections(), inheritance(), exceptions())

    private fun dataTypes(): DemoSection {
        val rows = 5
        val cols = 6
        val capacity: Int = rows * cols
        val alternate: Int = rows * ((cols + 1) / 2)
        val perInvigilator: Double = 30.0
        val rollNo: String? = null
        return DemoSection(
            "Data types and null safety",
            "val is read-only, types are inferred, and String? may hold null",
            listOf(
                DemoLine("val capacity: Int = rows * cols", "$capacity seats in a 5 x 6 hall"),
                DemoLine("val alternate = rows * ((cols + 1) / 2)", "$alternate seats usable with the alternate-seat rule"),
                DemoLine("val perInvigilator: Double = 30.0", "1 invigilator per $perInvigilator students"),
                DemoLine("val rollNo: String? = null; rollNo ?: \"unassigned\"", rollNo ?: "unassigned")
            )
        )
    }

    private fun collections(): DemoSection {
        val roll = listOf("CSE004", "CSE001", "CSE003", "CSE002", "CSE005", "CSE006")
        val sorted = roll.sorted()
        val alternate = sorted.filterIndexed { i, _ -> i % 2 == 0 }
        val byPrefix = listOf("CSE001", "ECE001", "CSE002", "ECE002", "MECH001").groupBy { it.takeWhile { c -> c.isLetter() } }
        val duties = mapOf("Priya" to 3, "Arun" to 1, "Kavita" to 2)
        val fewest = duties.minByOrNull { it.value }?.key
        val totalDuties = duties.values.reduce { a, b -> a + b }
        return DemoSection(
            "Collections: filter, map, groupBy, reduce",
            "Functional operations on lists and maps replace manual loops",
            listOf(
                DemoLine("roll.sorted()", sorted.joinToString(", ")),
                DemoLine("sorted.filterIndexed { i, _ -> i % 2 == 0 }", alternate.joinToString(", ") + "  (alternate seats)"),
                DemoLine("list.groupBy { it.takeWhile { c -> c.isLetter() } }", byPrefix.mapValues { it.value.size }.toString()),
                DemoLine("duties.minByOrNull { it.value }?.key", "$fewest has the fewest duties, so is picked first"),
                DemoLine("duties.values.reduce { a, b -> a + b }", "$totalDuties duties in total")
            )
        )
    }

    private fun inheritance(): DemoSection {
        val people: List<Person> = listOf(
            StudentPerson("Rahul", "CSE001"),
            FacultyPerson("Dr. Priya", "CSE"),
            Person("Visitor")
        )
        return DemoSection(
            "Classes and inheritance",
            "open classes can be extended; subclasses override behaviour (polymorphism)",
            people.map { DemoLine("${it::class.simpleName}.describe()", it.describe()) }
        )
    }

    private fun exceptions(): DemoSection {
        val lines = mutableListOf<DemoLine>()

        val a = try {
            val start = 12
            val end = 9
            require(end > start) { "End time must be after start time" }
            "no error"
        } catch (e: IllegalArgumentException) {
            "caught IllegalArgumentException: ${e.message}"
        }
        lines.add(DemoLine("try { require(end > start) } catch (e: IllegalArgumentException)", a))

        val b = runCatching { throw ConflictException("Hall A is already used in this slot") }
            .exceptionOrNull()
        lines.add(DemoLine("throw ConflictException(...)  // custom exception, becomes HTTP 409", "caught ${b?.javaClass?.simpleName}: ${b?.message}"))

        val c = try {
            "ABC".toInt()
        } catch (e: NumberFormatException) {
            -1
        } finally {
            // finally always runs
        }
        lines.add(DemoLine("try { \"ABC\".toInt() } catch (e: NumberFormatException) { -1 }", "returns $c (try is an expression)"))

        return DemoSection("Exception handling", "try / catch / finally, require(), and a custom exception hierarchy", lines)
    }
}
