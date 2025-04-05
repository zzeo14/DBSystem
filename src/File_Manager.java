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
                System.out.println("파일이 이미 존재함");
                return;
            }

            List<Fields> fields = metadata.getFields();
            block[4] = (byte)fields.size(); // field 개수 저장
            int offset = 5; // pointer(0~3), field 개수(4) 이후부터 저장
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

    public void insert_record(List<Record> records, String file_name){
        byte[] header_block = new byte[Block_Size];
        header_block = io.read(file_name + ".txt", 0);
        int offset = 4 + 16;

        for(int i = 0 ; i < records.size() ; i++){
            Record record = records.get(i);

            List<byte[]> fields = record.getFields();
            byte bitmap = record.getBitmap();

            int field_cnt = fields.size();
            for(int j = 0 ; j < 8 ; j++){
                if((bitmap & 1 << (8 - j)) != 0) field_cnt++;
            }

            if(field_cnt != header_block[4]){ inv_q(i); continue; }

        }

    }

    public static int getBlock_Size(){ return Block_Size; }
    public void inv_q(int i) {
        System.out.println("Invalid query at line " + Integer.toString(4 + i));
    }
}
