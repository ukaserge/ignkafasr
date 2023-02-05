package limdongjin.authserver.dto

import org.springframework.data.relational.core.mapping.Table

@Table("person_refresh")
data class RefreshRequest(
    val name: String,
    val accessToken: String,
    val refreshToken: String
)