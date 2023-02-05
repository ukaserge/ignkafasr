package limdongjin.authserver.dto

class PersonRefreshResponse (
    val name: String,
    val accessToken: String,
    val refreshToken: String,
    val msg: String
)