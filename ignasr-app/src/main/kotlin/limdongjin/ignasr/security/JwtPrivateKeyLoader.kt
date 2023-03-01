package limdongjin.ignasr.security

import org.apache.commons.codec.binary.Base64
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec

object JwtPrivateKeyLoader {
    fun loadPrivateKey(rawKeyContent: String): RSAPrivateKey {
        val key = rawKeyContent
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace(System.lineSeparator(), "")
            .replace("\\n", "")
            .replace("-----END PRIVATE KEY-----", "")
        val encoded = Base64.decodeBase64(key)
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(encoded)
        println(key)
        println(System.lineSeparator())
        return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
    }
//    private fun readPublicKey(): RSAPublicKey {
//        val resource = ClassPathResource("public.pem")
//        val key = resource.inputStream
//            .readAllBytes()
//            .let { String(it, Charset.defaultCharset()) }
//            .replace("-----BEGIN PUBLIC KEY-----", "")
//            .replace(System.lineSeparator(), "")
//            .replace("-----END PUBLIC KEY-----", "")
//        val encoded = Base64.decodeBase64(key)
//        val keyFactory = KeyFactory.getInstance("RSA")
//        val keySpec = X509EncodedKeySpec(encoded)
//
//        return keyFactory.generatePublic(keySpec) as RSAPublicKey
//    }
}