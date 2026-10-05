package com.examguard.service

import com.examguard.model.Exam
import kotlin.math.ceil

/**
 * The rule book. Every allocation decision in ExamGuard is made by these plain rules,
 * so the result is predictable, explainable and easy to defend in a viva.
 */
object Rules {
    /** R1: one invigilator for every 30 students in a hall (minimum one per hall in use). */
    const val STUDENTS_PER_INVIGILATOR = 30

    /** R2: a faculty member may be given at most this many duties in total (fair workload). */
    const val MAX_DUTIES = 6

    /** Students of an exam: its own department, or everyone when the exam is for "ALL". */
    fun deptMatches(examDept: String, studentDept: String): Boolean =
        examDept.equals("ALL", ignoreCase = true) || examDept.equals(studentDept, ignoreCase = true)

    /** Two exams clash for students if their department groups share students. */
    fun departmentsClash(a: String, b: String): Boolean =
        a.equals("ALL", ignoreCase = true) || b.equals("ALL", ignoreCase = true) || a.equals(b, ignoreCase = true)

    /** Two exams overlap when they are on the same day and their time ranges intersect. */
    fun overlaps(a: Exam, b: Exam): Boolean =
        a.examDate == b.examDate && a.startTime < b.endTime && b.startTime < a.endTime

    fun invigilatorsNeeded(studentsInHall: Int): Int =
        ceil(studentsInHall / STUDENTS_PER_INVIGILATOR.toDouble()).toInt().coerceAtLeast(1)

    fun seatLabel(row: Int, col: Int): String = "${'A' + (row - 1)}-$col"
}
