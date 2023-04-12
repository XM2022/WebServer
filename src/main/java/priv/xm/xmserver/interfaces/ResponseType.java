package priv.xm.xmserver.interfaces;

public enum ResponseType {
    YES("OK"),
    NO("NO");
    
    private String responseHeader;
    private ResponseType(String header) {
        this.responseHeader = header;
    }
    
    @Override
    public String toString() {
        return this.responseHeader;
    }
}
