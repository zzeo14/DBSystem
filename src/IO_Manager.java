import java.io.File;
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

    public boolean is_file_exist(String path) {
        File file = new File(path);
        return file.exists();
    }

    public boolean is_header_pointer_filled(String path){
        byte[] header_block = new byte[File_Manager.getBlock_Size()];
        try{
            RandomAccessFile file = new RandomAccessFile(path, "r");

            file.seek(0);
            file.read(header_block);

            for(int i = 0 ; i < 4 ; i++){
                if(header_block[i] != 0) {
                    System.out.println(header_block[i]);
                    return true;
                }
            }
            return false;
        }
        catch (IOException e){
            System.out.println("IO Exception 발생");
            e.printStackTrace();
            return false;
        }
    }
}
