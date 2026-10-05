package com.examguard.config

import com.examguard.model.Exam
import com.examguard.model.Hall
import com.examguard.model.HallStatus
import com.examguard.model.Role
import com.examguard.model.User
import com.examguard.repository.ExamRepository
import com.examguard.repository.HallRepository
import com.examguard.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalTime

/** Loads demo data on the first start (only when the users table is empty). */
@Component
class DataSeeder(
    private val users: UserRepository,
    private val halls: HallRepository,
    private val exams: ExamRepository,
    private val encoder: PasswordEncoder
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(DataSeeder::class.java)

    override fun run(vararg args: String) {
        if (users.count() > 0) return

        val adminHash = encoder.encode("admin123")
        val facultyHash = encoder.encode("faculty123")
        val studentHash = encoder.encode("student123")

        users.save(User(name = "Exam Cell Admin", email = "admin@examguard.com", password = adminHash, role = Role.ADMIN, department = "EXAM CELL"))

        val faculty = listOf(
            Triple("Dr. Priya Nair", "priya@faculty.com", "CSE"),
            Triple("Prof. Arun Mehta", "arun@faculty.com", "CSE"),
            Triple("Dr. Kavita Rao", "kavita@faculty.com", "ECE"),
            Triple("Prof. Sameer Joshi", "sameer@faculty.com", "ECE"),
            Triple("Dr. Neha Kulkarni", "neha@faculty.com", "MECH"),
            Triple("Prof. Vikram Shah", "vikram@faculty.com", "MECH"),
            Triple("Dr. Isha Verma", "isha@faculty.com", "AIML"),
            Triple("Prof. Rohit Patil", "rohit@faculty.com", "AIML")
        )
        faculty.forEach { (name, email, dept) ->
            users.save(User(name = name, email = email, password = facultyHash, role = Role.FACULTY, department = dept))
        }

        val first = listOf(
            "Rahul", "Sai", "Ananya", "Rohan", "Meera", "Kabir", "Diya", "Aarav", "Ishaan", "Tara",
            "Vihaan", "Nisha", "Arjun", "Pooja", "Karan", "Riya", "Aditya", "Sneha", "Dev", "Kiara"
        )
        val last = listOf("Sharma", "Patil", "Iyer", "Singh", "Desai")
        val perDept = listOf("CSE" to 24, "ECE" to 18, "MECH" to 12, "AIML" to 12)
        var counter = 0
        for ((dept, count) in perDept) {
            for (n in 1..count) {
                val roll = dept + n.toString().padStart(3, '0')
                val name = first[counter % first.size] + " " + last[(counter / first.size + n) % last.size]
                users.save(User(name = name, email = roll.lowercase() + "@student.com", password = studentHash, role = Role.STUDENT, department = dept, rollNo = roll))
                counter++
            }
        }

        halls.saveAll(
            listOf(
                Hall(name = "Hall A", building = "Block 1", rowCount = 5, colCount = 6),
                Hall(name = "Hall B", building = "Block 1", rowCount = 6, colCount = 6),
                Hall(name = "Hall C", building = "Block 2", rowCount = 5, colCount = 8),
                Hall(name = "Hall D", building = "Block 2", rowCount = 5, colCount = 6),
                Hall(name = "Seminar Hall", building = "Main Block", rowCount = 8, colCount = 10, status = HallStatus.MAINTENANCE)
            )
        )

        val today = LocalDate.now()
        val morning = LocalTime.of(9, 0) to LocalTime.of(12, 0)
        exams.saveAll(
            listOf(
                Exam(code = "CS301", name = "Data Structures", department = "CSE", examDate = today.plusDays(7), startTime = morning.first, endTime = morning.second),
                Exam(code = "EC302", name = "Signals and Systems", department = "ECE", examDate = today.plusDays(7), startTime = morning.first, endTime = morning.second),
                Exam(code = "ME303", name = "Thermodynamics", department = "MECH", examDate = today.plusDays(8), startTime = morning.first, endTime = morning.second),
                Exam(code = "AI304", name = "Machine Learning", department = "AIML", examDate = today.plusDays(8), startTime = LocalTime.of(14, 0), endTime = LocalTime.of(17, 0)),
                Exam(code = "MA101", name = "Engineering Mathematics", department = "ALL", examDate = today.plusDays(10), startTime = LocalTime.of(10, 0), endTime = LocalTime.of(13, 0))
            )
        )
        log.info("Demo data loaded: {} users, {} halls, {} exams", users.count(), halls.count(), exams.count())
    }
}
