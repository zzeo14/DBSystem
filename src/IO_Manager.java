import java.io.IOException;
import java.io.RandomAccessFile;

public class IO_Manager {

    public void write(byte[] s, String path, long offset) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(path, "rw");
        raf.seek(offset);
        raf.write(s);

        raf.close();
    }
}
