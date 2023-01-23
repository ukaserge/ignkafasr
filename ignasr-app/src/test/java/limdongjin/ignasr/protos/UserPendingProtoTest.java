package limdongjin.ignasr.protos;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class UserPendingProtoTest {
    @Test
    void test1() throws InvalidProtocolBufferException {
        UserPendingProto.UserPending userPending = UserPendingProto.UserPending
                .newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setUserId(UUID.randomUUID().toString())
                .build();

        System.out.println(userPending);
        var encoded = userPending.toByteArray();
        UserPendingProto.UserPending decoded = UserPendingProto.UserPending.parseFrom(encoded);
        System.out.println(decoded);
    }
}