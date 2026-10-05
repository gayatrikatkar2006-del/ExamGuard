package com.examguard.service

import com.examguard.dto.AllocationResult
import com.examguard.dto.DutyView
import com.examguard.dto.ExamSeatingView
import com.examguard.dto.HallSeating
import com.examguard.dto.InvigilatorView
import com.examguard.dto.MySeatView
import com.examguard.dto.OtherSeat
import com.examguard.dto.SeatView
import com.examguard.exception.BadRequestException
import com.examguard.exception.ConflictException
import com.examguard.model.HallStatus
import com.examguard.model.Invigilation
import com.examguard.model.Role
import com.examguard.model.SeatAllocation
import com.examguard.model.User
import com.examguard.repository.ExamRepository
import com.examguard.repository.HallRepository
import com.examguard.repository.InvigilationRepository
import com.examguard.repository.SeatAllocationRepository
import com.examguard.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AllocationService(
    private val exams: ExamRepository,
    private val halls: HallRepository,
    private val users: UserRepository,
    private val seats: SeatAllocationRepository,
    private val invigilations: InvigilationRepository,
    private val examService: ExamService
) {
    private data class Pos(val hallId: Long, val row: Int, val col: Int)

    // ------------------------------------------------------------------
    // Seating rules
    //   S1  students are placed in roll-number order
    //   S2  only halls that are AVAILABLE are used, in alphabetical order
    //   S3  a seat used by another exam in the same time slot is skipped
    //   S4  alternate-seat rule: two students of the same exam never sit side by side
    //   S5  if everyone cannot be seated, nothing is saved and the admin is told how many are left
    // ------------------------------------------------------------------
    @Transactional
    fun allocateSeats(examId: Long): AllocationResult {
        val exam = examService.find(examId)
        if (seats.existsByExamId(examId)) {
            throw ConflictException("Seats are already allocated for ${exam.code}. Reset the allocation first.")
        }

        val students = users.findByRole(Role.STUDENT)
            .filter { Rules.deptMatches(exam.department, it.department) }
            .sortedBy { it.rollNo ?: "" }
        if (students.isEmpty()) throw BadRequestException("No registered students found for ${exam.department}")

        val taken = HashSet<Pos>()
        exams.findByExamDate(exam.examDate)
            .filter { it.id != exam.id && Rules.overlaps(it, exam) }
            .forEach { other ->
                seats.findByExamId(other.id).forEach { taken.add(Pos(it.hallId, it.seatRow, it.seatCol)) }
            }

        val mine = HashSet<Pos>()
        val plan = ArrayList<SeatAllocation>()
        val queue = ArrayDeque(students)
        val usable = halls.findAll().filter { it.status == HallStatus.AVAILABLE }.sortedBy { it.name }

        for (hall in usable) {
            if (queue.isEmpty()) break
            for (r in 1..hall.rowCount) {
                for (c in 1..hall.colCount) {
                    if (queue.isEmpty()) break
                    val here = Pos(hall.id, r, c)
                    if (here in taken) continue
                    if (Pos(hall.id, r, c - 1) in mine || Pos(hall.id, r, c + 1) in mine) continue
                    val student = queue.removeFirst()
                    mine.add(here)
                    plan.add(SeatAllocation(examId = exam.id, hallId = hall.id, studentId = student.id, seatRow = r, seatCol = c))
                }
            }
        }

        if (queue.isNotEmpty()) {
            throw BadRequestException(
                "Not enough seats: ${queue.size} of ${students.size} students could not be seated " +
                    "with the alternate-seat rule. Add a hall or bring a hall out of maintenance."
            )
        }
        seats.saveAll(plan)
        val hallCount = plan.map { it.hallId }.distinct().size
        return AllocationResult("Seated ${students.size} students in $hallCount hall(s)", students.size, hallCount)
    }

    // ------------------------------------------------------------------
    // Invigilation rules
    //   I1  one invigilator per 30 students in each hall (minimum one)
    //   I2  nobody invigilates an exam of their own department (conflict of interest)
    //   I3  nobody is placed in two exams that overlap in time
    //   I4  nobody exceeds the maximum number of duties
    //   I5  the faculty member with the fewest duties so far is chosen first (fair workload)
    //   I6  the first invigilator in each hall is the CHIEF
    // ------------------------------------------------------------------
    @Transactional
    fun allocateInvigilators(examId: Long): AllocationResult {
        val exam = examService.find(examId)
        val examSeats = seats.findByExamId(examId)
        if (examSeats.isEmpty()) throw BadRequestException("Allocate seats first. Invigilators are assigned per hall.")
        if (invigilations.existsByExamId(examId)) {
            throw ConflictException("Invigilators are already assigned for ${exam.code}. Reset the allocation first.")
        }

        val busy: Set<Long> = exams.findByExamDate(exam.examDate)
            .filter { it.id != exam.id && Rules.overlaps(it, exam) }
            .flatMap { invigilations.findByExamId(it.id) }
            .map { it.facultyId }
            .toSet()

        val duties: MutableMap<Long, Int> =
            invigilations.findAll().groupingBy { it.facultyId }.eachCount().toMutableMap()

        val pool = users.findByRole(Role.FACULTY).filter {
            it.id !in busy && !it.department.equals(exam.department, ignoreCase = true)
        }

        val hallNames = halls.findAllById(examSeats.map { it.hallId }.distinct()).associate { it.id to it.name }
        val perHall = examSeats.groupBy { it.hallId }.entries.sortedBy { hallNames[it.key] ?: "" }

        val chosen = HashSet<Long>()
        val plan = ArrayList<Invigilation>()
        for ((hallId, list) in perHall) {
            val needed = Rules.invigilatorsNeeded(list.size)
            for (i in 0 until needed) {
                val pick: User = pool
                    .filter { it.id !in chosen && (duties[it.id] ?: 0) < Rules.MAX_DUTIES }
                    .minWithOrNull(compareBy<User> { duties[it.id] ?: 0 }.thenBy { it.name })
                    ?: throw BadRequestException(
                        "Not enough eligible faculty. Need ${perHall.sumOf { Rules.invigilatorsNeeded(it.value.size) }} " +
                            "invigilators but only ${pool.size} are free and allowed for ${exam.code}."
                    )
                chosen.add(pick.id)
                duties[pick.id] = (duties[pick.id] ?: 0) + 1
                plan.add(
                    Invigilation(
                        examId = exam.id,
                        hallId = hallId,
                        facultyId = pick.id,
                        duty = if (i == 0) "CHIEF" else "ASSISTANT"
                    )
                )
            }
        }
        invigilations.saveAll(plan)
        return AllocationResult("Assigned ${plan.size} invigilator(s) to ${perHall.size} hall(s)", plan.size, perHall.size)
    }

    @Transactional
    fun reset(examId: Long) {
        examService.find(examId)
        invigilations.deleteByExamId(examId)
        seats.deleteByExamId(examId)
    }

    // ------------------------------------------------------------------
    // Read models
    // ------------------------------------------------------------------
    fun seating(examId: Long): ExamSeatingView {
        val exam = examService.find(examId)
        val seatList = seats.findByExamId(examId)
        val students = users.findAllById(seatList.map { it.studentId }).associateBy { it.id }
        val invList = invigilations.findByExamId(examId)
        val faculty = users.findAllById(invList.map { it.facultyId }).associateBy { it.id }
        val hallMap = halls.findAllById(seatList.map { it.hallId }.distinct()).associateBy { it.id }

        // seats of other exams in the same time slot, so the chart shows the interleaving
        val otherExams = exams.findByExamDate(exam.examDate).filter { it.id != exam.id && Rules.overlaps(it, exam) }
        val otherSeats = otherExams.flatMap { o -> seats.findByExamId(o.id).map { it to o.code } }

        val hallViews = seatList.groupBy { it.hallId }.map { (hid, list) ->
            val h = hallMap.getValue(hid)
            HallSeating(
                hallId = h.id,
                hallName = h.name,
                building = h.building,
                rowCount = h.rowCount,
                colCount = h.colCount,
                seats = list.sortedWith(compareBy<SeatAllocation>({ it.seatRow }, { it.seatCol })).map { s ->
                    val st = students[s.studentId]
                    SeatView(
                        studentId = s.studentId,
                        name = st?.name ?: "Unknown",
                        rollNo = st?.rollNo ?: "-",
                        department = st?.department ?: "",
                        seatRow = s.seatRow,
                        seatCol = s.seatCol,
                        seatLabel = Rules.seatLabel(s.seatRow, s.seatCol)
                    )
                },
                others = otherSeats.filter { it.first.hallId == hid }.map { OtherSeat(it.first.seatRow, it.first.seatCol, it.second) },
                invigilators = invList.filter { it.hallId == hid }.map {
                    val f = faculty[it.facultyId]
                    InvigilatorView(it.facultyId, f?.name ?: "Unknown", f?.department ?: "", it.duty)
                }.sortedBy { it.duty }
            )
        }.sortedBy { it.hallName }

        return ExamSeatingView(examService.get(examId), hallViews)
    }

    fun mySeats(studentId: Long): List<MySeatView> {
        val mine = seats.findByStudentId(studentId)
        val examMap = exams.findAllById(mine.map { it.examId }).associateBy { it.id }
        val hallMap = halls.findAllById(mine.map { it.hallId }.distinct()).associateBy { it.id }
        return mine.mapNotNull { s ->
            val e = examMap[s.examId] ?: return@mapNotNull null
            val h = hallMap[s.hallId] ?: return@mapNotNull null
            MySeatView(
                e.id, e.code, e.name, e.examDate, e.startTime, e.endTime,
                h.name, h.building, h.rowCount, h.colCount, s.seatRow, s.seatCol,
                Rules.seatLabel(s.seatRow, s.seatCol)
            )
        }.sortedWith(compareBy<MySeatView>({ it.examDate }, { it.startTime }))
    }

    fun myDuties(facultyId: Long): List<DutyView> {
        val mine = invigilations.findByFacultyId(facultyId)
        val examMap = exams.findAllById(mine.map { it.examId }).associateBy { it.id }
        val hallMap = halls.findAllById(mine.map { it.hallId }.distinct()).associateBy { it.id }
        return mine.mapNotNull { d ->
            val e = examMap[d.examId] ?: return@mapNotNull null
            val h = hallMap[d.hallId] ?: return@mapNotNull null
            val count = seats.findByExamId(e.id).count { it.hallId == h.id }
            DutyView(e.id, e.code, e.name, e.examDate, e.startTime, e.endTime, h.name, h.building, d.duty, count)
        }.sortedWith(compareBy<DutyView>({ it.examDate }, { it.startTime }))
    }
}
