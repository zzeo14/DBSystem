import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQL_Manager {
    public void execute(String query, String type){
        //System.out.println(query);

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
                case "find field": {
                    break;
                }
                case "find record": {
                    break;
                }
                default:{
                    System.out.println("sql query 에러");
                }
            }
        }
        catch (SQLException e){
            System.out.println("sql query 에러");
            e.printStackTrace();
        }
    }
}
