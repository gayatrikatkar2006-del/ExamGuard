package com.examguard.service

import com.examguard.dto.AuthResponse
import com.examguard.dto.LoginRequest
import com.examguard.dto.RegisterRequest
import com.examguard.dto.UserDto
import com.examguard.exception.BadRequestException
import com.examguard.exception.ConflictException
import com.examguard.exception.NotFoundException
import com.examguard.exception.UnauthorizedException
import com.examguard.model.Role
import com.examguard.model.User
import com.examguard.repository.UserRepository
import com.examguard.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

fun User.toDto() = UserDto(id, name, email, role.name, department, rollNo)

@Service
class AuthService(
    private val users: UserRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtService
) {
    fun register(req: RegisterRequest): AuthResponse {
        val email = req.email.trim().lowercase()
        if (users.existsByEmail(email)) throw ConflictException("This email is already registered")

        val role = if (req.role.equals("FACULTY", ignoreCase = true)) Role.FACULTY else Role.STUDENT
        var roll: String? = null
        if (role == Role.STUDENT) {
            roll = req.rollNo?.trim()?.uppercase()
            if (roll.isNullOrBlank()) throw BadRequestException("Roll number is required for students")
            if (users.existsByRollNo(roll)) throw ConflictException("Roll number $roll is already registered")
        }

        val saved = users.save(
            User(
                name = req.name.trim(),
                email = email,
                password = encoder.encode(req.password),
                role = role,
                department = req.department.trim().uppercase(),
                rollNo = roll
            )
        )
        return AuthResponse(jwt.generate(saved), saved.toDto())
    }

    fun login(req: LoginRequest): AuthResponse {
        val user = users.findByEmail(req.email.trim().lowercase())
            ?: throw UnauthorizedException("Invalid email or password")
        if (!encoder.matches(req.password, user.password)) throw UnauthorizedException("Invalid email or password")
        return AuthResponse(jwt.generate(user), user.toDto())
    }

    fun me(id: Long): UserDto =
        users.findById(id).orElseThrow { NotFoundException("User not found") }.toDto()
}
