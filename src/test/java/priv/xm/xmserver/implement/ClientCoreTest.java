package priv.xm.xmserver.implement;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class ClientCoreTest {
    private ClientCore client;
    
    {
        try {
            client = new ClientCore("127.0.0.1", 9000);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Test
    public void testClient() throws ClassNotFoundException, IOException {
        //client.downloadFile("difficult.txt");
        //client.downloadFile("星空.jpg");
        //client.downloadFile("goods.sql");
        //client.downloadFile("中小学武术健身操-旭日东升.mp4");
        while(true)
            ;
    }
    
    @Test
    public void testTransferTime() throws ClassNotFoundException, IOException {
        long start;
        long end;
        client.setCompressTransfer(true);
        client.setTransferCompressRatio(1);
        start = System.currentTimeMillis();
        client.downloadFile("MyClient-store.rar", "MyClient-level-1.rar");
        end = System.currentTimeMillis();
        System.out.printf("1文件传输完成, 共耗时%dS%n", (end-start)/1000);
        
        client.setTransferCompressRatio(4);
        start = System.currentTimeMillis();
        client.downloadFile("MyClient-store.rar", "MyClient-level-4.rar");
        end = System.currentTimeMillis();
        System.out.printf("4文件传输完成, 共耗时%dS%n", (end-start)/1000);
        
        client.setTransferCompressRatio(9);
        start = System.currentTimeMillis();
        client.downloadFile("MyClient-store.rar", "MyClient-level-9.rar");
        end = System.currentTimeMillis();
        System.out.printf("9文件传输完成, 共耗时%dS%n", (end-start)/1000);

        while(true)
            ;
        
    }

    
    

    @Test
    public void testSetTransferCompressRatio() {
        fail("Not yet implemented");
    }

    @Test
    public void testClientCore() {
        fail("Not yet implemented");
    }

    @Test
    public void testUploadFile() {
        fail("Not yet implemented");
    }

    @Test
    public void testDownloadFileString() {
        fail("Not yet implemented");
    }

    @Test
    public void testDownloadFileStringString() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpdateFileList() {
        fail("Not yet implemented");
    }

    @Test
    public void testRunUI() throws ClassNotFoundException, IOException, InterruptedException {
        client.showUI();
    }

    @Test
    public void testCreateDirectory() throws ClassNotFoundException, IOException {
        client.createDirectory();
    }

    @Test
    public void testDeleteFile() {
        fail("Not yet implemented");
    }

}
