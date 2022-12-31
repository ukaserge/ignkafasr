package limdongjin.ignasr.event;

import java.util.UUID;

public class IgnasrEvent {
    public String userName;
    public UUID reqUuid;
    public String state;
    public String msg;

    public IgnasrEvent() {}

    public IgnasrEvent(String userName, UUID reqUuid, String state, String msg) {
        this.userName = userName;
        this.reqUuid = reqUuid;
        this.state = state;
        this.msg = msg;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public UUID getReqUuid() {
        return reqUuid;
    }

    public void setReqUuid(UUID reqUuid) {
        this.reqUuid = reqUuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
