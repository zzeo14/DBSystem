import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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

    // records 파라미터는 아직 에러 감지 안 한 상태
    //
    public void insert_record(List<Record> records, String file_name){
        byte[] header_block = new byte[Block_Size];
        header_block = io.read(file_name + ".txt", 0);
        int offset = 4;
        if(!io.is_file_exist(file_name + ".txt")){
            System.out.println(file_name + " 파일이 존재하지 않음");
            return;
        }

        int field_num = header_block[offset]; // field 개수
        offset++;

        List<String> field_names = new ArrayList<>();
        int[] field_lengths = new int[field_num];
        byte[] field_orders = new byte[field_num]; // field 개수만큼 order변수 생성
        int order_cnt = 0;

        Pattern pattern = Pattern.compile("char\\((\\d+)\\)"); // char(124) -> 124 처럼 char () 내부의 integer 뽑기
        // 헤더블록 읽기
        while(offset < Block_Size){
            // field name 읽기
            String field_name = "";
            for(int i = offset; i < offset + 16 ; i++){
                if(header_block[i] != 0) field_name += (char)header_block[i];
            }
            if(field_name.equals("")) break;
            offset += 16;
            
            // field type 읽기
            String field_type = "";
            int length = 0;
            for(int i = offset ; i < offset + 8 ; i++){
                if(header_block[i] != 0  && (char)header_block[i] != ',') field_type += (char)header_block[i];
            }
            if(field_type.equals("")) break;
            Matcher matcher = pattern.matcher(field_type);
            if(matcher.find()){
                length = Integer.parseInt(matcher.group(1));
            }
            offset += 8;
            
            // field order 읽기
            byte field_order = header_block[offset];
            offset++;

            field_names.add(field_name);
            field_lengths[order_cnt] = length;
            field_orders[order_cnt++] = field_order;
        }

        offset = 0;
        // 각 record에 대해 수행
        for(int i = 0 ; i < records.size() ; i++){
            int record_size = 1 + 4; // 1 bit : bitmap size, 4 bit : pointer size
            Record record = records.get(i);

            List<byte[]> fields = record.getFields();
            byte bitmap = record.getBitmap();

            // search key(첫 번째 field)가 null이면 에러처리 //
            if((bitmap & 1 << 7) != 0) { inv_q(i); continue; }

            int field_cnt = fields.size();
            for(int j = 0 ; j < 8 && j < field_names.size() ; j++){
                if((bitmap & 1 << (7 - j)) != 0) field_cnt++;
                else record_size += field_lengths[j];
            }
            if(field_cnt != field_num){ inv_q(i); continue; } // field의 개수가 header block 정보와 다르면 에러처리

            // field type size가 안 맞으면 에러처리 //
            //System.out.println("line: " + (i + 4));
            int null_cnt = 0;
            for(int j = 0 ; j < field_cnt ; j++){
                if((bitmap & 1 << (7 - j)) != 0) { null_cnt++; continue; }
                if(fields.get(j - null_cnt).length != field_lengths[j]) { inv_q(i); continue; }
            }
            
            // block 채우고 쓰기
            //System.out.println("record size: " + record_size);
            if(record_size + offset >= Block_Size){ // record를 block에 넣을 수 없으면 block write 후 새로운 block 쓰기
                io.write_block(block, file_name + ".txt");
                block_initialize();
                offset = 0;
            }
            // block에 공간 남아있으면 record 삽입하기
            block[offset++] = bitmap;                               // 1 : bitmap정보 삽입

            for(int j = 0 ; j < field_cnt ; j++){                   // 2 : field 정보 삽입
                for(int k = 0 ; k < fields.get(j).length ; k++){
                    block[offset++] = fields.get(j)[k];
                }
            }

            for(int j = 0 ; j < 4; j++){                            // 3 : pointer 정보 삽입
                block[offset++] = (byte)0xFF;
            }

            int before_record_offset = offset - record_size - 4;
            for(int j = 0 ; j < 4 ; j++){
                block[before_record_offset++] = (byte)0xFF;
            }
        }

    }

    public static int getBlock_Size(){ return Block_Size; }
    
    public void inv_q(int i) {
        System.out.println("Invalid query at line " + Integer.toString(4 + i));
    }
    
    private void block_initialize(){
        for(int i = 0 ; i < Block_Size ; i++){
            block[i] = 0;
        }
    }
}
