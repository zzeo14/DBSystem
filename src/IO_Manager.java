import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.RandomAccess;

public class IO_Manager {

    public void write(byte[] s, String path, long offset) {
        try {
            RandomAccessFile file = new RandomAccessFile(path, "rw");
            if(offset == -1)  file.seek(file.length());
            else file.seek(offset);
            file.write(s);

            file.close();
        }
        catch(IOException e){
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

    public byte[] read(String path, long offset){
        byte[] ret_bytes = new byte[Global_Variables.Block_Size];
        try{
            RandomAccessFile file = new RandomAccessFile(path, "rw");
            ret_bytes = read(file, offset);
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

    public boolean is_header_pointer_filled(RandomAccessFile file) throws IOException {
        byte[] header_block = new byte[Global_Variables.Block_Size];
        file.seek(0);
        file.read(header_block);

        for(int i = 0 ; i < Global_Variables.pointer_bytes ; i++){
            if(header_block[i] != 0) {
                System.out.println(header_block[i]);
                return true;
            }
        }
        return false;
    }

    // 현재 record의 길이를 찾는 method             //
    // record: 길이를 찾을 record                 //
    // field_lengths : file의 column들의 길이 배열 //
    public int get_record_length(Record record, int[] field_lengths) {
        int length = 0;

        byte[] bitmap = record.getBitmap();
        return get_record_length(bitmap, field_lengths);
    }

    public int get_record_length(byte[] bitmap, int[] field_lengths){
        // default : bitmap과 pointer
        int length = Global_Variables.bitmap_bytes + Global_Variables.pointer_bytes;

        for(int i = 0 ; i < field_lengths.length ; i++){
            int byteIndex = i / 8;
            int bitIndex = i % 8;

            // i / 8번째 byte의 왼쪽에서 i % 8번째 bit가 1인지 확인
            boolean isNull = (bitmap[byteIndex] & (1 << (7 - bitIndex))) != 0;

            if(!isNull) length += field_lengths[i];
        }
        return length;
    }

    // integer를 byte 배열로 변환
    public byte[] IntToByte(int value, int byte_size) {
        byte[] result = new byte[byte_size];
        for(int i = 0 ; i < byte_size ; i++){
            result[byte_size - 1 - i] = (byte)((value >> (8 * i)) & 0xFF);
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

    // input record들 중 포인터가 정해지지 않은 record의 포인터 결정 함수
    // 현재 record가 바로 다음 record의 주소를 가리킴.
    // records: 입력 레코드 리스트
    // pointers: 호출한 시점에서 정해진 레코드들의 포인터 리스트
    // offset: pointers에 n개의 주소가 있다고 할 때, n+1번째 record의 주소
    public List<byte[]> determine_pointers(List<Record> records, List<byte[]> pointers, int offset, int[] field_lengths){
        // 포인터가 모두 결정되어 있으면 포인터 값 그대로 리턴
        if(records.size() == pointers.size()) return pointers;
;
        int now_offset = offset;
        for(int i = 0 ; i < records.size() ; i++){
            // pointer가 정해진 record는 건너뛰기
            if(i < pointers.size()) continue;
            if(i == records.size() - 1 && records.size() != pointers.size()) {
                pointers.add(IntToByte(0, Global_Variables.pointer_bytes));
            }

            Record record = records.get(i);
            int record_length = get_record_length(record, field_lengths);

            int next_offset;

            // 블록 안에 record가 들어가는 경우 = 현재 레코드 시작 주소 + 현재 레코드 길이
            if(now_offset % Global_Variables.Block_Size < (now_offset + record_length) % Global_Variables.Block_Size) next_offset = now_offset + record_length;
            // 블록을 넘어가는 경우 = 다음 블록의 첫 주소
            else {
                next_offset = ((now_offset / Global_Variables.Block_Size) + 1) * Global_Variables.Block_Size;
                System.out.println(next_offset);
            }
            pointers.add(IntToByte(next_offset, Global_Variables.pointer_bytes));

            now_offset = next_offset;
        }
        // 마지막 record의 s_k가 file에서 가장 큰 s_k를 가진 record보다 크다면, 마지막 record의 pointer는 0으로 설정
        if(pointers.size() != records.size()) pointers.add(IntToByte(0, Global_Variables.pointer_bytes));

        return pointers;
    }

    // record의 위치를 이용해 해당 record의 search key를 반환
    public byte[] find_search_key(byte[] block, int block_offset, int search_key_size){
        byte[] ret_value = new byte[search_key_size];
        System.arraycopy(block, block_offset + Global_Variables.bitmap_bytes, ret_value, 0, search_key_size);
        return ret_value;
    }

    // record가 file의 record보다 search key가 작은 경우, input record들이 연속적으로 file의 search key보다 작은 것을 대비하는 함수
    // 연속적으로 작다면, input record가 연속적으로 pointer를 갖게 되고, 마지막으로 작은 record가 file의 record를 가리키게 됨.
    public List<byte[]> take_less_records(List<Record> records, int record_num, byte[] current_record_search_key, int offset, int[] field_lengths){
        List<byte[]> ret_value = new ArrayList<>();

        for(int i = record_num + 1; i < records.size(); i++){
            Record record = records.get(i);
            if(Arrays.compare(record.getFields().getFirst(), current_record_search_key) <= 0){
                ret_value.add(IntToByte(offset, Global_Variables.pointer_bytes)); // 자신의 주소를 넣음: 이전 record의 pointer가 됨
            }
            else break;
        }
        return ret_value;
    }

    // 주어진 input record들의 next record pointer를 지정하는 함수 //
    // block 단위로 file을 읽으며, record가 들어올 자리가 있으면 file 내부의 record pointer도 변경 //
    // file의 가장 마지막 record는 0을 기록하여 다음 record가 없음을 나타낸다 //
    public List<byte[]> find_next_pointers(List<Record> records, String path, int[] field_lengths) {
        if (records.isEmpty()) return null;
        List<byte[]> pointers = new ArrayList<>();
        int search_key_size = field_lengths[0];

        List<byte[]> blocks = new ArrayList<>();

        try{
            RandomAccessFile file = new RandomAccessFile(path, "rw");
            // blocks 0 : 헤더블록     blocks 1 ~ blocks n : 레코드를 가진 블록
            int n_th_block = 0;                                              // loop가 끝나면 n_th_block에는 총 block 개수가 저장됨
            for(n_th_block = 0 ; ; n_th_block++){
                file.seek(n_th_block * Global_Variables.Block_Size);    // n번째 블록 offset 설정
                byte[] block = new byte[Global_Variables.Block_Size];        // n번째 블록 가져오기
                if(file.read(block) == -1) break;

                blocks.add(block);
            }

            // 헤더블록 하나만 존재하는 경우, header block의 pointer를 input record의 첫 record로 할당
            // 헤더블록을 disk에 write하고, input record들은 각자 연속적으로 pointer를 가지도록 return
            if(blocks.size() == 1){
                System.arraycopy(IntToByte(Global_Variables.Block_Size, Global_Variables.pointer_bytes), 0, blocks.getFirst(), 0, Global_Variables.pointer_bytes);
                write(blocks.getFirst(), path, 0);
                return determine_pointers(records, pointers, Global_Variables.Block_Size * n_th_block, field_lengths);
            }

            byte[] header_block_pointer = new byte[Global_Variables.pointer_bytes];
            System.arraycopy(blocks.getFirst(), 0, header_block_pointer, 0, Global_Variables.pointer_bytes);

            // 마지막 block 바로 다음 첫 번째 byte로 시작점 설정
            int current_record_offset = ByteToInt(header_block_pointer);
            int before_record_offset = 0;
            int my_record_offset = Global_Variables.Block_Size * n_th_block;

            int cur_record_block_num = 0;        // 레코드가 들어간 블록 번호
            int cur_record_block_offset = 0;     // 레코드가 블록 내에서 갖는 논리적 주소 (0 ~ block_size - 1)
            int bef_record_block_num = 0;
            int bef_record_block_offset = 0;

            int my_record_num = 0;
            // 각 record마다 자신이 들어갈 위치 찾기
            while(my_record_num < records.size()){
                Record record = records.get(my_record_num);
                // file의 record를 순서대로, 마지막 record까지 탐색 (마지막 record의 pointer offset == 0)
                while(current_record_offset != 0){
                    cur_record_block_num = current_record_offset / Global_Variables.Block_Size;        // 레코드가 들어간 블록 번호
                    cur_record_block_offset = current_record_offset % Global_Variables.Block_Size;     // 레코드가 블록 내에서 갖는 논리적 주소 (0 ~ block_size - 1)
                    bef_record_block_num = before_record_offset / Global_Variables.Block_Size;
                    bef_record_block_offset = before_record_offset % Global_Variables.Block_Size;

                    byte[] my_record_search_key = record.getFields().getFirst();
                    byte[] current_record_search_key = find_search_key(blocks.get(cur_record_block_num), cur_record_block_offset, search_key_size);

                    // my record의 search key가 탐색하는 record의 search key보다 작은 경우
                    if(Arrays.compare(my_record_search_key, current_record_search_key) <= 0){
                        byte[] my_record_offset_byte = IntToByte(my_record_offset, Global_Variables.pointer_bytes);

                        byte[] before_block = blocks.get(bef_record_block_num);
                        byte[] before_record_bitmap = new byte[Global_Variables.bitmap_bytes];
                        System.arraycopy(before_block, bef_record_block_offset, before_record_bitmap, 0, Global_Variables.bitmap_bytes);
                        int before_record_length = get_record_length(before_record_bitmap, field_lengths);
                        // search key가 마지막으로 작았던 file record의 pointer를 my record의 주소로 업데이트
                        if(bef_record_block_num == 0 && bef_record_block_offset == 0) System.arraycopy(my_record_offset_byte, 0, blocks.getFirst(), 0, Global_Variables.pointer_bytes);
                        else System.arraycopy(my_record_offset_byte, 0, blocks.get(bef_record_block_num), bef_record_block_offset + before_record_length - Global_Variables.pointer_bytes, Global_Variables.pointer_bytes);

                        // input record들 중에서 현재 file record보다 search key가 작은 모든 record를 찾아 pointers에 추가
                        List<byte[]> less_records = take_less_records(records, my_record_num, current_record_search_key, my_record_offset, field_lengths);
                        less_records.add(IntToByte(current_record_offset, Global_Variables.pointer_bytes));
                        pointers.addAll(less_records);

                        // 업데이트할 record 번호는 채워진 pointer 배열의 원소 개수와 같음
                        for(int k = 0 ; k < pointers.size() - my_record_num - 1 ; k++){
                            my_record_num++;
                            if(my_record_num >= records.size()) break;
                            record = records.get(my_record_num);
                            my_record_offset += get_record_length(record, field_lengths);
                        }
                    }

                    // before record offset, current record offset 업데이트
                    before_record_offset = current_record_offset;

                    byte[] current_block = blocks.get(cur_record_block_num);
                    byte[] current_record_bitmap = new byte[Global_Variables.bitmap_bytes];
                    System.arraycopy(current_block, cur_record_block_offset, current_record_bitmap, 0, Global_Variables.bitmap_bytes);
                    int current_record_length = get_record_length(current_record_bitmap, field_lengths);
                    int current_record_pointer_offset = current_record_offset + current_record_length - Global_Variables.pointer_bytes;

                    byte[] next_record_offset = new byte[Global_Variables.pointer_bytes];
                    System.arraycopy(current_block, current_record_pointer_offset % Global_Variables.Block_Size, next_record_offset, 0, Global_Variables.pointer_bytes);
                    current_record_offset = ByteToInt(next_record_offset);
                }
                my_record_num++;
                my_record_offset += get_record_length(record, field_lengths);
            }

            pointers = determine_pointers(records, pointers, Global_Variables.Block_Size * n_th_block, field_lengths);

            // block들 파일에 쓰기
            for(int i = 0 ; i < n_th_block ; i++){
                write(blocks.get(i), path, Global_Variables.Block_Size * i);
            }
        }
        catch (IOException e){
            System.out.println("IOException 발생");
            e.printStackTrace();
        }

        return pointers;
    }
}
