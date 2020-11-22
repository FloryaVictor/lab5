package lab5.Messages;

public class GetMsg {
    private final String url;
    public GetMsg(String url){
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

}
