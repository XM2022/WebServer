package priv.xm.xmserver.interfaces;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 针对客户端自定义传输协议:1.0的实现类.
 * URL支持中文字符.
 */
public abstract class MyTransferProtocol {

    public final static String MESSAGE_SEPARATOR = "\r\n";
    public final static int MAX_MESSAGE_LENGTH = 1024;  
    public final static Charset CHARSET = StandardCharsets.UTF_8;
    public final static String VERSION = "XMTP1.0";
    
    protected InputStream socketIn;
    protected OutputStream socketOut;
    protected DataInput sizeIn;
    protected DataOutput sizeOut;


    public MyTransferProtocol() {    }
    
    private static boolean isCompress = true;  //是否压缩传输标志位
    private static int compressLevel = 9;  //0:无压缩  9:最高压缩
    
    /**
     * 设置是否文件压缩传输
     * @param isCompress
     */
    public void setCompressTransfer(boolean isCompress) {
        MyTransferProtocol.isCompress = isCompress;
    }
    
    /**
     * 设置传输压缩率.
     * @param level  0~9
     */
    public void setTransferCompressRatio(int level) {
        if(level<0 || 9<level)  throw new IllegalArgumentException("只支持0~9的压缩等级!");
        compressLevel = level;
    }
    
    /**
     * 将信息进行压缩
     * @param data 待压缩字节
     * @return  压缩后的字节
     * @throws IOException
     */
    public byte[] compress(byte[] data) throws IOException {
        if(data.length == 0) return data;
        
        if(infoVisible) System.out.println("压缩前大小:" + data.length);
        byteArrayOutputStream.reset();  //清空字节数组流
        ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream, Charset.defaultCharset());
        zipOut.setMethod(ZipOutputStream.DEFLATED);
        zipOut.setLevel(compressLevel); 
        ZipEntry e = new ZipEntry("data");
        zipOut.putNextEntry(e);
        zipOut.write(data);    //压缩写入
        zipOut.flush();        
        zipOut.closeEntry();  //完成压缩
        zipOut.close();
        
