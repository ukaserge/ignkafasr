package limdongjin.stomasr.dto;

public class JoinDto {
    String userId;

    public JoinDto(String userId, String reqId, String msg) {
        this.userId = userId;
        this.reqId = reqId;
        this.msg = msg;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    String reqId;
    String msg;

    public JoinDto() {
    }

    @Override
    public String toString() {
        return "JoinDto{" +
                "userId='" + userId + '\'' +
                ", reqId='" + reqId + '\'' +
                ", msg='" + msg + '\'' +
                '}';
    }

    public JoinDto(String userId, String msg) {
        this.userId = userId;
        this.msg = msg;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
