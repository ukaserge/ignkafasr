package limdongjin.authserver.dto

import org.springframework.data.relational.core.mapping.Table

@Table(name = "person")
data class PersonRegister(
    val name: String,
    val password: String,
    val role: String
)