package com.examguard.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalTime

enum class Role { ADMIN, FACULTY, STUDENT }

enum class HallStatus { AVAILABLE, MAINTENANCE }

/** Table 1: users (admin, faculty and students share one table, split by role). */
@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    var name: String = "",
    @Column(unique = true, nullable = false) var email: String = "",
    var password: String = "",
    @Enumerated(EnumType.STRING) var role: Role = Role.STUDENT,
    var department: String = "",
    var rollNo: String? = null
)

/** Table 2: halls. Seats are a rowCount x colCount grid. */
@Entity
@Table(name = "halls")
class Hall(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(unique = true, nullable = false) var name: String = "",
    var building: String = "",
    var rowCount: Int = 1,
    var colCount: Int = 1,
    @Enumerated(EnumType.STRING) var status: HallStatus = HallStatus.AVAILABLE
) {
    val capacity: Int get() = rowCount * colCount
}

/** Table 3: exams (a department of "ALL" means a common subject for every student). */
@Entity
@Table(name = "exams")
class Exam(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(unique = true, nullable = false) var code: String = "",
    var name: String = "",
    var department: String = "",
    var examDate: LocalDate = LocalDate.now(),
    var startTime: LocalTime = LocalTime.of(9, 0),
    var endTime: LocalTime = LocalTime.of(12, 0)
)

/** Table 4: seat_allocations (one row = one student in one seat for one exam). */
@Entity
@Table(
    name = "seat_allocations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["exam_id", "student_id"])]
)
class SeatAllocation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    var examId: Long = 0,
    var hallId: Long = 0,
    var studentId: Long = 0,
    var seatRow: Int = 0,
    var seatCol: Int = 0
)

/** Table 5: invigilations (one row = one faculty member on duty in one hall for one exam). */
@Entity
@Table(
    name = "invigilations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["exam_id", "faculty_id"])]
)
class Invigilation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    var examId: Long = 0,
    var hallId: Long = 0,
    var facultyId: Long = 0,
    var duty: String = "ASSISTANT"
)
