package priv.xm.xmserver.interfaces.service;

/**客户端业务层功能接口*/
public interface ClientService {
    void showProgress();  //控制台显示百分比进度条
    void uploadFile();
    void downloadFile();
}
