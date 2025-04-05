import java.io.IOException;
import java.io.RandomAccessFile;

public class IO_Manager {

    public void write(byte[] s, String path, long offset) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(path, "rw");
        raf.seek(offset);
        raf.write(s);

        raf.close();
    }

    public byte[] read(String path, long offset) {
        byte[] ret_bytes = new byte[File_Manager.getBlock_Size()];
        try{
            RandomAccessFile file = new RandomAccessFile(path, "r");

            file.seek(offset);
            file.read(ret_bytes);

            file.close();
        }
        catch (IOException e){
            System.out.println("IO Exception 발생");
        }
        return ret_bytes;
    }
}
