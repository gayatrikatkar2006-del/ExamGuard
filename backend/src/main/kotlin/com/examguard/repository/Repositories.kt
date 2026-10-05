package com.examguard.repository

import com.examguard.model.Exam
import com.examguard.model.Hall
import com.examguard.model.Invigilation
import com.examguard.model.Role
import com.examguard.model.SeatAllocation
import com.examguard.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByRollNo(rollNo: String): Boolean
    fun findByRole(role: Role): List<User>
}

interface HallRepository : JpaRepository<Hall, Long> {
    fun findByName(name: String): Hall?
}

interface ExamRepository : JpaRepository<Exam, Long> {
    fun findByExamDate(examDate: LocalDate): List<Exam>
    fun existsByCode(code: String): Boolean
}

interface SeatAllocationRepository : JpaRepository<SeatAllocation, Long> {
    fun findByExamId(examId: Long): List<SeatAllocation>
    fun findByStudentId(studentId: Long): List<SeatAllocation>
    fun existsByExamId(examId: Long): Boolean
    fun existsByHallId(hallId: Long): Boolean
    fun countByExamId(examId: Long): Long
    fun deleteByExamId(examId: Long)
}

interface InvigilationRepository : JpaRepository<Invigilation, Long> {
    fun findByExamId(examId: Long): List<Invigilation>
    fun findByFacultyId(facultyId: Long): List<Invigilation>
    fun existsByExamId(examId: Long): Boolean
    fun countByExamId(examId: Long): Long
    fun deleteByExamId(examId: Long)
}
