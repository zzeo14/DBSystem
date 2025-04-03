import java.util.List;
import java.util.ArrayList;

public class Metadata {
    private List<Fields> fields = new ArrayList<>();

    public void AddField(Fields field){
        fields.add(field);
    }
    public void AddFields(List<Fields> fields){
        this.fields.addAll(fields);
    }

    public List<Fields> GetFields(){
        return fields;
    }
}