package maigosoft.mcpdict;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;

public class FileUtils {

    public static void copyFile(String srcPath, String dstPath) throws IOException {
        makeParentDirs(dstPath);
        File srcFile = new File(srcPath);
        File dstFile = new File(dstPath);
        FileInputStream srcStream = new FileInputStream(srcFile);
        FileOutputStream dstStream = new FileOutputStream(dstFile);
        FileChannel srcChannel = srcStream.getChannel();
        FileChannel dstChannel = dstStream.getChannel();
        srcChannel.transferTo(0, srcChannel.size(), dstChannel);
        srcStream.close();
        dstStream.close();
    }

    public static void makeParentDirs(String path) throws IOException {
        File parent = new File(path).getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
    }

    public static void dumpException(String path, Throwable e) throws IOException {
        PrintWriter writer = new PrintWriter(path);
        e.printStackTrace(writer);
        writer.close();
    }
}
