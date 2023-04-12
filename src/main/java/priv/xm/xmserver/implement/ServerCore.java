package priv.xm.xmserver.implement;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import priv.xm.xmserver.interfaces.MyTransferProtocol;
import priv.xm.xmserver.interfaces.ResponseType;

/**
 * 服务器核心功能抽象类.
 * @author HW
 *
 */
public class ServerCore extends MyTransferProtocol {
    private Socket socket;
    private static String  repository = "D:\\MyServer";  //服务器仓库所在目录
    @SuppressWarnings("unused")
    private Charset charset = StandardCharsets.UTF_8;
    
    private static int connectQuantity = 0;
    
    /**Linux、Window下均可正确获得本机IP, 不管是否开启虚拟机网卡*/
    public static String getRealLocalAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces
                        .nextElement();
 
                // 去除回环接口，子接口，未运行和接口
                if (netInterface.isLoopback() || netInterface.isVirtual()
                        || !netInterface.isUp()) {
                    continue;
                }
                
                if (!netInterface.getDisplayName().contains("Intel")
                        && !netInterface.getDisplayName().contains("Realtek")) {
                    continue;
                }
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    if (ip != null) {
                        // ipv4
                        if (ip instanceof Inet4Address) {
                            return ip.getHostAddress();
                        }
                    }
                }
                break;
            }
        } catch (SocketException e) {
            System.err.println("Error when getting host ip address"
                    + e.getMessage());
        }
        return null;
    }
    
    private static void changeConnectQuantity(int variation) {
        connectQuantity += variation;
        if(variation > 0) System.out.printf("新增1个连接, 当前连接数:%d%n", connectQuantity);
        else if(variation < 0) System.out.printf("断开1个连接, 当前连接数:%d%n", connectQuantity);
    }
    
    public ServerCore(Socket clientSocket){
        try {
            socket = clientSocket;
            socketIn = new BufferedInputStream(clientSocket.getInputStream());
            socketOut = new BufferedOutputStream(clientSocket.getOutputStream());
            sizeIn = new DataInputStream(socketIn);
            sizeOut = new DataOutputStream(socketOut);
            changeConnectQuantity(+1);
            
            //判断连接者
            char[] type = new char[3];
            for(int i=0; i<type.length; ++i) {  //读取前3个字节
                type[i] = (char)socketIn.read();
            }
            if("XMT".equals(new String(type))) {
                isClient = true;
                System.out.println("客户端连接.");
            }
            else{
                isClient = false;
                browserMessage.append(type); 
                socket.setSoTimeout(7000);
                System.out.println("浏览器连接.");
            }
            
        } 
        catch (IllegalArgumentException e) {
            System.err.println("服务器初始化失败, 无效端口号!");
        }
        catch (IOException e) {
            System.err.println("服务器启动失败!");
            e.printStackTrace();
        }
    }
    
    /**浏览器或客户端*/
    private boolean isClient;
    
    public void acceptMessage() throws ClassNotFoundException, IOException {
        if(isClient) super.acceptMessage();
        else acceptBrowserRequest();
    }
    
    public void responseMessage() throws IOException, NumberFormatException, ClassNotFoundException {
        if(isClient) responseMessage((String)message.get("type"));
        else responseBrowserRequest();
    }
    /**
     * 调度函数
     * @throws IOException 
     * @throws ClassNotFoundException 
     * @throws NumberFormatException */
    public void responseMessage(String messageType) throws IOException, NumberFormatException, ClassNotFoundException {

        switch (messageType) { 
        //自定义协议消息头
        case "UP":
            responseUpload();
            break;
        case "DW":
            responseDownload();
            break;
        case "BW":
            responseFileList();
            break;
        case "CRT":
            responseCreateDirectory();
            break;
        case "DEL":
            responseDeleteFile();
            break;
        case "SRH":
            responseSearchFile();
            break;
            
        default:
            System.err.println("该请求无法处理!");;
        }
        message.clear();
    }
    
    private void responseUpload() throws FileNotFoundException, IOException {
        String filePath = (String)message.get("url");  //相对路径+文件名
        //将文件名更改为临时文件名.
        Path upFile = Paths.get(repository, filePath);
        Path dir = upFile.getParent();
        String fileName = upFile.getFileName().toString();
        int separatorPos = fileName.lastIndexOf('.');
        if(separatorPos==-1) separatorPos = fileName.length();  //文件名无"."后缀的情况
        Path tmpFile = Paths.get(dir.toString(), fileName.substring(0, separatorPos) + "-Uploading"); 
        long filePointer;
        if(Files.exists(tmpFile)) {  //断点续传
            filePointer = tmpFile.toFile().length();
        }
        else {
            filePointer = 0;
            Files.createFile(tmpFile);
        }
        message.clear();
        message.put("filePointer", filePointer);
        sendMessage(ResponseType.YES, message);
        acceptFile(tmpFile, filePointer);
        
        if(Files.exists(upFile)) Files.deleteIfExists(upFile); //删除并更新同名文件
        tmpFile.toFile().renameTo(upFile.toFile());  //将临时文件重命名为正式文件
    }
    
    public void closeSocket() {
        if(socket.isConnected() && !socket.isClosed())
            try {
                socket.close();
                changeConnectQuantity(-1);
            } catch (IOException e) {
                System.out.println("socket关闭失败!");
                e.printStackTrace();
            }
    }
    
    
    /**
     * 
     * @param fileName
     * @param filePointer  断点续传位置.
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    private void responseDownload() throws IOException, ClassNotFoundException {
        String fileName = (String)message.get("url");
        long filePointer = Long.valueOf((String)message.get("filePointer"));
        
        message.clear();
        Path downloadedFile = Paths.get(repository, fileName);
        sendMessage(ResponseType.YES, message);
        sendFile(downloadedFile, filePointer);
    }
    
    private Map<String, Object> browserRequest = new TreeMap<String, Object>();
    
    private StringBuffer browserMessage = new StringBuffer();
    /**
     * @return 封装后的请求报文
     * @throws IOException
     */
    public void acceptBrowserRequest() throws IOException {
        int c = 0, prec = 0;
        while(true){
            prec = c;
            c = socketIn.read();
            if(prec == '\n' && c == '\r') {
                break;
            }
            browserMessage.append((char)c);
        }
        socketIn.read();  //清除末尾'\n'
        
        //提取整理浏览器报文信息进Map中
        browserRequest = new TreeMap<String, Object>();
        String[] lines = browserMessage.toString().split("\r\n");
        String[] head = lines[0].split(" ");  //首部行
        browserRequest.put("type", head[0]);
        browserRequest.put("url", head[1]);
        browserRequest.put("version", head[2]);
        for (int i = 1; i < lines.length; i++) {
            String[] kv = lines[i].split(": ");
            browserRequest.put(kv[0], kv[1]);
        }
        browserMessage.delete(0, browserMessage.length());  //清空
        
        System.out.println("浏览器请求:");
        System.out.println(browserRequest);
    }

    /**
     * 简单的响应网页功能.
     * @throws IOException 
     */
    public void responseBrowserRequest() throws IOException {
        File file = Paths.get(repository, (String)browserRequest.get("url")).toFile();
        if(file.exists()) {
            String fileName = file.getName();
            socketOut.write("HTTP/1.1 200 OK\r\n".getBytes());
            socketOut.write(String.format("content-length: %d\r\n", file.length()).getBytes());
            
            if("html".equals(fileName.substring(fileName.lastIndexOf('.')+1, fileName.length()))) {
                socketOut.write("content-type: text/html\r\n".getBytes());
                socketOut.write("\r\n".getBytes());
                FileInputStream fileIn = new FileInputStream(file);
                socketOut.write(fileIn.readAllBytes());
                fileIn.close();
            }
            else {
                long start = System.currentTimeMillis();
                socketOut.write(("Content-Disposition: attachment;filename=" + fileName + "\r\n").getBytes());
                socketOut.write("Content-Type: application/octet-stream\r\n".getBytes());
                socketOut.write("\r\n".getBytes());
                InputStream fileIn = Files.newInputStream(file.toPath());
                fileIn.transferTo(socketOut);
                /*int dataBlockSize = getDataBlockSize();
                BufferedInputStream fileBfIn = new BufferedInputStream(fileIn, dataBlockSize);
                byte[] data = new byte[dataBlockSize];
                while(fileBfIn.read(data) != -1) {
                    data = compress(data);
                    socketOut.write(data);
                    socketOut.flush();
                }*/
                
                fileIn.close();
                long end = System.currentTimeMillis();
                System.out.printf("传输共耗时%.1fS%n", (end-start)/1000.0);
            }
        }
        else {  //显示错误网页
            file = Paths.get(repository, "error.html").toFile();
            socketOut.write("HTTP/1.1 404 Not Found\r\n".getBytes());
            socketOut.write("Content-Type: text/html\r\n".getBytes());
            socketOut.write(String.format("Content-Length: %d\r\n", file.length()).getBytes());
            socketOut.write("\r\n".getBytes());
            InputStream fileIn = Files.newInputStream(file.toPath());
            fileIn.transferTo(socketOut);
            fileIn.close();
        }
        
        socketOut.flush(); 
    }
    
    /**
     * 目前实现: 仅从服务器仓库根目录全局查找.
     * @param fileName 绝对路径:服务器仓库全局查找   相对路径:仅当前目录查找
     * @return 文件绝对路径. 如果文件未找到或路径有误, 返回null.
     * @throws IOException 
     */
    private void responseSearchFile() throws IOException {
        Object fileName = message.get("url");
        message.clear();
        Files.walkFileTree(Paths.get(repository), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(fileName.equals(file.getFileName().toString())) {
                    Path dir = file.getParent();
                    List<String> fileList = new ArrayList<String>();
                    for (Path item : Files.newDirectoryStream(dir)) {
                        fileList.add(item.getFileName().toString());
                    }
                    String relativizeDir = Paths.get(repository).relativize(dir).toString();  //目录相对化
                    if(relativizeDir.length()==0) relativizeDir="/";
                    else relativizeDir= "/" + relativizeDir + "/";
                    message.put("url", relativizeDir);  //目录所在路径
                    message.put("files", fileList);  //该目录下的文件列表
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }

        });
        
        try {
        if(message.size() != 0) { //查找成功
            sendMessage(ResponseType.YES, message);
        }
        else {
            sendSimpleResponse(false);
        }
        }catch(IOException e) {
            e.printStackTrace();
            throw e;
        }
        
    }
    
    /**
     * @param browseDirectory 以服务器仓库根目录为基准的相对路径.
     * @throws IOException 
     */
    public void responseFileList() throws IOException {
        String browseDirectory = (String)message.get("browseDirectory");
        List<String> fileList = new ArrayList<String>();
        for (Path path : Files.newDirectoryStream(Paths.get(repository, browseDirectory))) {
            //目录末尾添加'/'
            if(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                fileList.add(path.getFileName().toString() + "/");
            }
            else {
                fileList.add(path.getFileName().toString());
            }
        }
        
        message.clear();
        message.put("files", fileList);
        sendMessage(ResponseType.YES, message);
    }
    
    /**发送仅包含YES/NO的简单响应消息.*/
    private void sendSimpleResponse(boolean response) throws IOException {
        message.clear();
        if(response) sendMessage(ResponseType.YES, message);
        else sendMessage(ResponseType.NO, message);
    }
    
    /**
     * 删除文件或(多级)目录.
     * @param file 绝对路径
     * @throws IOException 
     */
    public void responseDeleteFile() throws IOException {
        Path file = Paths.get(repository, (String)message.get("url"));
        Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);  //删除文件
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.SKIP_SUBTREE;  //跳过此子目录
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);  //删除目录
                return FileVisitResult.CONTINUE;
            }
            
        });
                
        sendSimpleResponse(true);
    }
    
    public void responseCreateDirectory() throws IOException {
        Path newDir = Paths.get(repository, (String)message.get("url"));
        try{
            Files.createDirectory(newDir);
            sendSimpleResponse(true);
        }
        catch(FileAlreadyExistsException e) {
            sendSimpleResponse(false);
        }
    }
 
}
