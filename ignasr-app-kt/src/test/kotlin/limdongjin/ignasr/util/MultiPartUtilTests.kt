package limdongjin.ignasr.util

import limdongjin.ignasr.MyTestUtil.prepareServerRequest
import limdongjin.ignasr.util.MultiPartUtil.toFunctionThatFieldNameToBytesMono
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException


class MultiPartUtilTests {
    @Test
    @Throws(IOException::class, UnsupportedAudioFileException::class)
    fun canLoadWavBlobFromServerRequest() {
        // Given
        val reqId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val classPathResource = ClassPathResource("data/foo.wav")
        val file = classPathResource.file
        val expectedAudioInputStream = AudioSystem.getAudioInputStream(file)
        val label = "ldj"

        // Prepare ServerRequest
        val serverRequest = prepareServerRequest(classPathResource, reqId, label, userId)

        // Extract file, name from ServerRequest, Load Audio
        val fieldNameToBytesMono = toFunctionThatFieldNameToBytesMono(
            serverRequest!!
        )
        val fileMono = fieldNameToBytesMono.apply("file")
        val nameMono = fieldNameToBytesMono.apply("name").map { bytes: ByteArray? -> String(bytes!!) }
        val actualAudioInputStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(fileMono.block()))

        // Verify
        Assertions.assertEquals(reqId, nameMono.block())
        Assertions.assertEquals(expectedAudioInputStream.format.toString(), actualAudioInputStream.format.toString())
        Assertions.assertEquals(expectedAudioInputStream.frameLength, actualAudioInputStream.frameLength)
        println(actualAudioInputStream.format)
        // example:
        // PCM_SIGNED 16000.0 Hz, 16 bit, mono, 2 bytes/frame, little-endian
        println(nameMono.block())
    }
}