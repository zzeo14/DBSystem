import java.io.RandomAccessFile;
import java.io.File;
import java.util.List;
import java.io.IOException;

public class File_Manager {
    private static final int Block_Size = 512;
    private IO_Manager io = new IO_Manager();
    private byte[] block = new byte[Block_Size];

    public void create_file(String file_name, Metadata metadata) {
        for (int i = 0; i < Block_Size; i++) {block[i] = 0;} // block을 0으로 초기화
        
        try {
            File file = new File(file_name + ".txt");

            if (!file.createNewFile()) {
                System.out.println("File already exists!");
                return;
            }

            List<Fields> fields = metadata.getFields();
            int offset = 4; // pointer(0~3바이트) 이후부터 저장
            for(int i = 0 ; i < fields.size() ; i++){
                System.arraycopy(fields.get(i).getField_name(), 0, block, offset, fields.get(i).getField_name().length);
                offset += 16;
                System.arraycopy(fields.get(i).getField_type(), 0, block, offset, fields.get(i).getField_type().length);
                offset += 8;
                block[offset] = fields.get(i).getField_order();
                offset += 1;
            }

            io.write(block, file_name + ".txt", 0);
        }
        catch (IOException e){
            System.out.println(e);
        }
    }

    public void insert_data(){

    }

    public static int getBlock_Size(){ return Block_Size; }
}
