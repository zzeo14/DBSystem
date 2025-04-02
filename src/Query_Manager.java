import java.io.BufferedReader;
import java.io.IOException;

public class Query_Manager {
    public static void query(BufferedReader br, String sql_query) throws IOException {
        String line;

        if((line = br.readLine()) != null){
            if(line.equalsIgnoreCase("create table")){ // table 선언문인지 
                sql_query += "create table";
                if((line = br.readLine()) != null) {
                    String table_name = line; // table 이름값 저장
                    sql_query += " " + table_name + "(";
                    if ((line = br.readLine()) != null) {
                        if(line.length() == 1 && line.charAt(0) >= '0' && line.charAt(0) <= '9') { // 정확히 숫자 하나만 있어야 함
                            for(int i = 0 ; i < line.charAt(0) - '0' ; i++){
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

                                sql_query += column + " " + type;
                            }
                            if((line = br.readLine()) != null){
                                String[] pk = line.split("\\s+");
                                if (!pk[0].equalsIgnoreCase("s_k")){
                                    inv_q();
                                }
                                else{
                                    
                                }
                            }
                            else { // 첫 번째 column을 자동으로 search key로 설정

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
    }

    private static void inv_q(){
        System.out.println("Invalid Query");
    }
}
