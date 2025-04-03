import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class File_Manager {
    public void create_file(String path, String file_name, Metadata metadata) {
        try {
            File file = new File(path);

            if (file.createNewFile()) {
                FileWriter fileWriter = new FileWriter(file);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

                bufferedWriter.write("Hello World");
                bufferedWriter.newLine();
                bufferedWriter.write("My name is Zeo");
                bufferedWriter.close();
            } else {
                System.out.println("File already exists!");
            }
        }
        catch (IOException e){
            System.out.println(e);
        }
    }
}
