package priv.xm.xmserver.interfaces;

public enum RequestType {
    UPLOAD_FILE("UP"),
    DOWNLOAD_FILE("DW"),
    BROWSE_FILE("BW"),
    MOVE_FILE("MV"),
    CREATE_DIRECTORY("CRT"),
    DELETE_FILE("DEL"),
    SEARCH_FILE("SRH");
    
    private String requestHeader;
    private RequestType(String header) {
        this.requestHeader = header;
    }
    
    @Override
    public String toString() {
        return this.requestHeader;
    }
}
