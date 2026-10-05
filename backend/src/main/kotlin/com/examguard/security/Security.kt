package com.examguard.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import com.examguard.model.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest
import java.util.Date
import javax.crypto.SecretKey

/** The logged-in user, available in controllers through @AuthenticationPrincipal. */
data class AuthUser(val id: Long, val email: String, val role: String)

@Service
class JwtService(
    @Value("\${app.jwt.secret}") secret: String,
    @Value("\${app.jwt.expiration-hours}") private val hours: Long
) {
    // Hash the secret so the HMAC key is always long enough for HS256.
    private val key: SecretKey =
        Keys.hmacShaKeyFor(MessageDigest.getInstance("SHA-256").digest(secret.toByteArray()))

    fun generate(user: User): String =
        Jwts.builder()
            .subject(user.email)
            .claim("uid", user.id)
            .claim("role", user.role.name)
            .claim("name", user.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + hours * 3_600_000L))
            .signWith(key)
            .compact()

    fun parse(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
}

@Component
class JwtFilter(private val jwt: JwtService) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            try {
                val claims = jwt.parse(header.substring(7))
                val role = claims["role"] as String
                val principal = AuthUser((claims["uid"] as Number).toLong(), claims.subject, role)
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(principal, null, listOf(SimpleGrantedAuthority("ROLE_$role")))
            } catch (e: Exception) {
                SecurityContextHolder.clearContext() // bad or expired token: stay anonymous
            }
        }
        filterChain.doFilter(request, response)
    }
}

@Configuration
@EnableMethodSecurity
class SecurityConfig(private val jwtFilter: JwtFilter) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                    .requestMatchers("/api/health", "/api/kotlin-demo").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, res, _ -> writeJson(res, 401, "Please log in to continue") }
                it.accessDeniedHandler { _, res, _ -> writeJson(res, 403, "You do not have permission for this action") }
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    private fun writeJson(res: HttpServletResponse, status: Int, message: String) {
        res.status = status
        res.contentType = "application/json"
        res.writer.write("{\"error\":\"$message\"}")
    }
}
