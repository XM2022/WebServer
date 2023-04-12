package priv.xm.xmserver.app;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import priv.xm.xmserver.implement.ServerCore;

public class MyServer {

    public static void main(String[] args) throws IOException {
        final int PORT = 9000;
        final String IP = ServerCore.getRealLocalAddress();
        @SuppressWarnings("resource")
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server startup successfully.");
        System.out.printf("The server ip: %s  port: %d%n", IP, PORT);
        System.out.println("等待连接...");
        
        //手动创建线程
        /*while(true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(()->{
                ServerCore server = new ServerCore(clientSocket);
                while(true) {
                    try {
                        server.acceptMessage();
                        server.responseMessage();
                    } catch(SocketTimeoutException e) {
                        server.closeSocket();
                        return ;
                    }
                    catch (ClassNotFoundException | IOException e) {
                        RuntimeException packedE = new RuntimeException();
                        packedE.initCause(e);
                        throw packedE;
                    }
                }
            }).start();
        
        }*/
        
        //使用线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(50);
        while(true) {
            Socket clientSocket = serverSocket.accept();
            threadPool.submit(()->{
                ServerCore server = new ServerCore(clientSocket);
                while(true) {
                    try {
                        server.acceptMessage();
                        server.responseMessage();
                    } catch(SocketTimeoutException e) {
                        server.closeSocket();
                        return ;
                    }
                    catch (ClassNotFoundException | IOException e) {
                        RuntimeException packedE = new RuntimeException();
                        packedE.initCause(e);
                        throw packedE;
                    }
                }
            });
        }
    }
}
