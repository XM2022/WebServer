package priv.xm.xmserver.app;

import java.io.IOException;

import priv.xm.xmserver.implement.ClientCore;

public class MyClient {
    
    public static void main(String[] args) throws ClassNotFoundException, InterruptedException, IOException  {
        ClientCore client = new ClientCore("127.0.0.1", 9000);
        client.setCompressTransfer(true);
        client.setTransferCompressRatio(9);
        client.showUI();
    }
}
