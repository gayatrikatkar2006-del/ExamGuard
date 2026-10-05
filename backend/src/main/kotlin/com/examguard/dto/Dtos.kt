package com.examguard.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalTime

// ---------- Auth ----------
data class RegisterRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 3, message = "Name must be at least 3 characters")
    val name: String = "",
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Enter a valid email address")
    val email: String = "",
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val password: String = "",
    @field:NotBlank(message = "Department is required")
    val department: String = "",
    val role: String? = null,
    val rollNo: String? = null
)

data class LoginRequest(
    @field:NotBlank(message = "Email is required") val email: String = "",
    @field:NotBlank(message = "Password is required") val password: String = ""
)

data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val role: String,
    val department: String,
    val rollNo: String?
)

data class AuthResponse(val token: String, val user: UserDto)

// ---------- Halls and exams ----------
data class HallRequest(
    @field:NotBlank(message = "Hall name is required") val name: String = "",
    @field:NotBlank(message = "Building is required") val building: String = "",
    @field:Min(value = 1, message = "Rows must be between 1 and 26")
    @field:Max(value = 26, message = "Rows must be between 1 and 26")
    val rowCount: Int = 0,
    @field:Min(value = 1, message = "Columns must be between 1 and 20")
    @field:Max(value = 20, message = "Columns must be between 1 and 20")
    val colCount: Int = 0,
    val status: String? = null
)

data class ExamRequest(
    @field:NotBlank(message = "Exam code is required") val code: String = "",
    @field:NotBlank(message = "Subject name is required") val name: String = "",
    @field:NotBlank(message = "Department is required") val department: String = "",
    @field:NotNull(message = "Exam date is required") val examDate: LocalDate? = null,
    @field:NotNull(message = "Start time is required") val startTime: LocalTime? = null,
    @field:NotNull(message = "End time is required") val endTime: LocalTime? = null
)

data class ExamView(
    val id: Long,
    val code: String,
    val name: String,
    val department: String,
    val examDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val enrolled: Int,
    val seated: Int,
    val invigilatorsAssigned: Int,
    val invigilatorsRequired: Int,
    val status: String
)

// ---------- Allocation output ----------
data class AllocationResult(val message: String, val students: Int, val halls: Int)

data class SeatView(
    val studentId: Long,
    val name: String,
    val rollNo: String,
    val department: String,
    val seatRow: Int,
    val seatCol: Int,
    val seatLabel: String
)

data class OtherSeat(val seatRow: Int, val seatCol: Int, val examCode: String)

data class InvigilatorView(val facultyId: Long, val name: String, val department: String, val duty: String)

data class HallSeating(
    val hallId: Long,
    val hallName: String,
    val building: String,
    val rowCount: Int,
    val colCount: Int,
    val seats: List<SeatView>,
    val others: List<OtherSeat>,
    val invigilators: List<InvigilatorView>
)

data class ExamSeatingView(val exam: ExamView, val halls: List<HallSeating>)

data class MySeatView(
    val examId: Long,
    val examCode: String,
    val examName: String,
    val examDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val hallName: String,
    val building: String,
    val rowCount: Int,
    val colCount: Int,
    val seatRow: Int,
    val seatCol: Int,
    val seatLabel: String
)

data class DutyView(
    val examId: Long,
    val examCode: String,
    val examName: String,
    val examDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val hallName: String,
    val building: String,
    val duty: String,
    val studentsInHall: Int
)

// ---------- Dashboard ----------
data class HallUsage(val hall: String, val capacity: Int, val examsHosted: Int)
data class DutyLoad(val faculty: String, val department: String, val duties: Int)

data class AdminDashboard(
    val totalStudents: Int,
    val totalFaculty: Int,
    val totalHalls: Int,
    val usableCapacity: Int,
    val totalExams: Int,
    val draftExams: Int,
    val seatedExams: Int,
    val readyExams: Int,
    val studentsByDepartment: Map<String, Int>,
    val hallUsage: List<HallUsage>,
    val dutyLoad: List<DutyLoad>,
    val upcoming: List<ExamView>
)
