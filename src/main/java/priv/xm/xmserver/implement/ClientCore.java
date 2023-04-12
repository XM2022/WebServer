package priv.xm.xmserver.implement;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import priv.xm.xmserver.interfaces.MyTransferProtocol;
import priv.xm.xmserver.interfaces.RequestType;
import priv.xm.xmserver.xmLibrary1_0.ConsoleUI;
import priv.xm.xmserver.xmLibrary1_0.ConsoleUI.MenuItem;

public class ClientCore extends MyTransferProtocol {
    private Socket socket;
    //private String tmpFilePath = "D:\\MyClient\\tmp";  //保存断点记录
    
    @SuppressWarnings("unused")
    private Charset charset = StandardCharsets.UTF_8;
    private Path savePath = Paths.get("D:\\MyClient");  //客户端文件保存路径
    
    public ClientCore(String host, int port) throws IOException {
        try {
            socket = new Socket(host, port);
            socketIn = new BufferedInputStream(socket.getInputStream());
            socketOut = new BufferedOutputStream(socket.getOutputStream());
            sizeIn = new DataInputStream(socketIn);
            sizeOut = new DataOutputStream(socketOut);
            
            //发送问候语
            socketOut.write("XMT".getBytes());
            socketOut.flush();
        } 
        catch (UnknownHostException e) {
            System.err.println("输入服务器IP有误!");
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    private FileDialog dlg = new FileDialog(new Frame(), "", FileDialog.LOAD);
    public void uploadFile() {
        dlg.setTitle("请选择上传的文件");
        dlg.setVisible(true);
        message.clear();
        message.put("url", currentDirectory + dlg.getFile());
        try {
            sendMessage(RequestType.UPLOAD_FILE, message);
            acceptMessage();
            long start = System.currentTimeMillis();
            System.out.println("文件上传中...");
            sendFile(Paths.get(dlg.getDirectory(), dlg.getFile()), (Long)message.get("filePointer"));
            System.out.print("文件上传完成！");
            long end = System.currentTimeMillis();
            System.out.printf("共花费%.1fS%n", (end-start)/1000.0);
            
            updateFileList();
            ConsoleUI.anyKeyInput();
        } catch (IOException | NumberFormatException | ClassNotFoundException e) { //异常包装技术--菜单项功能绑定不允许抛出异常
            RuntimeException packedE = new RuntimeException("包装异常");
            packedE.initCause(e);
            throw packedE;
        }
    }
    
    public void downloadFile() {
        String fileName = "";
        System.out.println("是否新命名下载文件?");
        if(ConsoleUI.boolInput()) { 
            System.out.println("请输入新文件名:");
            if(ConsoleUI.input.hasNextLine()) fileName = ConsoleUI.input.nextLine();
        }
        else {
            fileName = selectedFile;
        }

        try {
            downloadFile(fileName);  
        } catch (ClassNotFoundException | IOException e) { //异常包装技术--菜单项功能绑定不允许抛出异常
            RuntimeException packedE = new RuntimeException("包装异常");
            packedE.initCause(e);
            throw packedE;
        }
    }
    
    public void setTransferCompressRatio() {
        setTransferCompressRatio(ConsoleUI.numberInput(0, 9, "请输入压缩传输等级(0~9):"));
    }
    
    /**
     * 
     * @param requestType
     * @param selectedFile  当前浏览目录下的文件, 非绝对路径!
     * @param newFileName
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    public void downloadFile(String newFileName) throws IOException, ClassNotFoundException{
        if(newFileName == null) throw new IllegalArgumentException("文件名或保存文件不能为空!");
        //将文件名更改为临时文件名.
        Path dwFile = Paths.get(savePath.toString(), currentDirectory, newFileName);
        Path dir = dwFile.getParent();
        int separatorPos = newFileName.lastIndexOf('.');
        if(separatorPos==-1) separatorPos = newFileName.length();  //文件名无"."后缀的情况
        Path tmpFile = Paths.get(dir.toString(), newFileName.substring(0, separatorPos) + "-Downloading");  //临时文件名
        long filePointer;
        if(Files.notExists(dir)) Files.createDirectories(dir);
        if(Files.exists(tmpFile)) {
            filePointer = tmpFile.toFile().length();  //从下一字节开始读写
        }
        else {
            Files.createFile(tmpFile);
            filePointer = 0;
        }
        message.clear();
        message.put("url", currentDirectory + this.selectedFile);
        message.put("filePointer", String.valueOf(filePointer));
        sendMessage(RequestType.DOWNLOAD_FILE, message);
        if(acceptSimpleResponse()) {
            long start = System.currentTimeMillis();
            System.out.println("文件下载中...");
            acceptFile(tmpFile, filePointer);
            if(dwFile.toFile().exists()) dwFile.toFile().delete(); //删除并更新同名文件
            tmpFile.toFile().renameTo(dwFile.toFile());  //将临时文件重命名为正式文件
            System.out.print("文件下载完成！");
            long end = System.currentTimeMillis();
            System.out.printf("共花费%.1fS%n", (end-start)/1000.0);
        }
        else {
            System.out.println("服务器仓库上没有此文件!");
            return;
        }
        updateFileList();
        ConsoleUI.anyKeyInput();
    }
    
    /**解析处理服务器仅包含YES/NO的简单响应消息
     * @throws IOException 
     * @throws ClassNotFoundException */
    private boolean acceptSimpleResponse() throws ClassNotFoundException, IOException {
        acceptMessage();
        return "OK".equals(message.get("type"));
    }
    
    private String currentDirectory = "/";  //当前浏览目录; 默认服务器仓库根目录
    private List<String> currentFileList; //注意:保存上一次调用updateFileList所获取的文件列表, 可能不是最新的. 
    
    @SuppressWarnings("unchecked")
    /**
     * 从从服务器中获取currentDirectory当前目录下的最新文件列表.
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void updateFileList() throws IOException, ClassNotFoundException {
        message.clear();
        message.put("browseDirectory", currentDirectory);
        sendMessage(RequestType.BROWSE_FILE, message);
        acceptMessage();
        currentFileList = (List<String>)message.get("files");
    }
    
    
    /**
     * 设置传输压缩率.
     * @param level  0~9
     */
    @Override
    public void setTransferCompressRatio(int level) {
        super.setTransferCompressRatio(level);
    }
    
    private ConsoleUI ui;
    private MenuItem[] defaultRootMenuSet;  /**未选中任何文件的菜单集, 处于根目录*/
    private MenuItem[] defaultEmptyRootMenuSet; //未选中任何文件的菜单集, 处于根目录(空)
    private MenuItem[] defaultMenuSet; //未选中任何文件的菜单集, 处于子目录
    private MenuItem[] defaultEmptyMenuSet; //未选中任何文件的菜单集, 处于子目录(空)
    private MenuItem[] directoryRootMenuSet; //选中目录的菜单集, 处于根目录
    private MenuItem[] directoryMenuSet; //选中目录的菜单集, 处于子目录
    private MenuItem[] fileRootMenuSet; //选中文件的菜单集, 处于根目录
    private MenuItem[] fileMenuSet; //选中文件的菜单集, 处于子目录
    
    
    private void initUI(){
        ui = new ConsoleUI("网盘客户端");
        ui.setListTile("小明的网盘");
        ui.setExitItemName("退出程序");
        ui.setMenuBraidVisible(false);
        ui.setUiLength(56);
        ui.setVerticalShowMenu(false);  //横排显示菜单.
        
        //"1.创建目录 2.上传文件 3.选择文件/目录 4.返回上级 0.退出程序");
        //"1.下载文件 2.删除文件 3.移动文件 4.取消选择 0.退出程序");
        //"1.进入目录 2.删除目录 3.移动目录 4.取消选择 0.退出程序");
        MenuItem createDirMenu = ui.new MenuItem("创建目录", this::createDirectory);
        MenuItem enterDirMenu = ui.new MenuItem("进入目录", this::enterDirectory);
        MenuItem selectMenu = ui.new MenuItem("选择文件/目录", this::selectFile);
        MenuItem uploadMenu = ui.new MenuItem("上传文件", this::uploadFile);
        MenuItem downloadMenu = ui.new MenuItem("下载文件", this::downloadFile);
        MenuItem returnMenu = ui.new MenuItem("返回上级", this::returnLastHierarchy);
        MenuItem deleteDirectoryMenu = ui.new MenuItem("删除目录", this::deleteFile);
        MenuItem deleteFileMenu = ui.new MenuItem("删除文件", this::deleteFile);
        MenuItem moveFileMenu = ui.new MenuItem("移动文件", this::moveFile);
        MenuItem moveDirectoryMenu = ui.new MenuItem("移动目录", this::moveFile);
        MenuItem cancelSelectMenu = ui.new MenuItem("取消选择", this::cancelSelectFile);
        MenuItem searchMenu = ui.new MenuItem("查找文件", this::searchFile);
        //MenuItem setMenu = ui.new MenuItem("设置压缩等级", this::setTransferCompressRatio);
        
        defaultRootMenuSet = new MenuItem[] { createDirMenu, uploadMenu, selectMenu, searchMenu, /*setMenu*/ };
        defaultEmptyRootMenuSet = new MenuItem[]{ createDirMenu, uploadMenu };
        defaultMenuSet = new MenuItem[]{ createDirMenu, uploadMenu, selectMenu, returnMenu };
        defaultEmptyMenuSet = new MenuItem[]{ createDirMenu, uploadMenu, returnMenu };
        directoryRootMenuSet = new MenuItem[]{ enterDirMenu, deleteDirectoryMenu, moveDirectoryMenu, cancelSelectMenu };
        directoryMenuSet = new MenuItem[]{ enterDirMenu, deleteDirectoryMenu, moveDirectoryMenu, returnMenu, cancelSelectMenu };
        fileRootMenuSet = new MenuItem[]{ downloadMenu, deleteFileMenu, moveFileMenu, cancelSelectMenu };
        fileMenuSet = new MenuItem[]{ downloadMenu, deleteFileMenu, moveFileMenu, returnMenu, cancelSelectMenu };
    }
    
    /**
     * 显示控制台界面和功能调度.
     * @throws IOException 
     * @throws ClassNotFoundException 
     * @throws InterruptedException 
     */
    public void showUI() throws ClassNotFoundException, IOException, InterruptedException {
        //System.out.println("请输入要连接服务器IP和端口号:");  --定制客户端不用指定服务器.
        this.printInfo(false);  //关闭报文的打印
        updateFileList();  //从服务器更新currentFileList, 之后采用本地更新.
        
        initUI();  //初始化菜单集
        ui.welcome();
        while(true) {  //空目录
            String positionText = "当前位置:" + currentDirectory;
            int fileCount = currentFileList.size();  //更新/获取文件列表数目
            if("/".equals(currentDirectory)) {  //处于服务器根目录
                if(fileCount == 0) {
                    String emptyDirectoryText = " ".repeat((ui.getUiLength()-13) / 2) + "当前目录为空.";
                    ui.setInsertText(true, positionText, emptyDirectoryText);
                    ui.addMainMenus(defaultEmptyRootMenuSet);
                }
                else {  //非空目录
                    ui.setInsertText(true, positionText);
                    ui.addMainMenus(defaultRootMenuSet);
                }
            }
            else {  //处于服务器子目录
                if(fileCount == 0) {  //空目录
                    String emptyDirectoryText = " ".repeat((ui.getUiLength()-13) / 2) + "当前目录为空.";
                    ui.setInsertText(true, positionText, emptyDirectoryText);
                    ui.addMainMenus(defaultEmptyMenuSet);
                }
                else {  //非空目录
                    ui.setInsertText(true, positionText);
                    ui.addMainMenus(defaultMenuSet);
                }
            }
            ui.showList(currentFileList);  
            List<MenuItem> menus = ui.showMenu();
            
            int selectedMenuNumber = ui.selectMenu(menus.size());
            menus.get(selectedMenuNumber).click();  //执行菜单功能
        } //while(true)
        
    }
    
    private String selectedFile = null;  //当前选中的文件/目录
    
    public void createDirectory() {
        System.out.println("请输入新目录名:");
        String dirName = "";
        while (true) {
            if(ConsoleUI.input.hasNextLine()) dirName = ConsoleUI.input.nextLine();
            try {
                Paths.get(dirName);
            }
            catch (InvalidPathException e) {
                System.out.println("目录名含有非法字符,请重新输入:");
                continue;
            }
            if(currentFileList.contains(dirName)) {
                System.out.println("该目录名已存在,请重新输入:");
                continue;
            }
            break;
        }
        dirName += "/";  //目录追加/
        
        message.clear();
        message.put("url", currentDirectory + dirName);
        try {
            sendMessage(RequestType.CREATE_DIRECTORY, message);
            if(acceptSimpleResponse()) {
                currentFileList.add(dirName);  //直接从本地更新, 避免服务器获取以提升效率
                Collections.sort(currentFileList);
                System.out.println("创建目录成功！");
            }
            else {
                System.out.println("创建目录失败！");
            }
        } catch (ClassNotFoundException | IOException e) {  //异常包装技术--菜单项功能绑定不允许抛出异常
            RuntimeException packedE = new RuntimeException("包装异常");
            packedE.initCause(e);
            throw packedE;
        }
        ConsoleUI.anyKeyInput();
    }
    
    public void selectFile() {
        System.out.println("键入目录/文件前面的序号来选择操作.");
        selectedFile = currentFileList.get(ConsoleUI.numberInput(1, currentFileList.size(), "请输入序号:") -1);
        if(selectedFile.endsWith("/")) {  //选中的是目录
            if("/".equals(currentDirectory)) ui.addMainMenus(directoryRootMenuSet); //处于根目录
            else ui.addMainMenus(directoryMenuSet); //处于子目录
        }
        else { //选中的是文件
            if("/".equals(currentDirectory)) ui.addMainMenus(fileRootMenuSet); //处于根目录
            else ui.addMainMenus(fileMenuSet); //处于子目录
        }
        ui.showList(currentFileList);
        List<MenuItem> menus = ui.showMenu();
        int selectMenuNumber = ui.selectMenu(menus.size());
        menus.get(selectMenuNumber).click();  
    }
    
    public void cancelSelectFile() {
        selectedFile = null;
    }
    
    public void moveFile() {
        System.out.println("功能未实现");
        ConsoleUI.anyKeyInput();
    }
    
    public void returnLastHierarchy() {
        try {
            currentDirectory = currentDirectory.substring(0, currentDirectory.lastIndexOf("/", currentDirectory.length()-2/*跳过末尾的/*/) + 1); 
            updateFileList();
        } catch (ClassNotFoundException | IOException e) {  //异常包装技术--菜单项功能绑定不允许抛出异常
            RuntimeException packedE = new RuntimeException("包装异常");
            packedE.initCause(e);
            throw packedE;
        }
        //continue;
    }
    
    public void enterDirectory() {
        try {
            currentDirectory += selectedFile;
            updateFileList();
        } catch (ClassNotFoundException | IOException e) { //异常包装技术--菜单项功能绑定不允许抛出异常
            RuntimeException packedE = new RuntimeException("包装异常");
            packedE.initCause(e);
            throw packedE;
        }
        //continue;
    }
    
    /**
     * 删除文件或目录.
     * @param selectedFile 仅文件名, 绝对路径由currentDir确定. (客户端单线程)
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    public void deleteFile() {
        boolean isDir = Files.isDirectory(Paths.get(currentDirectory + selectedFile));
        if(isDir) System.out.printf("确定删除目录%s吗？%n", selectedFile);
        else System.out.printf("确定删除文件%s吗？%n", selectedFile);
        if(ConsoleUI.boolInput()== false) {
            System.out.println("操作已取消.");
            ConsoleUI.anyKeyInput();
            return ;
        }
        
        message.clear();
        message.put("url", currentDirectory+selectedFile);
        try {
            sendMessage(RequestType.DELETE_FILE, message);
            if(acceptSimpleResponse()) {
                if(isDir) System.out.println("目录删除成功!");
                else System.out.println("文件删除成功!");
                currentFileList.remove(selectedFile);
                selectedFile = null;
            }
            else {
                if(isDir) System.out.println("目录删除失败!");
                else System.out.println("文件删除失败!");
            }
        } catch (ClassNotFoundException | IOException e) { //异常包装技术--菜单项功能绑定不允许抛出异常
            RuntimeException packedE = new RuntimeException("包装异常");
            packedE.initCause(e);
            throw packedE;
        }
        ConsoleUI.anyKeyInput();
    }
    
    /**
     * 在服务器仓库全局查找文件
     */
    @SuppressWarnings("unchecked")
    public void searchFile() {
        System.out.println("请输入要查找的文件名:");
        String fileName = "";
        if(ConsoleUI.input.hasNextLine()) fileName = ConsoleUI.input.nextLine();
        
        message.clear();
        message.put("url", fileName);
        try {
            sendMessage(RequestType.SEARCH_FILE, message);
            acceptMessage();
            if("OK".equals(message.get("type"))) {
                currentDirectory = (String)message.get("url");
                currentFileList = (List<String>)message.get("files");
            }
            else {
                System.out.println("未查找到该文件.");
                ConsoleUI.anyKeyInput();
            }
        } catch (ClassNotFoundException | IOException e) { //异常包装技术--菜单项功能绑定不允许抛出异常
            RuntimeException packedE = new RuntimeException("包装异常");
            packedE.initCause(e);
            throw packedE;
        } 
        
    }
}
