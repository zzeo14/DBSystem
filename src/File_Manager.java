import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class File_Manager {
    private IO_Manager io = new IO_Manager();
    private byte[] block = new byte[Global_Variables.Block_Size];

    public void create_file(String file_name, Metadata metadata) {
        for (int i = 0; i < Global_Variables.Block_Size; i++) {block[i] = 0;} // block을 0으로 초기화
        
        try {
            File file = new File(file_name + ".txt");

            if (!file.createNewFile()) {
                System.out.println("파일이 이미 존재함");
                return;
            }

            List<Fields> fields = metadata.getFields();
            block[4] = (byte)fields.size(); // field 개수 저장
            int offset = Global_Variables.pointer_bytes + Global_Variables.field_num_bytes; // pointer(0~3), field 개수(4) 이후부터 저장
            for(int i = 0 ; i < fields.size() ; i++){
                System.arraycopy(fields.get(i).getField_name(), 0, block, offset, fields.get(i).getField_name().length);
                offset += Global_Variables.field_name_bytes;
                System.arraycopy(fields.get(i).getField_type(), 0, block, offset, fields.get(i).getField_type().length);
                offset += Global_Variables.field_type_bytes;
                block[offset] = fields.get(i).getField_order();
                offset += Global_Variables.field_order_bytes;
            }

            io.write(block, file_name + ".txt", 0);
        }
        catch (IOException e){
            System.out.println(e);
        }
    }

    // records 파라미터는 아직 에러 감지 안 한 상태
    public void insert_record(List<Record> records, String file_name){
        // 헤더블록 읽은 후 field names, field lengths, field orders 저장
        Header_Content header_content = read_header(file_name);
        if(header_content == null){
            System.out.println("Error occured : Wrong header block");
            return;
        }
        int field_num = header_content.getFieldNum();
        int[] field_lengths = header_content.getFieldLengths();
        List<String> field_names = header_content.getFieldNames();
        byte[] field_orders = header_content.getFieldOrders();

        // 각 record에 대해 record_loop 수행
        int records_size = records.size();
        List<Integer> error_records = new ArrayList<>();

        record_loop:
        for(int rec_num = 0 ; rec_num < records_size ; rec_num++) {
            int record_size = Global_Variables.bitmap_bytes + Global_Variables.pointer_bytes; // 1 bit : bitmap size, 4 bit : pointer size
            Record record = records.get(rec_num);

            List<byte[]> fields = record.getFields();
            byte[] bitmap = record.getBitmap();

            // search key(첫 번째 field)가 null이면 에러처리 //
            if ((bitmap[0] & (1 << 7)) != 0) { inv_q(rec_num); error_records.add(rec_num); continue; }

            int field_cnt = fields.size();
            for (int j = 0; j < field_names.size(); j++) {
                int bitmap_byte = j / 8;
                int bitmap_bit = j % 8;

                if (((bitmap[bitmap_byte] >> (7 - bitmap_bit)) & 1) != 0) field_cnt++;
                else record_size += field_lengths[j];
            }

            // record의 field 개수가 header block 정보와 다르면 에러처리 //
            if (field_cnt != field_num) {inv_q(rec_num); error_records.add(rec_num); continue; }
            // field type size가 안 맞으면 에러처리 //
            int null_cnt = 0;

            for (int j = 0; j < field_names.size(); j++) {
                int bitmap_byte = j / 8;
                int bitmap_bit = j % 8;
                if (((bitmap[bitmap_byte] >> (7 - bitmap_bit)) & 1) != 0) {
                    null_cnt++;
                    continue;
                }

                if (fields.get(j - null_cnt).length != field_lengths[j]) {
                    String a = new String(fields.get(j), StandardCharsets.US_ASCII);
                    System.out.println(a);
                    inv_q(rec_num);
                    error_records.add(rec_num);
                    continue record_loop;
                }
            }
        }

        // 에러 record들 삭제
        for(int i = error_records.size() - 1 ; i >= 0 ; i--){
            System.out.println(error_records.get(i));
            records.remove(records.get(error_records.get(i)));
        }
        
        // 입력한 record들 정렬
        Collections.sort(records);

        // pointer값 가져오기 (함수 내부에서 file에 저장되어 있는 record들의 포인터도 조정함)
        List<byte[]> pointers = io.find_next_pointers(records, file_name + ".txt", field_lengths);
        if(pointers.size() != records.size()){
            pointers.add(io.IntToByte(0, Global_Variables.pointer_bytes));
        }

        // record를 file에 write
        byte[] block = new byte[Global_Variables.Block_Size];
        int offset = 0;
        for(int i = 0 ; i < records.size() ; i++){
            Record record = records.get(i);
            int record_size = io.get_record_length(record, field_lengths);

            // record가 더이상 block에 들어가지 않으면, block 쓰고 초기화
            // block의 마지막 record에 있는 포인터를 그 다음 block의 첫 record의 offset으로 변경해줌
            if(offset + record_size >= Global_Variables.Block_Size){
                io.write(block, file_name + ".txt", -1); // 파일의 맨 뒤에 write
                for(int j = 0 ; j < Global_Variables.Block_Size ; j++) {
                    block[j] = 0;
                }
                offset = 0;
            }

            // bitmap 입력
            System.arraycopy(record.getBitmap(), 0, block, offset, Global_Variables.bitmap_bytes);
            offset += Global_Variables.bitmap_bytes;
            List<byte[]> fields = record.getFields();
            // 각 field 입력
            for(int j = 0 ; j < fields.size() ; j++) {
                System.arraycopy(fields.get(j), 0, block, offset, fields.get(j).length);
                offset += fields.get(j).length;
            }

            System.arraycopy(pointers.get(i), 0, block, offset, Global_Variables.pointer_bytes);
            offset += Global_Variables.pointer_bytes;
        }
        io.write(block, file_name + ".txt", -1);
    }

    private Header_Content read_header(String file_name){

        byte[] header_block = new byte[Global_Variables.Block_Size];
        header_block = io.read(file_name + ".txt", 0);

        if(!io.is_file_exist(file_name + ".txt")){
            System.out.println(file_name + " 파일이 존재하지 않음");
            return null;
        }
        int offset = Global_Variables.pointer_bytes;
        int field_num = header_block[offset]; // field 개수
        offset += Global_Variables.field_num_bytes;
        List<String> field_names = new ArrayList<>();
        int[] field_lengths = new int[field_num];
        byte[] field_orders = new byte[field_num]; // field 개수만큼 order변수 생성
        int order_cnt = 0;

        Pattern pattern = Pattern.compile("char\\((\\d+)\\)"); // char(124) -> 124 처럼 char () 내부의 integer 뽑기
        while(offset < Global_Variables.Block_Size){
            // field name 읽기
            String field_name = "";
            for(int i = offset; i < offset + Global_Variables.field_name_bytes ; i++){
                if(header_block[i] != 0) field_name += (char)header_block[i];
            }
            if(field_name.equals("")) break;
            offset += Global_Variables.field_name_bytes;

            // field type 읽기
            String field_type = "";
            int length = 0;
            for(int i = offset ; i < offset + Global_Variables.field_type_bytes ; i++){
                if(header_block[i] != 0  && (char)header_block[i] != ',') field_type += (char)header_block[i];
            }
            if(field_type.equals("")) break;
            Matcher matcher = pattern.matcher(field_type);
            if(matcher.find()){
                length = Integer.parseInt(matcher.group(1));
            }
            offset += Global_Variables.field_type_bytes;

            // field order 읽기
            byte field_order = header_block[offset];
            offset += Global_Variables.field_order_bytes;

            field_names.add(field_name);
            field_lengths[order_cnt] = length;
            field_orders[order_cnt++] = field_order;
        }

        // return값 저장
        Header_Content header_content = new Header_Content();
        header_content.SetFieldNum(field_num);
        header_content.SetField_names(field_names);
        header_content.SetField_lengths(field_lengths);
        header_content.SetField_orders(field_orders);
        return header_content;
    }

    // error handling
    public void inv_q(int i) {
        System.out.println("Invalid query at line " + (4 + i));
    }
}
