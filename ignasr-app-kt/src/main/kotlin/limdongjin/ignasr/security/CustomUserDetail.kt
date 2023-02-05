package limdongjin.ignasr.security

import org.springframework.security.core.GrantedAuthority

import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(
    private val username: String,
    private val password: String,
    private val roles: Collection<GrantedAuthority?>,
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority?> {
        return roles
    }

    override fun getPassword(): String {
        return password
    }

    override fun getUsername(): String {
        return username
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }
}