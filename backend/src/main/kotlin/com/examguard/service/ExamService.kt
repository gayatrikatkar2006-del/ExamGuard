package com.examguard.service

import com.examguard.dto.ExamRequest
import com.examguard.dto.ExamView
import com.examguard.exception.BadRequestException
import com.examguard.exception.ConflictException
import com.examguard.exception.NotFoundException
import com.examguard.model.Exam
import com.examguard.model.Role
import com.examguard.repository.ExamRepository
import com.examguard.repository.InvigilationRepository
import com.examguard.repository.SeatAllocationRepository
import com.examguard.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ExamService(
    private val exams: ExamRepository,
    private val users: UserRepository,
    private val seats: SeatAllocationRepository,
    private val invigilations: InvigilationRepository
) {
    fun find(id: Long): Exam = exams.findById(id).orElseThrow { NotFoundException("Exam not found") }

    fun list(): List<ExamView> =
        toViews(exams.findAll().sortedWith(compareBy<Exam>({ it.examDate }, { it.startTime })))

    fun get(id: Long): ExamView = toViews(listOf(find(id))).first()

    @Transactional
    fun create(req: ExamRequest): ExamView {
        val date = req.examDate ?: throw BadRequestException("Exam date is required")
        val start = req.startTime ?: throw BadRequestException("Start time is required")
        val end = req.endTime ?: throw BadRequestException("End time is required")
        if (!end.isAfter(start)) throw BadRequestException("End time must be after start time")

        val code = req.code.trim().uppercase()
        if (exams.existsByCode(code)) throw ConflictException("Exam code $code already exists")

        val candidate = Exam(
            code = code,
            name = req.name.trim(),
            department = req.department.trim().uppercase(),
            examDate = date,
            startTime = start,
            endTime = end
        )

        // Rule: students must never have two exams at the same time.
        val clash = exams.findByExamDate(date).firstOrNull {
            Rules.overlaps(it, candidate) && Rules.departmentsClash(it.department, candidate.department)
        }
        if (clash != null) {
            throw ConflictException(
                "Clash: ${clash.code} (${clash.department}) is already scheduled from ${clash.startTime} to ${clash.endTime} " +
                    "and shares students with this exam"
            )
        }
        return toViews(listOf(exams.save(candidate))).first()
    }

    @Transactional
    fun delete(id: Long) {
        find(id)
        invigilations.deleteByExamId(id)
        seats.deleteByExamId(id)
        exams.deleteById(id)
    }

    fun toViews(list: List<Exam>): List<ExamView> {
        val students = users.findByRole(Role.STUDENT)
        return list.map { e ->
            val seatList = seats.findByExamId(e.id)
            val assigned = invigilations.countByExamId(e.id).toInt()
            val required = seatList.groupBy { it.hallId }.values.sumOf { Rules.invigilatorsNeeded(it.size) }
            val status = when {
                seatList.isEmpty() -> "DRAFT"
                assigned >= required -> "READY"
                else -> "SEATED"
            }
            ExamView(
                id = e.id,
                code = e.code,
                name = e.name,
                department = e.department,
                examDate = e.examDate,
                startTime = e.startTime,
                endTime = e.endTime,
                enrolled = students.count { Rules.deptMatches(e.department, it.department) },
                seated = seatList.size,
                invigilatorsAssigned = assigned,
                invigilatorsRequired = required,
                status = status
            )
        }
    }
}