        //返回压缩后的报文
        data = byteArrayOutputStream.toByteArray();
        if(infoVisible) System.out.println("压缩后大小:" + data.length);
        return data;
    }
    
    /**
     * 将信息进行解压处理.
     * @return 解压缩后的字节数,不能超过2GB字节数
     * @throws IOException
     */
    private byte[] decompress(byte[] data) throws IOException {
        if(data.length == 0) return data;
        
        //System.out.println("解压前大小:" + data.length);
        ByteArrayInputStream partStream = new ByteArrayInputStream(data);  //将字节流封装为InputStream传入ZipInputStream.
        ZipInputStream zipIn = new ZipInputStream(partStream, Charset.defaultCharset());
        @SuppressWarnings("unused")
        ZipEntry entry;
        if((entry = zipIn.getNextEntry()) != null) {
            //可以利用entry获取一些文件信息
            //System.out.println("compress:" + entry.getCompressedSize());
            data = zipIn.readAllBytes();  //读取的是此压缩项(entry)而非整个压缩文件(ZipInputStream)的所有字节
            zipIn.closeEntry();
        }
        zipIn.close();
        
        //System.out.println("解压后大小:" + data.length);
        return data;  
    }
    
    /**对报文进行压缩处理,提升网络传输效率
     * 格式: 字节数+报文块
     * @throws IOException */
    protected final void sendMessage(RequestType requestType, Map<String, Object> message) throws IOException {
        if(message == null) throw new IllegalArgumentException();
        message.put("type", requestType.toString());
        sendMessage(message);
    }
    
    /**对报文进行压缩处理,提升网络传输效率
     * 格式: 字节数+报文块
     * @throws IOException */
    protected final void sendMessage(ResponseType responseType, Map<String, Object> message) throws IOException {
        if(message == null) throw new IllegalArgumentException();
        message.put("type", responseType.toString());
        sendMessage(message);
    }
    
    private ByteArrayInputStream byteArrayInputStream;
    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    
    private boolean infoVisible = true;
    /**
     * 设置是否打印报文. 默认打印
     * @param visible
     */
    public void printInfo(boolean visible) {
        this.infoVisible = visible;
    }
    /**
     * 对报文进行压缩发送.
     * 格式: 字节数+报文块
     * @param message
     * @throws IOException
     */
    private void sendMessage(Map<String, Object> message) throws IOException {
        message.put("version", VERSION);
        if(infoVisible) System.out.println("发送报文:" + message);
        byteArrayOutputStream.reset();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);  //封装为对象流
        objectOutputStream.writeObject(message);  
        byte[] objectBytes = byteArrayOutputStream.toByteArray();  //从底层字节数组流中获得对象字节流
        if(isCompress) objectBytes = compress(objectBytes);  //压缩序列化对象
        
        //网络传输
        sizeOut.writeInt(objectBytes.length);
        socketOut.write(objectBytes);  
        socketOut.flush();
    }
    
    /**保存报文信息*/
    protected Map<String, Object> message = new HashMap<String, Object>();
    private RandomAccessFile randomAccessFile;
    
    @SuppressWarnings("unchecked")
    public void acceptMessage() throws IOException, ClassNotFoundException{
        byte[] objectBytes = acceptData();
        if(byteArrayInputStream != null) byteArrayInputStream.close();  //关闭之前的字节输入流
        byteArrayInputStream = new ByteArrayInputStream(objectBytes);  //创建新的字节输入流
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        message = (Map<String, Object>)objectInputStream.readObject();  //还原序列化对象
        if(infoVisible) System.out.println("接收报文:" + message);
    }
    
    /**
     * 通过"大小+数据块"格式发送.
     * @param data 待发送的数据块
     * @throws IOException
     */
    private void sendData(byte[] data) throws IOException {
        if(isCompress) {  //进行压缩传输
            data = compress(data);
        }
        sizeOut.writeInt(data.length);
        socketOut.write(data);  //发送文件
        socketOut.flush();
    }
    
    /**
     * 按照接收"大小+数据块"格式对socket进行接收.
     * @return 接收到的数据块.
     * @throws IOException
     */
    private byte[] acceptData() throws IOException {
        int dataSize = sizeIn.readInt();
        byte[] dataBlock = new byte[dataSize];
        //socketIn.read(fileBytes); 错误! --TCP协议分包/文件过大时, 因网络传输时延无法读满数组长度
        //方法1: 逐字节读取
        /*for (int i = 0; i < dataBlock.length; i++) {
            dataBlock[i] = (byte)socketIn.read();
        }*/
        //方法2: 数组读取, 判断读入长度--效率较高
        for(int i=0; i<dataSize; ) {
            i += socketIn.read(dataBlock, i, dataSize-i);
        }
        
        if(isCompress) {
            dataBlock = decompress(dataBlock);
        }
        return dataBlock;
    }
    
    private int dataBlockSize = 1024*1024;   //默认1MB; 断点传输文件时,每次发送数据块的大小--用于性能调优

    public int getDataBlockSize() {
        return dataBlockSize;
    }

    /**
     * @param dataBlockSize 单位:KB
     */
    public void setDataBlockSize(int dataBlockSize) {
        this.dataBlockSize = dataBlockSize*1024;
    }
    
    /**
     * 通过Map<String, long>对象序列化持久保存文件传输进度.
     */
    private ObjectInputStream progressFileIn;
    private ObjectOutputStream progressFileOut;
    /**含文件名*/
    private Path progressFile;  
    
    private void setProgressFileIn(FileInputStream progressFileIn) throws IOException {
        if(this.progressFileIn != null) progressFileIn.close();
        this.progressFileIn = new ObjectInputStream(progressFileIn);
    }

    private void setProgressFileOut(FileOutputStream progressFileOut) throws IOException {
        if(this.progressFileOut != null) progressFileOut.close();
        this.progressFileOut = new ObjectOutputStream(progressFileOut);
    }

    private String progressFilePrefix = "progress";
    private String progressFileSuffix = ".tmp";
    
    /*public void setProgressFile(String newPath) throws IOException{
        progressFile = Paths.get(newPath);
        if(Files.notExists(progressFile)) {
            Files.createDirectories(progressFile.getParent());
            //Files.createTempFile(Paths.get(newPath), progressFilePrefix, progressFileSuffix);
            Files.createFile(Paths.get(newPath));
        }
        //注意顺序, 应写入空Object文件. ObjectInputStream直接设置普通空文件夹会引发EOFException.
        setProgressFileOut(new FileOutputStream(progressFile.toFile(), false));
        setProgressFileIn(new FileInputStream(progressFile.toFile()));
    }*/
    
    /**
     * 更新持久化文件传输进度.
     * @param fileName
     * @param filePointer
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private void updateProgress(String fileName, long filePointer) throws IOException, ClassNotFoundException {
        Map<String, Long> progress;
        if(progressFile.toFile().length() == 0) {
            progress = new TreeMap<String, Long>();
        }
        else {
            progress = (Map<String, Long>)progressFileIn.readObject();
        }
        progress.put(fileName, filePointer);
        progressFileOut.writeObject(progress);
        progressFileOut.flush();
    }
    
    /**
     * 获取指定文件的传输进度.
     * @param fileName 
     * @return 文件指针
     * @throws ClassNotFoundException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private long getProgress(String fileName) throws ClassNotFoundException, IOException {
        if(progressFile.toFile().length() == 0)  return 0;  //进度文件被清理过,处于刚新建状态.
        Map<String, Long> progress = (Map<String, Long>)progressFileIn.readObject();
        Long filePointer = progress.get(fileName);
        return filePointer==null ? 0 : filePointer;
    }
    
    /**
     * 支持断点续传
     * @param filePath 文件所在位置, 包含文件名.
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    /*@SuppressWarnings({ "unchecked"})
    protected void sendFile(Path filePath) throws IOException, ClassNotFoundException{
        RandomAccessFile randomAccessFile = new RandomAccessFile(filePath.toFile(), "r");
        String fileName = filePath.getFileName().toString();
        long filePointer = getProgress(fileName);
        randomAccessFile.seek(filePointer);  //设置文件指针
        byte[] dataBlock = new byte[this.dataBlockSize];
        while(randomAccessFile.read(dataBlock) != -1) {
            sendData(dataBlock);
        }
        randomAccessFile.close();
        sendData(new byte[0]);
    }*/
    
    /**
     * 支持断点续传
     * 发送0字节数组指明文件结尾.
     * @param file 文件所在位置, 包含文件名. 需要保证文件存在.
     * @param filePointer
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected void sendFile(Path file, long filePointer) throws IOException, ClassNotFoundException{
        RandomAccessFile randomAccessFile = new RandomAccessFile(file.toFile(), "r");
        randomAccessFile.seek(filePointer);  //设置文件指针
        byte[] dataBlock = new byte[this.dataBlockSize];
        while(randomAccessFile.read(dataBlock) != -1) {
            sendData(dataBlock);
        }
        randomAccessFile.close();
        sendData(new byte[0]);  //指明文件结尾
    }
    
    /**
     * 支持断点续传.
     * 接收到0字节数组指明文件结尾.
     * @param file 文件所在位置, 包含文件名.
     * @param filePointer 
     * @throws FileNotFoundException 
     * @throws IOException 
     */
    protected void acceptFile(Path file, long filePointer) throws FileNotFoundException, IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file.toFile(), "rw");
        randomAccessFile.seek(filePointer);
        byte[] dataBlock;
        int i = 30;
        while(true) {  
            dataBlock = acceptData();
            if(dataBlock.length == 0) break; //接受到0字节数组,表明文件接收完毕.
            randomAccessFile.write(dataBlock);
            if(--i == 0) {
                //int j = 5 / 0;  
            }
        };
        randomAccessFile.close();  
    }
    

    /**
     * 格式化请求体-弃用.
     * "属性名=属性值"格式,单独一行; 
     * 不含报文体(文件字节);
     * 空行表示报文结束.
     * @param type
     * @param content 包含请求属性和值的映射
     * @return
     public static Message formatRequest(RequestType type, Map<String, Object> content) {
        if(content == null) throw new IllegalArgumentException("参数为null!");
        
        String msg = "";
        msg += "type=" + type.getRequestHeader() + MESSAGE_SEPARATOR;  //添加请求类型
        msg += "version=" + XMTP.VERSION + MESSAGE_SEPARATOR;    //添加协议版本号
        //从map中提取出键值对
        String mapStr = content.toString();
        mapStr = mapStr.substring(1, mapStr.length()-1); //去除{}括号
        for (String item : mapStr.split(", ")) {
            msg += item + MESSAGE_SEPARATOR;
        }
        msg += MESSAGE_SEPARATOR;  //追加空行,表示报文结束
        return new Message(msg.getBytes(CHARSET));
    }*/
    
    /**
     * 格式化响应体-弃用.
     * "属性名=属性值"格式,单独一行; 
     * 不含报文体(文件字节)
    public static Message formatResponse(ResponseType responseType, String ...messgeBlock) {
        String msg = responseType.getResponseType() + MESSAGE_SEPARATOR;
        if(messgeBlock != null) {
            for (String block : messgeBlock) {
                msg += block + MESSAGE_SEPARATOR;
            }
        }
        return new Message(msg.getBytes());
    }*/
    
    
    /**处理报文--弃用
     * 只读取socket字节流请求头部分.
     * 同时解压报文.
     
    public static Map<String, Object> acceptMessage(InputStream socketStream) {
        int b = 0;
        StringBuffer strbf = new StringBuffer();  //线程安全
        //读取所有报文头
        if(b!='\n' || (b=socketStream.read())!='\r') {  //"\r\n\r\n"表示报文头结束
            strbf.append(b);
        }
        socketStream.read();  //清除末尾\n
        
        //解压缩, 并将报文转换为字符串
        byte[] bytes = strbf.toString().getBytes(XMTP.CHARSET);
        bytes = decompress(bytes);  //
        String msg = new String(bytes);  
        
        String[] lines = msg.split(XMTP.MESSAGE_SEPARATOR); //提取出每一行
        Map<String, Object> attribute = new HashMap<String, Object>(lines.length);
        for (int i = 0; i < lines.length; i++) {
            
        }
        
        return 
    }*/
    
    /*封装报文信息
    static class Message implements Serializable{
        private static final long serialVersionUID = 5014840769193549286L;
        
        private byte[] message;
        
        public Message(byte[] message) {
            this.setMessage(message);
        }
        
        public byte[] getMessage() {
            return message;
        }
        
        public void setMessage(byte[] message) {
            this.message = message;
        }
    }*/
    
    /*static public class Message{
        private List<String> lines;
        private byte[] body;
        private Charset charset = StandardCharsets.UTF_8;
        
        public Message(byte[] message) { 
            BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(message), charset));
            String line;
            try {
                while((line=reader.readLine()).length() != 0) { //空行"\r\n"代表消息头结束
                    lines.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        public String getMessageHeader() {
            return lines.get(0); 
        }
        
    }*/
    
}
