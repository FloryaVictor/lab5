package lab5.Messages;

public class GetMsg {
    private final String url;
    private final Integer time;
    public GetMsg(String url, Integer time){
        this.url = url;
        this.time = time;
    }

    public String getUrl() {
        return url;
    }

    public Integer getTime() {
        return time;
    }
}
