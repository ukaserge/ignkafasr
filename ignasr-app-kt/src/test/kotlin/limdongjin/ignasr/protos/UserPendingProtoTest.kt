package limdongjin.ignasr.protos

import com.google.protobuf.InvalidProtocolBufferException
import limdongjin.ignasr.protos.UserPendingProto.UserPending
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class UserPendingProtoTest {
    @Test
    @Throws(InvalidProtocolBufferException::class)
    fun simpleTest() {
        val reqId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val userPending = UserPending
            .newBuilder()
            .setReqId(reqId)
            .setUserId(userId)
            .build()

        println(userPending)
        Assertions.assertEquals(reqId, userPending.reqId)
        Assertions.assertEquals(userId, userPending.userId)

        val encoded = userPending.toByteArray()
        val decoded = UserPending.parseFrom(encoded)

        println(decoded)
        Assertions.assertEquals(reqId, decoded.reqId)
        Assertions.assertEquals(userId, decoded.userId)
    }
}