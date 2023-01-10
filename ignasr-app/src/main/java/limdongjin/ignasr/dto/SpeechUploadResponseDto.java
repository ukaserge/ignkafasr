package limdongjin.ignasr.dto;

public class SpeechUploadResponseDto {
    public String userName;
    public String message;

    public String label;

    public SpeechUploadResponseDto(String userName, String message, String label) {
        this.userName = userName;
        this.message = message;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
