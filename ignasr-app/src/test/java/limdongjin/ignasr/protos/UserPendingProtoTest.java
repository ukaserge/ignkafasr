package limdongjin.ignasr.protos;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserPendingProtoTest {
    @Test
    void test1() throws InvalidProtocolBufferException {
        UserPendingProto.UserPending userPending = UserPendingProto.UserPending
                .newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .build();

        System.out.println(userPending);
        var encoded = userPending.toByteArray();
        UserPendingProto.UserPending decoded = UserPendingProto.UserPending.parseFrom(encoded);
        System.out.println(decoded);
    }
}