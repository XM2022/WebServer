package priv.xm.xmserver.interfaces;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {

    public static void main(String[] args) throws InterruptedException, IOException {
        /*System.out.println(Paths.get("te", "rt"));
        FileDialog fileDialog = new FileDialog(new Frame(), "丰登", FileDialog.SAVE);
        //fileDialog.setFile("D://MyClient");
        fileDialog.setDirectory("D://MyClient");
        fileDialog.setVisible(true);
        System.out.println(fileDialog.getDirectory());
        System.out.println(fileDialog.getFile());
        System.out.println(Arrays.toString(fileDialog.getFiles()));
        File file;
        Path dirA = Paths.get("D://A");
        System.out.println("\nnewDirectoryStream:");
        try(DirectoryStream<Path> newDirectoryStream = Files.newDirectoryStream(dirA)){
            for (Path path : newDirectoryStream) {
                System.out.println(path);
            }
            
        }
        
        System.out.println("\nlistFiles:");
        for (File f : dirA.toFile().listFiles()) {
            System.out.println(f);
        }
        System.out.println("\nlist:");
        for (String f : dirA.toFile().list()) {
            System.out.println(f);
        }
        
        System.out.println("\nwalkFileTree:");
        Files.walkFileTree(Paths.get("D:/A/测试55.txt"), new SimpleFileVisitor<Path>() {
        
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                //System.out.println(dir);
                return super.preVisitDirectory(dir, attrs);
            }
        
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println(file);
                return super.visitFile(file, attrs);
            }
        
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                // TODO Auto-generated method stub
                return super.visitFileFailed(file, exc);
            }
        
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                //System.out.println(dir);
                return super.postVisitDirectory(dir, exc);
            }
            
        });
        
        Stream<Path> walk = Files.walk(dirA, 10, FileVisitOption.FOLLOW_LINKS);  //非IO流, 另一种类型的流
        System.out.println("\nwalk:");
        System.out.println(Paths.get("").toAbsolutePath());
        
        try {
            System.out.println(Paths.get("F%?F"));
        }catch(InvalidPathException e) {
            System.err.println("路径有误");
            //throw e;
        }
        */
        System.out.println(Paths.get("D:/DD/D2").getFileName().toAbsolutePath());
        System.out.println(Files.isDirectory(Paths.get("/A")));
        System.out.println("/A".substring(0, "/A".lastIndexOf('/')) + "|");
        
        Test test = new Test();
        
        System.out.printf("%2s%n|", "fds");
        
        System.out.println(Paths.get("/A/").toAbsolutePath());
        String s1 = "123|";
        System.out.println(s1.substring(0, s1.lastIndexOf("|", s1.length()-1)));
        System.out.println(s1.lastIndexOf("|", s1.length()-2));
        System.out.println(Paths.get("D:/A/A3").getFileName().toAbsolutePath());
        System.out.println(Paths.get("D:/A/char5435.txt").toFile().length());
        
        byte[] bs = new byte[10];
        System.out.println(Arrays.toString(bs));
        //ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("12367879809090".getBytes());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(20);
        byteArrayOutputStream.write("123".getBytes());
        //byteArrayInputStream.read(bs);
        byteArrayOutputStream.toByteArray();
        System.out.println(Arrays.toString(bs));
        
        System.exit(0);
    }

    
}


