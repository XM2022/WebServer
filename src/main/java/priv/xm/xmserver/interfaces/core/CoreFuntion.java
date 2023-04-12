package priv.xm.xmserver.interfaces.core;

/**服务器的内核功能*/
public interface CoreFuntion {
    void createFolder(String folderName, String path);
    void showFiles(String path);
    void addFile(String path);    //二叉树
    void deleteFile(String path);
    void breakpointTransport();  //断点续传
    void highSpeedTransmission();  //多线程高速传输
    void sendProgress();  //给客户端或浏览器发送传输进度, 便于表现层进行展示.
}
