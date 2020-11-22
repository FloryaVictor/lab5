package lab5.Messages;

public class TestMsg {
    private final String url;
    private final Integer time;
    public TestMsg(String url, Integer time){
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
