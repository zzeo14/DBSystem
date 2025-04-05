import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class Block {
    private Record[] records;
}

class Header_Block {
    private int first_block;
    private Metadata metadata;
}

class Fields {
    private byte[] field_name = new byte[16];
    private byte[] field_type = new byte[8];
    private byte field_order;

    public Fields(){}

    public Fields(String field_name, String field_type, int field_order) {
        this.field_name = field_name.getBytes();
        this.field_type = field_type.getBytes();
        this.field_order = (byte)field_order;
    }

    public byte[] getField_name() {
        return field_name;
    }
    public byte[] getField_type() {
        return field_type;
    }
    public byte getField_order() { return field_order; }

    public void setField_name(String field_name) {
        this.field_name = field_name.getBytes();
    }
    public void setField_type(String field_type) { this.field_type = field_type.getBytes(); }
    public void setField_order(int field_order) { this.field_order = (byte)field_order; }
}

class Metadata {
    private List<Fields> fields = new ArrayList<>();
    private byte field_num;

    public void AddField(Fields field){
        fields.add(field);
    }
    public void AddFields(List<Fields> fields){ this.fields.addAll(fields); }

    public void setField_num(int field_num) { this.field_num = (byte)field_num; }
    public byte getField_num() { return field_num; }

    public List<Fields> getFields(){
        return fields;
    }
}

class Record {
    private byte bitmap;
    private byte[][] fields;
    private int next_pointer;

    public Record(byte bitmap, byte[][] fields, int next_pointer, int field_num){
        this.bitmap = bitmap;
        this.fields = fields;
        this.next_pointer = next_pointer;
    }
}
