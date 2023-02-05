package limdongjin.authserver.dto

import org.springframework.data.relational.core.mapping.Table

@Table(name = "person")
data class PersonLogin(
    val name: String,
    val password: String
)