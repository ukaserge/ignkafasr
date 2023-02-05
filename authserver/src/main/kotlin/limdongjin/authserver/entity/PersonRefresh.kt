package limdongjin.authserver.entity

import org.springframework.data.relational.core.mapping.Table

@Table("person_refresh")
data class PersonRefresh(
    val id: String,
    val accessToken: String,
    val refreshToken: String
)