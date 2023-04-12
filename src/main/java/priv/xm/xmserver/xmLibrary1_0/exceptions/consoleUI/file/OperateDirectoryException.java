package priv.xm.xmserver.xmLibrary1_0.exceptions.consoleUI.file;

import java.io.IOException;

public class OperateDirectoryException extends IOException {
    private static final long serialVersionUID = 4617197069541841639L;
    
    public OperateDirectoryException() {  }
    public OperateDirectoryException(String msg) {
        super(msg);
    }
}
