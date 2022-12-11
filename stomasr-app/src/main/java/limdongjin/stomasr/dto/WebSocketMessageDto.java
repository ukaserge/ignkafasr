package limdongjin.stomasr.dto;

public class WebSocketMessageDto {
    private String from; // 보내는 유저 UUID
    private String type; // 메시지 타입
    private String data; // roomId
    private Object candidate; // 상태

    @Override
    public String toString() {
        return "WebSocketMessageDto{" +
                "from='" + from + '\'' +
                ", type='" + type + '\'' +
                ", data='" + data + '\'' +
                ", candidate=" + candidate +
                ", sdp=" + sdp +
                '}';
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Object getCandidate() {
        return candidate;
    }

    public void setCandidate(Object candidate) {
        this.candidate = candidate;
    }

    public Object getSdp() {
        return sdp;
    }

    public void setSdp(Object sdp) {
        this.sdp = sdp;
    }

    private Object sdp; // sdp 정보

    public WebSocketMessageDto() {
    }

    public WebSocketMessageDto(String from, String type, String data, Object candidate, Object sdp) {
        this.from = from;
        this.type = type;
        this.data = data;
        this.candidate = candidate;
        this.sdp = sdp;
    }
}
