import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                byte[] header = new byte[Global_Variables.Block_Size];
                file.seek(0);
                file.read(header);

                // block size를 byte배열로 변환
                // header block이 0 ~ block_size - 1까지이므로,
                // first block은 block_size offset부터 시작한다.
                byte[] first_block_pointer = new byte[Global_Variables.pointer_bytes];
                for(int i = 0; i < Global_Variables.pointer_bytes; i++){
                    first_block_pointer[i] = (byte)(Global_Variables.Block_Size >>> (8 * (3-i)));
                    header[i] = first_block_pointer[i];
                }

                file.seek(Global_Variables.Block_Size);
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
        byte[] ret_bytes = new byte[Global_Variables.Block_Size];
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
        byte[] header_block = new byte[Global_Variables.Block_Size];
        try{
            RandomAccessFile file = new RandomAccessFile(path, "r");

            file.seek(0);
            file.read(header_block);

            for(int i = 0 ; i < Global_Variables.pointer_bytes ; i++){
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

    // 현재 record의 길이를 찾는 method             //
    // record: 길이를 찾을 record                 //
    // field_lengths : file의 column들의 길이 배열 //
    public int get_record_length(Record record, int[] field_lengths) {
        byte bitmap = record.getBitmap();
        int length = 0;
        for(int i = 0 ; i < field_lengths.length ; i++){
            if((bitmap & (byte)(1 << (Global_Variables.bitmap_bytes * 8 - 1 - i))) == 0){
                length += field_lengths[i];
            }
        }
        return length;
    }

    // integer를 byte 배열로 변환
    public byte[] IntToByte(int value, int byte_size) {
        byte[] return_byte = new byte[byte_size];
        for(int i = 0 ; i < byte_size ; i++){
            return_byte[i] = (byte)((value >> (8 * i)) & 0xFF);
        }
        return return_byte;
    }

    // byte 배열을 integer로 변환
    public int ByteToInt(byte[] bytes) {
        int result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result |= ((bytes[i] & 0xFF) << (8 * i));
        }
        return result;
    }

    // 주어진 input record들의 next record pointer를 지정하는 함수 //
    // block 단위로 file을 읽으며, record가 들어올 자리가 있으면 file 내부의 record pointer도 변경 //
    // file의 가장 마지막 record는 0을 기록하여 다음 record가 없음을 나타낸다 //
    public List<byte[]> find_next_pointer(List<Record> records, String path, int[] field_lengths) {
        List<byte[]> pointers = new ArrayList<>();
        int search_key_size = field_lengths[0];

        byte[] block = new byte[Global_Variables.Block_Size];
        byte[] current_search_key = new byte[search_key_size];
        byte[] before_search_key = new byte[search_key_size];

        int now_record_offset = 0;
        int next_record_offset = 0;

        int last_offset = 0;
        int offset = 0;

        try{
            RandomAccessFile file = new RandomAccessFile(path, "rw");
            int n_th_block = 1;
            for(n_th_block = 1 ; ; n_th_block ++){  // 0은 헤더블록이므로 제외
                file.seek(n_th_block * Global_Variables.Block_Size); // n번째 block 가져오기
                if(file.read(block) == -1) break;

                byte bitmap = block[offset];
                offset += Global_Variables.bitmap_bytes;
                if(offset == Global_Variables.bitmap_bytes && n_th_block == 1) { // before record가 없는 경우에는 record 가져오기까지만 하기
                    System.arraycopy(block, offset, current_search_key, 0, search_key_size); // 해당 record의 search key 가져오기
                    continue;
                }
                System.arraycopy(current_search_key, 0, before_search_key, 0, search_key_size);     // 현재 record search key를 before로 저장
                System.arraycopy(block, offset, current_search_key, 0, search_key_size);                  // 해당 record의 search key 가져오기

                // 각 record를 읽으면서 record가 들어갈 자리인지 확인
                for(int n_th_record = 0 ; n_th_record < records.size() ; n_th_record++){
                    Record record = records.get(n_th_record); // n번째 record 가져오기
                    byte[] field = record.getFields().getFirst();

                    // 내 record가 들어갈 자리라면
                    // TODO 1: 내 다음 record의 search key가 file의 search key보다 작으면 내 record의 포인터는 내 다음 record
                    // TODO 2: 내 다음 record가 없거나, 내 다음 record의 search key가 file의 search key보다 크면 내 record의 포인터는 file의 current record
                    if(Arrays.compare(field, before_search_key) > 0 && Arrays.compare(field, current_search_key) <= 0){
                        if(record != records.getLast() && Arrays.compare(records.get(n_th_record + 1).getFields().getFirst(), current_search_key) <= 0){
                            // TODO 1
                        }
                        else{
                            // TODO 2
                        }
                    }
                    else continue;
                }
            }

            // block을 다 읽은 후, 아직 포인터가 명시되지 않은 record들은 input의 바로 다음 record를 가리킴.
            // 해당 record offset에서 record 길이만큼 더한 것이 다음 record의 주소가 됨.
            for(int n_th_record = 0 ; n_th_record < records.size() ; n_th_record++){
                Record record = records.get(n_th_record);
                // record의 pointer가 지정됐거나, 마지막 record일 경우 다음 record로 넘어감
                if(record.getNext_pointer() == 0 && n_th_record != records.size() - 1){
                    // 1 : file에 record가 하나도 없는 경우
                    // 2 : 입력한 record들 중 가장 search key가 작은 record의 search key가, file에서 search key가 가장 큰 record의 search key보다 큰 경우
                    if(n_th_record == 0){
                        last_offset = n_th_block * Global_Variables.Block_Size + get_record_length(record, field_lengths);
                        pointers.add(IntToByte(last_offset, Global_Variables.pointer_bytes));
                    }
                    // record들 중 몇 개는 pointer가 명시되고, 몇 개는 값이 0인 경우
                    // 현재 record의 offset에서 현재 record의 길이만큼 더하여 offset을 결정한다.
                    else {
                        last_offset += get_record_length(record, field_lengths);
                        pointers.add(IntToByte(last_offset, Global_Variables.pointer_bytes));
                    }
                }
            }
        }
        catch (IOException e){
            System.out.println("IOException 발생");
            e.printStackTrace();
        }

        return pointers;
    }
}
