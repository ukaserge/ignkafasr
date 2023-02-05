package limdongjin.authserver.entity

import org.springframework.data.relational.core.mapping.Table

@Table(name = "person")
data class Person(
    val name: String,
    val password: String,
    val role: String
)