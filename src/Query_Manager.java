import java.io.*;
import java.util.Scanner;
import java.io.BufferedReader;

public class Query_Manager {
    public static String query(BufferedReader br) throws IOException {
        Scanner sc = new Scanner(System.in);
        String line;
        String sql_query = "";
        String first_column = "";

        if((line = br.readLine()) != null){
            if(line.equalsIgnoreCase("create table")){ // table 선언문인지
                sql_query = "create table";
                if((line = br.readLine()) != null) {
                    String table_name = line; // table 이름값 저장
                    sql_query += " " + table_name + "("; // mysql query update
                    if ((line = br.readLine()) != null) {
                        if(line.length() == 1 && line.charAt(0) >= '0' && line.charAt(0) <= '9') { // 정확히 숫자 하나만 있어야 함
                            int iteration = line.charAt(0) - '0';
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
                                String column = query[0];
                                String type = query[1];
                                if(i == 0) first_column = column;

                                if(i < iteration - 1 && type.charAt(type.length() - 1) != ',') {
                                    inv_q();
                                    break;
                                }

                                sql_query += column + " " + type;
                            }
                            if((line = br.readLine()) != null){
                                String[] pk = line.split("\\s+");
                                if (!pk[0].equalsIgnoreCase("s_k")){
                                    inv_q();
                                }
                                else{
                                    sql_query += "primary key (" + pk[1].substring(1, pk[1].length() - 1) + "));";
                                    //return sql_query;
                                }
                            }
                            else { // 첫 번째 column을 자동으로 search key로 설정
                                sql_query += "primary key (" + first_column + "));";
                            }

                            System.out.print("\nEnter new file path >>");
                            String file_path = sc.nextLine();

                            String path;
                            if(file_path.charAt(file_path.length() - 1) == '/'){
                                path = file_path + table_name + "txt";
                            }
                            else {
                                path = file_path + "/" + table_name + ".txt";
                            }
                            File file = new File(path);

                            if(file.createNewFile()){
                                FileWriter fileWriter = new FileWriter(file);
                                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

                                bufferedWriter.write(sql_query);
                                bufferedWriter.newLine();
                            }
                            else {
                                System.out.println("File already exists!");
                            }
                        }
                        else inv_q();
                    }
                    else inv_q();
                }
                else inv_q();
            }
            else if(line.equalsIgnoreCase("insert into")){

            }
            else if(line.equalsIgnoreCase("find record")){

            }
            else if(line.equalsIgnoreCase("find field")){

            }
            else inv_q();
        }
        else inv_q();
        return sql_query;
    }

    private static void inv_q(){
        System.out.println("Invalid Query");
    }
}
