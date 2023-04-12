package priv.xm.xmserver.implement;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.Test;

public class ServerCoreTest {
    private ServerCore server;
    
    {
        try {
            server = new ServerCore();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Test
    public void testServer() throws IOException, ClassNotFoundException {
        
        server.setTransferCompressRatio(5);
        while(true) {
            server.acceptMessage();
            server.responseMessage();
        }
        
    }
    
    /*@Test 
    public void testResponseBrowser() throws IOException {
        while(true) {
            server.getBrowserRequest();
            server.responseBrowserRequest();
        }
        
    }*/
    

}
