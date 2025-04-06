import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class IO_Manager {

    public void write(byte[] s, String path, long offset) {
        try {
            RandomAccessFile file = new RandomAccessFile(path, "rw");
            file.seek(offset);
            file.write(s);

            file.close();
        }
        catch(IOException e){
            System.out.println("IO Exception 발생");
            e.printStackTrace();
        }
    }

    public void write_block(byte[] s, String path) {
        try {
            RandomAccessFile file = new RandomAccessFile(path, "rw");
            if (is_header_pointer_filled(path)) {

            }
            else { // 헤더블록이 비어있으면 헤더블록에 포인터 추가 후 파일쓰기
                byte[] header = new byte[File_Manager.getBlock_Size()];
                file.seek(0);
                file.read(header);

                // block size를 byte배열로 변환
                // header block이 0 ~ block_size - 1까지이므로,
                // first block은 block_size offset부터 시작한다.
                byte[] first_block_pointer = new byte[4];
                for(int i = 0; i < 4; i++){
                    first_block_pointer[i] = (byte)(File_Manager.getBlock_Size() >>> (8 * (3-i)));
                    header[i] = first_block_pointer[i];
                }

                file.seek(File_Manager.getBlock_Size());
                file.write(s);

                file.close();
            }
        }
        catch(IOException e){
            System.out.println("IO Exception 발생");
            e.printStackTrace();
        }
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
            e.printStackTrace();
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
            file.close();
            return false;
        }
        catch (IOException e){
            System.out.println("IO Exception 발생");
            e.printStackTrace();
            return false;
        }
    }
}
