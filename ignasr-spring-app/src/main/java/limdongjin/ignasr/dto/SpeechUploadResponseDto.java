package limdongjin.ignasr.dto;

public class SpeechUploadResponseDto {
    public String userName;
    public String message;

    public SpeechUploadResponseDto(String userName, String message) {
        this.userName = userName;
        this.message = message;
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
