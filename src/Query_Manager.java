import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.BufferedReader;

public class Query_Manager {
    private MySQL_Manager sql_manager = new MySQL_Manager();

    public void query(BufferedReader br) throws IOException {
        Scanner sc = new Scanner(System.in);
        String line;
        String sql_query = "";
        String first_column = "";

        File_Manager file_manager = new File_Manager();

        if((line = br.readLine()) != null){
            // query문 parsing 진행 후 File_Manager에게 전달
            if(line.equalsIgnoreCase("create table")){ // table 선언문
                sql_query = "create table";
                Metadata metadata = new Metadata();

                if((line = br.readLine()) != null) {
                    String table_name = line; // table 이름값 저장
                    sql_query += " " + table_name + "("; // mysql query update
                    if ((line = br.readLine()) != null) {
                        if(line.length() == 1 && line.charAt(0) >= '0' && line.charAt(0) <= '9') { // 정확히 숫자 하나만 있어야 함
                            int iteration = line.charAt(0) - '0';
                            metadata.setField_num(iteration);
                            for(int i = 0 ; i < iteration ; i++){
                                line = br.readLine();
                                if(line == null) {
                                    inv_q();
                                    break;
                                }

                                String[] query = line.split("\\s+"); // column name과 type을 분리
                                if(query.length != 2) {
                                    inv_q();
                                    break;
                                }
                                String field_name = query[0];
                                String type = query[1];
                                if(i == 0) first_column = field_name; // primary key 자동설정

                                Fields field = new Fields(field_name, type, i + 1); // field 이름, 자료형, 순서가 저장된 field 변수 생성
                                metadata.AddField(field);

                                if(i < iteration - 1 && type.charAt(type.length() - 1) != ',') {
                                    inv_q();
                                    break;
                                }

                                if(i < iteration - 1) sql_query += field_name + " " + type;
                                else sql_query += field_name + " " + type.substring(0, type.length() - 1);
                            }
                            sql_query += ");";
                            file_manager.create_file(table_name, metadata);

                            sql_manager.execute(sql_query, "create");
                        } else inv_q();
                    } else inv_q();
                } else inv_q();
            }
            else if(line.equalsIgnoreCase("insert into")){ // record 삽입
                sql_query = "insert into";
                List<Record> records = new ArrayList<>();
                int pointer = 0;

                line = br.readLine();
                if(line == null) {
                    inv_q();
                    return;
                }
                String table_name = line;
                sql_query += " " + table_name + " values(";
                line = br.readLine();
                if(line == null) { inv_q(); return; }
                for(int i = 0 ; i < line.length() ; i++) {
                    if(line.charAt(i) < '0' || line.charAt(i) > '9') { inv_q(); return; }
                }
                int iteration = Integer.parseInt(line);
                for(int i = 0 ; i < iteration ; i++){ // 각 record에 대해 수행
                    Record record = new Record();
                    line = br.readLine();
                    if(line == null) { inv_q(); return; }
                    String temp_query = sql_query;

                    String[] query = line.split(";"); // 각 column을 semi colon으로 분리
                    List<byte[]> element = new ArrayList<>();

                    byte bitmap[] = new byte[Global_Variables.bitmap_bytes];
                    for(int j = 0 ; j < query.length ; j++) {
                        int byte_index = j / 8;
                        int bit_index = j % 8;
                        if(query[j].equalsIgnoreCase("null") || query[j].equalsIgnoreCase("null,")){ // null은 record에 field를 추가하지 않고 bitmap을 바꿈
                            bitmap[byte_index] |= (byte)(1 << (7 - bit_index));
                            temp_query += "null";
                        }
                        else {
                            if(query[j].charAt(query[j].length() - 1) != ',' ) temp_query += "'" + query[j] + "'";
                            else {
                                temp_query += "'" + query[j].substring(0, query[j].length() - 1) + "'";
                                query[j] = query[j].substring(0, query[j].length() - 1);    // 끝에 comma 빼기
                            }
                            element.add(query[j].getBytes());
                        }

                        if(j == query.length - 1) temp_query += ");";
                        else temp_query += ",";
                    }
                    record.setFields(element);
                    record.setBitmap(bitmap);

                    records.add(record);
                    sql_manager.execute(temp_query, "insert");
                }
                file_manager.insert_record(records, table_name);
            }
            else if (line.equalsIgnoreCase("join")) { // join 연산 수행
                sql_query = "select * from ";
                line = br.readLine();
                if(line == null) {inv_q(); return;}
                if(line.split(" ").length != 1) {inv_q(); return;}
                String first_file = line;
                sql_query += first_file; // first relation

                line = br.readLine();
                if(line == null) {inv_q(); return;}
                if(line.split(" ").length != 1) {inv_q(); return;}
                String second_file = line;
                sql_query += ", " + second_file + " where "; // second relation

                // 각 table의 header 가져오기
                Header_Content first_file_header = file_manager.read_header(first_file);
                Header_Content second_file_header = file_manager.read_header(second_file);
                if(first_file_header == null || second_file_header == null) {return;}

                // search key 가져오기
                String first_file_search_key = first_file_header.getFieldNames().getFirst();
                String second_file_search_key = second_file_header.getFieldNames().getFirst();

                // join 연산 수행
                file_manager.join_execute(first_file, first_file_header, second_file, second_file_header);

                // mysql 실행
                sql_query += first_file + "." + first_file_search_key + " = " + second_file + "." + second_file_search_key; // r.a = s.a
                sql_query += " order by " + first_file + "." + first_file_search_key; // order by r.a
                sql_manager.execute(sql_query, "join");
            }
            else {
                inv_q();
            }
        }
        else inv_q();
    }

    private static void inv_q(){
        System.out.println("Invalid Query");
    }

    private void print_bit(byte b){
        for (int i = 7; i >= 0; i--) {
            System.out.print((b >> i & 1));
        }
        System.out.println();
    }
}
