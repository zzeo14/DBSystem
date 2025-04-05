import java.io.IOException;
import java.io.RandomAccessFile;

public class IO_Manager {

    public void write(byte[] s, String path, long offset) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(path, "rw");
        raf.seek(offset);
        raf.write(s);

        raf.close();
    }

    public byte[] read(String path, long offset) throws IOException {
        RandomAccessFile file = new RandomAccessFile(path, "r");

        file.seek(offset);
        byte[] ret_bytes = new byte[File_Manager.getBlock_Size()];
        file.read(ret_bytes);

        file.close();
        return ret_bytes;
    }
}
