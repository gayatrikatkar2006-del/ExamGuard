package com.examguard

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ExamGuardApplication

fun main(args: Array<String>) {
    runApplication<ExamGuardApplication>(*args)
}
