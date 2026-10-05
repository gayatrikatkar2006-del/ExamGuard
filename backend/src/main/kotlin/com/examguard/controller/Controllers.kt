package com.examguard.controller

import com.examguard.dto.AdminDashboard
import com.examguard.dto.AllocationResult
import com.examguard.dto.AuthResponse
import com.examguard.dto.DutyView
import com.examguard.dto.ExamRequest
import com.examguard.dto.ExamSeatingView
import com.examguard.dto.ExamView
import com.examguard.dto.HallRequest
import com.examguard.dto.LoginRequest
import com.examguard.dto.MySeatView
import com.examguard.dto.RegisterRequest
import com.examguard.dto.UserDto
import com.examguard.model.Hall
import com.examguard.model.Role
import com.examguard.repository.UserRepository
import com.examguard.security.AuthUser
import com.examguard.service.AllocationService
import com.examguard.service.AuthService
import com.examguard.service.DashboardService
import com.examguard.service.ExamService
import com.examguard.service.HallService
import com.examguard.service.toDto
import com.examguard.exception.BadRequestException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HealthController {
    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "UP", "app" to "ExamGuard")
}

@RestController
@RequestMapping("/api/auth")
class AuthController(private val auth: AuthService) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody req: RegisterRequest): AuthResponse = auth.register(req)

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): AuthResponse = auth.login(req)

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal user: AuthUser): UserDto = auth.me(user.id)
}

@RestController
@RequestMapping("/api/halls")
class HallController(private val halls: HallService) {
    @GetMapping
    fun list(): List<Hall> = halls.list()

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: HallRequest): Hall = halls.create(req)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: HallRequest): Hall = halls.update(id, req)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = halls.delete(id)
}

@RestController
@RequestMapping("/api/exams")
class ExamController(
    private val exams: ExamService,
    private val allocation: AllocationService
) {
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    fun list(): List<ExamView> = exams.list()

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    fun get(@PathVariable id: Long): ExamView = exams.get(id)

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: ExamRequest): ExamView = exams.create(req)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = exams.delete(id)

    @PostMapping("/{id}/allocate-seats")
    @PreAuthorize("hasRole('ADMIN')")
    fun allocateSeats(@PathVariable id: Long): AllocationResult = allocation.allocateSeats(id)

    @PostMapping("/{id}/allocate-invigilators")
    @PreAuthorize("hasRole('ADMIN')")
    fun allocateInvigilators(@PathVariable id: Long): AllocationResult = allocation.allocateInvigilators(id)

    @DeleteMapping("/{id}/allocation")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun reset(@PathVariable id: Long) = allocation.reset(id)

    @GetMapping("/{id}/seating")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    fun seating(@PathVariable id: Long): ExamSeatingView = allocation.seating(id)
}

@RestController
@RequestMapping("/api/me")
class MeController(private val allocation: AllocationService) {
    @GetMapping("/seats")
    @PreAuthorize("hasRole('STUDENT')")
    fun seats(@AuthenticationPrincipal user: AuthUser): List<MySeatView> = allocation.mySeats(user.id)

    @GetMapping("/duties")
    @PreAuthorize("hasRole('FACULTY')")
    fun duties(@AuthenticationPrincipal user: AuthUser): List<DutyView> = allocation.myDuties(user.id)
}

@RestController
@RequestMapping("/api/users")
class UserController(private val users: UserRepository) {
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun list(@RequestParam(required = false) role: String?): List<UserDto> {
        val wanted: Role? = role?.let {
            try {
                Role.valueOf(it.trim().uppercase())
            } catch (e: IllegalArgumentException) {
                throw BadRequestException("Role must be ADMIN, FACULTY or STUDENT")
            }
        }
        return users.findAll()
            .filter { wanted == null || it.role == wanted }
            .sortedBy { it.name }
            .map { it.toDto() }
    }
}

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(private val dashboard: DashboardService) {
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    fun admin(): AdminDashboard = dashboard.admin()
}
