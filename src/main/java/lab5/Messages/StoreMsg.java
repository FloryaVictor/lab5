package lab5.Messages;

public class StoreMsg {
    private final String url;
    private final Long time;
    public StoreMsg(String url, Long time){
        this.url = url;
        this.time = time;
    }

    public String getUrl() {
        return url;
    }

    public Long getTime() {
        return time;
    }
}
