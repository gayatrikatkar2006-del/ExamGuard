package com.examguard.service

import com.examguard.dto.AdminDashboard
import com.examguard.dto.DutyLoad
import com.examguard.dto.HallUsage
import com.examguard.model.HallStatus
import com.examguard.model.Role
import com.examguard.repository.HallRepository
import com.examguard.repository.InvigilationRepository
import com.examguard.repository.SeatAllocationRepository
import com.examguard.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DashboardService(
    private val users: UserRepository,
    private val halls: HallRepository,
    private val seats: SeatAllocationRepository,
    private val invigilations: InvigilationRepository,
    private val examService: ExamService
) {
    fun admin(): AdminDashboard {
        val allUsers = users.findAll()
        val students = allUsers.filter { it.role == Role.STUDENT }
        val faculty = allUsers.filter { it.role == Role.FACULTY }
        val allHalls = halls.findAll().sortedBy { it.name }
        val allExams = examService.list()

        val examsPerHall = seats.findAll()
            .groupBy { it.hallId }
            .mapValues { entry -> entry.value.map { it.examId }.distinct().size }

        val dutyCount = invigilations.findAll().groupingBy { it.facultyId }.eachCount()

        return AdminDashboard(
            totalStudents = students.size,
            totalFaculty = faculty.size,
            totalHalls = allHalls.size,
            usableCapacity = allHalls.filter { it.status == HallStatus.AVAILABLE }.sumOf { it.capacity },
            totalExams = allExams.size,
            draftExams = allExams.count { it.status == "DRAFT" },
            seatedExams = allExams.count { it.status == "SEATED" },
            readyExams = allExams.count { it.status == "READY" },
            studentsByDepartment = students.groupBy { it.department }.mapValues { it.value.size }.toSortedMap(),
            hallUsage = allHalls.map { HallUsage(it.name, it.capacity, examsPerHall[it.id] ?: 0) },
            dutyLoad = faculty.map { DutyLoad(it.name, it.department, dutyCount[it.id] ?: 0) }
                .sortedByDescending { it.duties },
            upcoming = allExams.filter { !it.examDate.isBefore(LocalDate.now()) }.take(5)
        )
    }
}
