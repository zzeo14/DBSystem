import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class IO_Manager {

    public void write(byte[] s, String path, long offset) {
        try {
            RandomAccessFile file = new RandomAccessFile(path, "rw");
            if (offset == -1) file.seek(file.length());
            else file.seek(offset);
            file.write(s);

            file.close();
        } catch (IOException e) {
            System.out.println("IO Exception 발생");
            e.printStackTrace();
        }
    }

    public byte[] read(RandomAccessFile file, long offset) throws IOException {
        byte[] ret_bytes = new byte[Global_Variables.Block_Size];
        file.seek(offset);
        file.read(ret_bytes);

        file.close();
        return ret_bytes;
    }

    public byte[] read(String path, long offset) {
        byte[] ret_bytes = new byte[Global_Variables.Block_Size];
        try {
            RandomAccessFile file = new RandomAccessFile(path, "rw");
            ret_bytes = read(file, offset);
        } catch (IOException e) {
            System.out.println("IO Exception 발생");
            e.printStackTrace();
        }
        return ret_bytes;
    }

    public boolean is_file_exist(String path) {
        File file = new File(path);
        return file.exists();
    }

    // 현재 record의 길이를 찾는 method             //
    // record: 길이를 찾을 record                 //
    // field_lengths : file의 column들의 길이 배열 //
    public int get_record_length(Record record, int[] field_lengths) {
        int length = 0;

        byte[] bitmap = record.getBitmap();
        return get_record_length(bitmap, field_lengths);
    }

    public int get_record_length(byte[] bitmap, int[] field_lengths) {
        // default : bitmap과 pointer
        int length = Global_Variables.bitmap_bytes + Global_Variables.pointer_bytes;

        for (int i = 0; i < field_lengths.length; i++) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;

            // i / 8번째 byte의 왼쪽에서 i % 8번째 bit가 1인지 확인
            boolean isNull = (bitmap[byteIndex] & (1 << (7 - bitIndex))) != 0;

            if (!isNull) length += field_lengths[i];
        }
        return length;
    }

    // integer를 byte 배열로 변환
    public byte[] IntToByte(int value, int byte_size) {
        byte[] result = new byte[byte_size];
        for (int i = 0; i < byte_size; i++) {
            result[byte_size - 1 - i] = (byte) ((value >> (8 * i)) & 0xFF);
        }
        return result;
    }

    // byte 배열을 integer로 변환
    public int ByteToInt(byte[] bytes) {
        int result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result = result << 8 | (bytes[i] & 0xFF);
        }
        return result;
    }

    // record의 위치를 이용해 해당 record의 search key를 반환
    public byte[] find_search_key(byte[] block, int block_offset, int search_key_size) {
        byte[] ret_value = new byte[search_key_size];
        System.arraycopy(block, block_offset + Global_Variables.bitmap_bytes, ret_value, 0, search_key_size);
        return ret_value;
    }

    public void insert_first_record(Record record, byte[] header, String path, int[] field_lengths) {
        // 새로운 블록 위치 저장
        int record_offset = Global_Variables.Block_Size;
        System.arraycopy(IntToByte(record_offset, Global_Variables.pointer_bytes), 0, header, 0, Global_Variables.pointer_bytes);

        // 블록 개수 1로 지정
        byte[] block_number = IntToByte(1, Global_Variables.Block_number_bytes);
        System.arraycopy(block_number, 0, header, Global_Variables.field_num_bytes + Global_Variables.pointer_bytes, Global_Variables.Block_number_bytes);
        write(header, path, 0);

        byte[] new_block = new byte[Global_Variables.Block_Size];
        byte[] bitmap = record.getBitmap();

        // bitmap 입력
        int offset = 0;
        System.arraycopy(bitmap, 0, new_block, offset, Global_Variables.bitmap_bytes);
        offset += Global_Variables.bitmap_bytes;

        // field 입력
        List<byte[]> fields = record.getFields();
        for (int i = 0; i < fields.size(); i++) {
            byte[] field = fields.get(i);
            System.arraycopy(field, 0, new_block, offset, field.length);
            offset += field.length;
        }
        write(new_block, path, Global_Variables.Block_Size);
    }

    // block들 중에서 record 길이만큼 비어있는 공간을 찾음.
    // 비어있는 block이 하나도 없다면, -1 return
    public int find_my_offset(byte[][] blocks, Boolean[] check, int record_length, int[] field_lengths) {
        for (int i = 1; i < blocks.length; i++) {
            if (!check[i]) continue;

            int offset = 0;
            while(true){
                byte[] bitmap = new byte[Global_Variables.bitmap_bytes];
                System.arraycopy(blocks[i], offset, bitmap, 0, Global_Variables.bitmap_bytes);
                int length = get_record_length(bitmap, field_lengths);

                // 레코드가 없는 부분 판단 후 블록에 길이 측정 -> 넣을 수 있는지 판단 후 return
                Boolean is_exist_nonzero = false;
                for(int j = offset ; j < offset + length && j < Global_Variables.Block_Size ; j++){
                    if(blocks[i][j] != 0) is_exist_nonzero = true;
                }
                if(!is_exist_nonzero){

                    int k = (Global_Variables.Block_Size - 1) - offset; // 남은 byte 수
                    if(k >= record_length) return Global_Variables.Block_Size * i + offset;
                    else break;
                }
                else{
                    offset += length;
                }
            }
        }
        return -1;
    }

    // block size를 1만큼 증가시킬 때 헤더의 block 개수를 업데이트하는 method
    public void update_header_with_new_block(byte[] header, String path){
        int offset = Global_Variables.pointer_bytes + Global_Variables.field_num_bytes;
        byte[] block_num = new byte[Global_Variables.Block_number_bytes];

        System.arraycopy(header, offset, block_num, 0, Global_Variables.Block_number_bytes);
        int new_block_num = ByteToInt(block_num) + 1; // 1만큼 증가

        block_num = IntToByte(new_block_num, Global_Variables.Block_number_bytes);
        System.arraycopy(block_num, 0, header, offset, Global_Variables.Block_number_bytes);

        write(header, path, 0);
    }

    public void insert_record(Record record, String path, int[] field_lengths) {
        try {
            RandomAccessFile file = new RandomAccessFile(path, "rw");
            byte[] header;
            header = read(file, 0);

            Boolean has_first_record = false;
            for (int i = 0; i < Global_Variables.pointer_bytes; i++) {
                if (header[i] != 0) has_first_record = true;
            }

            // 헤더블록만 있으면 헤더블록에 포인터 설정하고 새로운 블록을 만들어서 disk에 write
            if (!has_first_record) {
                insert_first_record(record, header, path, field_lengths);
                return;
            }

            // block에 쓸 record 정보 배열 미리 만들어놓기
            int my_record_length = get_record_length(record, field_lengths);
            byte[] new_record = new byte[my_record_length];
            int offset = 0;
            System.arraycopy(record.getBitmap(), 0, new_record, offset, Global_Variables.bitmap_bytes);
            offset += Global_Variables.bitmap_bytes;
            for(int i = 0 ; i < record.getFields().size() ; i++){
                byte[] field = record.getFields().get(i);
                System.arraycopy(field, 0, new_record, offset, field.length);
                offset += field.length;
            }

            // 헤더에 있는 레코드 포인터 가져오기
            byte[] first_record = new byte[Global_Variables.pointer_bytes];
            System.arraycopy(header, 0, first_record, 0, Global_Variables.pointer_bytes);

            // 헤더에서 블록 개수 가져오기
            byte[] block_number = new byte[Global_Variables.Block_number_bytes];
            System.arraycopy(header, Global_Variables.pointer_bytes + Global_Variables.field_num_bytes, block_number, 0, Global_Variables.Block_number_bytes);
            int block_num = ByteToInt(block_number);
            byte[][] blocks = new byte[block_num + 1][];
            Boolean[] check_block = new Boolean[block_num + 1];
            for(int i = 0 ; i < block_num + 1 ; i++) check_block[i] = false;
            for(int i = 0 ; i < block_num + 1 ; i++) blocks[i] = new byte[Global_Variables.Block_Size];

            int record_offset = ByteToInt(first_record);
            int before_block_number = -1;
            int before_block_offset = -1;
            int before_block_pointer_offset = -1;
            while (true) {
                int Block_number = record_offset / Global_Variables.Block_Size;
                int offset_in_block = record_offset % Global_Variables.Block_Size;
                System.out.println("Block number: " + Block_number + " offset: " + offset_in_block);
                System.out.println("record_offset: " + record_offset + ", before block number: " + before_block_number + ", before block offset: " + before_block_offset + ", before block pointer offset: " + before_block_pointer_offset);

                // 블록 없으면 disk에서 블러오기
                if (!check_block[Block_number]) {
                    check_block[Block_number] = true;
                    byte[] block = read(path, Block_number * Global_Variables.Block_Size);
                    blocks[Block_number] = block;
                }

                byte[] my_search_key = record.getFields().getFirst();
                byte[] file_search_key = find_search_key(blocks[Block_number], offset_in_block, my_search_key.length);

                int my_record_offset = find_my_offset(blocks, check_block, my_record_length, field_lengths);
                int my_record_block_number = -1;
                int my_record_block_offset = -1;
                // 입력한 record의 search key가 더 작은 경우 -> file에 입력
                if (Arrays.compare(my_search_key, file_search_key) <= 0) {

                    // 들어갈 자리가 없으면 새로운 블록 생성
                    if (my_record_offset == -1) {
                        System.out.println("Hello 1");
                        // 변수들 업데이트
                        update_header_with_new_block(header, path);
                        check_block = Arrays.copyOf(check_block, check_block.length + 1);
                        blocks = Arrays.copyOf(blocks, blocks.length + 1);
                        block_num++;
                        check_block[block_num] = true;
                        blocks[block_num] = new byte[Global_Variables.Block_Size];
                        
                        // 새로운 블록 == blocks 배열의 마지막 block
                        byte[] before_block_pointer = new byte[Global_Variables.pointer_bytes];
                        System.arraycopy(blocks[before_block_number], before_block_pointer_offset, before_block_pointer, 0, Global_Variables.pointer_bytes);
                        // 이전 레코드가 가리키던 레코드를 가리킴
                        System.arraycopy(before_block_pointer, 0, new_record, offset, Global_Variables.pointer_bytes);

                        // file의 이전 record가 자신을 가리키도록 저장 후 write
                        System.arraycopy(IntToByte(Global_Variables.Block_Size * (block_num), Global_Variables.pointer_bytes), 0, blocks[before_block_number], before_block_pointer_offset, Global_Variables.pointer_bytes);

                        // block에 복사
                        System.arraycopy(new_record, 0, blocks[block_num], 0, my_record_length);

                        my_record_block_number = block_num;
                    }
                    // 자리 있으면 거기에 record 넣고, 포인터 업데이트
                    else {
                        System.out.println("Hello 2");
                        my_record_block_number = my_record_offset / Global_Variables.Block_Size;
                        my_record_block_offset = my_record_offset % Global_Variables.Block_Size;
                        byte[] before_block_pointer = new byte[Global_Variables.pointer_bytes];
                        System.arraycopy(blocks[before_block_number], before_block_pointer_offset, before_block_pointer, 0, Global_Variables.pointer_bytes);
                        // 이전레코드에 내 주소 쓰기
                        System.arraycopy(IntToByte(my_record_offset, Global_Variables.pointer_bytes), 0, blocks[before_block_number], before_block_pointer_offset, Global_Variables.pointer_bytes);

                        // 내 레코드에 이전 레코드가 가리키던 레코드 가리키기
                        System.arraycopy(before_block_pointer, 0, new_record, offset, Global_Variables.pointer_bytes);
                        
                        // block에 복사
                        System.arraycopy(new_record, 0, blocks[my_record_block_number], my_record_block_offset, my_record_length);
                    }
                    
                    // before block, my block disk에 쓴 후 함수종료
                    write(blocks[my_record_block_number], path, Global_Variables.Block_Size * my_record_block_number);
                    if(my_record_block_number != before_block_number)
                        write(blocks[before_block_number], path, Global_Variables.Block_Size * before_block_number);

                    return;
                }
                // 더 큰 경우 file의 다음 record 탐색
                else {
                    byte[] current_record_bitmap = new byte[Global_Variables.bitmap_bytes];
                    System.arraycopy(blocks[Block_number], offset_in_block, current_record_bitmap, 0, Global_Variables.bitmap_bytes);
                    int current_record_length = get_record_length(current_record_bitmap, field_lengths);
                    byte[] next_record_offset = new byte[Global_Variables.pointer_bytes];

                    int current_record_pointer = offset_in_block + current_record_length - Global_Variables.pointer_bytes;
                    System.arraycopy(blocks[Block_number], current_record_pointer, next_record_offset, 0, Global_Variables.pointer_bytes);

                    before_block_number = Block_number;
                    before_block_pointer_offset = current_record_pointer;
                    before_block_offset = offset_in_block;
                    record_offset = ByteToInt(next_record_offset);
                }

                // 다음 레코드가 없다면, 내 레코드의 search key가 가장 큰 레코드가 됨
                // 레코드가 들어간 자리를 찾고, 이전의 레코드가 자신을 가리키게 한 후 write. 그리고 return
                if(record_offset == 0){
                    if(my_record_offset == -1){
                        System.out.println("Hello 3");
                        // 변수 업데이트
                        update_header_with_new_block(header, path);
                        check_block = Arrays.copyOf(check_block, check_block.length + 1);
                        blocks = Arrays.copyOf(blocks, blocks.length + 1);
                        block_num++;
                        check_block[block_num] = true;
                        blocks[block_num] = new byte[Global_Variables.Block_Size];

                        // 이전레코드에 내 주소 쓰기
                        System.arraycopy(IntToByte(my_record_offset, Global_Variables.pointer_bytes), 0, blocks[before_block_number], before_block_pointer_offset, Global_Variables.pointer_bytes);

                        // block에 복사
                        System.arraycopy(new_record, 0, blocks[block_num], 0, my_record_length);

                        my_record_block_number = block_num;
                        my_record_offset = block_num * Global_Variables.Block_Size;
                    }
                    else{
                        System.out.println("Hello 4");
                        my_record_block_number = my_record_offset / Global_Variables.Block_Size;
                        my_record_block_offset = my_record_offset % Global_Variables.Block_Size;
                        // 이전레코드에 내 주소 쓰기
                        System.arraycopy(IntToByte(my_record_offset, Global_Variables.pointer_bytes), 0, blocks[before_block_number], before_block_pointer_offset, Global_Variables.pointer_bytes);
                        // block에 복사
                        System.arraycopy(new_record, 0, blocks[my_record_block_number], my_record_block_offset, my_record_length);
                    }
                    // before block, my block disk에 쓴 후 함수종료
                    write(blocks[before_block_number], path, Global_Variables.Block_Size * before_block_number);
                    write(blocks[my_record_block_number], path, my_record_block_number * Global_Variables.Block_Size);
                    return;
                }
            }

        } catch (Exception e) {
            System.out.println("IO Exception 발생");
            e.printStackTrace();
        }
    }

    public void insert_records(List<Record> records, String path, int[] field_lengths) {
        for (int i = 0 ; i < records.size() ;i++) {
            insert_record(records.get(i), path, field_lengths);
        }
    }
}