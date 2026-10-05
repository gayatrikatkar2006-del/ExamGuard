package com.examguard.service

import com.examguard.dto.HallRequest
import com.examguard.exception.BadRequestException
import com.examguard.exception.ConflictException
import com.examguard.exception.NotFoundException
import com.examguard.model.Hall
import com.examguard.model.HallStatus
import com.examguard.repository.HallRepository
import com.examguard.repository.SeatAllocationRepository
import org.springframework.stereotype.Service

@Service
class HallService(
    private val halls: HallRepository,
    private val seats: SeatAllocationRepository
) {
    fun list(): List<Hall> = halls.findAll().sortedBy { it.name }

    private fun parseStatus(value: String?): HallStatus =
        try {
            if (value.isNullOrBlank()) HallStatus.AVAILABLE else HallStatus.valueOf(value.trim().uppercase())
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Status must be AVAILABLE or MAINTENANCE")
        }

    fun create(req: HallRequest): Hall {
        val name = req.name.trim()
        if (halls.findByName(name) != null) throw ConflictException("A hall named $name already exists")
        return halls.save(
            Hall(
                name = name,
                building = req.building.trim(),
                rowCount = req.rowCount,
                colCount = req.colCount,
                status = parseStatus(req.status)
            )
        )
    }

    fun update(id: Long, req: HallRequest): Hall {
        val hall = halls.findById(id).orElseThrow { NotFoundException("Hall not found") }
        val name = req.name.trim()
        val same = halls.findByName(name)
        if (same != null && same.id != hall.id) throw ConflictException("A hall named $name already exists")

        val status = parseStatus(req.status)
        val layoutChanged = hall.rowCount != req.rowCount || hall.colCount != req.colCount
        if ((layoutChanged || status != HallStatus.AVAILABLE) && seats.existsByHallId(id)) {
            throw ConflictException("Seats are already allocated in this hall. Reset those exam allocations first.")
        }
        hall.name = name
        hall.building = req.building.trim()
        hall.rowCount = req.rowCount
        hall.colCount = req.colCount
        hall.status = status
        return halls.save(hall)
    }

    fun delete(id: Long) {
        if (!halls.existsById(id)) throw NotFoundException("Hall not found")
        if (seats.existsByHallId(id)) throw ConflictException("This hall is used by an exam. Reset that allocation first.")
        halls.deleteById(id)
    }
}
