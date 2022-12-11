package limdongjin.stomasr.dto;

public class UserMessage {
    private String targetUserName;
    private String message;

    public String getTargetUserName() {
        return targetUserName;
    }

    public void setTargetUserName(String targetUserName) {
        this.targetUserName = targetUserName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserMessage(){}
    public UserMessage(String targetUserName, String message) {
        this.targetUserName = targetUserName;
        this.message = message;
    }
}
