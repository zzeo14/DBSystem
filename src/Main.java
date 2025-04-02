import java.io.BufferedReader;
import java.util.Scanner;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        System.out.println("This is the DBSystem program.");
        while(true) {
            System.out.println("\n<Menu>");
            System.out.println("1 : Query");
            System.out.println("2 : Quit");

            System.out.print("Select Option 1 or 2 >> ");
            Scanner sc = new Scanner(System.in);
            String option = sc.nextLine();

            switch (option) {
                case "1":
                    System.out.print("\nEnter Query path >> ");
                    String path = sc.nextLine();
                    File query_file = new File(path);

                    if (!query_file.exists()) {
                        System.out.println("File Not Found");
                        continue;
                    }

                    try(FileReader fr = new FileReader(query_file)){
                        BufferedReader br = new BufferedReader(fr);

                        String sql_query = Query_Manager.query(br);
                        System.out.println(sql_query);
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }
                    break;
                case "2":
                    System.out.println("Terminate DB System.");
                    return;
                default:
                    System.out.println("Invalid Option. Please select 1 or 2");
            }
        }
    }
}