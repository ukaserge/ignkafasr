package limdongjin.authserver.dto

data class PersonLoginResponse (
    val name: String,
    val msg: String,
    val accessToken: String,
    val refreshToken: String
)

