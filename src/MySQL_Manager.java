import java.sql.*;

public class MySQL_Manager {
    public void execute(String query, String type){
        System.out.println(query);

        // 기본값 설정 //
        String url = "jdbc:mysql://localhost:3306/DBSystem";
        String user = "root";
        String password = SQL_password.password; // 비밀번호 파일에서 불러옴

        Connection conn = null;
        Statement stmt = null;

        // connection, statement 할당 //
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(url, user, password);
            stmt = conn.createStatement();
            if (stmt == null){
                throw new SQLException("연결 오류 발생");
            }
        }
        catch (SQLException e){
            System.out.println("데이터베이스 연결오류");
            e.printStackTrace();
        }
        catch (ClassNotFoundException e){
            System.out.println("드라이버 연결 오류");
            e.printStackTrace();
        }

        // 해당하는 sql query 실행 //
        try{
            switch (type){
                case "create": {
                    stmt.executeUpdate(query);
                    break;
                }
                case "insert": {
                    stmt.executeUpdate(query);
                    break;
                }
                case "join": {
                    ResultSet rs = stmt.executeQuery(query);
                    ResultSetMetaData rsmd = rs.getMetaData();
                    int columnCount = rsmd.getColumnCount();
                    System.out.println("--------------------MySQL Result--------------------");

                    // column name 출력
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rsmd.getColumnName(i);
                        System.out.printf("%-25s", columnName);
                    }
                    System.out.println();

                    // tuple 출력
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            String value = rs.getString(i);
                            if (value == null) value = "NULL"; // NULL 처리
                            System.out.printf("%-25s", value);
                        }
                        System.out.println();
                    }
                    break;
                }
                default:{
                    System.out.println("sql query 에러");
                }
            }
        }
        catch (NullPointerException e){
            System.out.println("Null pointer exception 에러 발생");
        }
        catch (SQLException e){
            System.out.println("sql query 에러");
        }
    }
}
