import java.util.ArrayList;
import java.util.List;

class Global_Variables {
    public static final int Block_Size = 128;
    public static final int bitmap_bytes = 1;
    public static final int field_num_bytes = 1;
    public static final int pointer_bytes = 4;
    public static final int field_name_bytes = 16;
    public static final int field_type_bytes = 8;
    public static final int field_order_bytes = 1;
    public static final int Block_number_bytes = 4;
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
    //private byte max_len = 0;

    public void AddField(Fields field){
        fields.add(field);
    }
    public void AddFields(List<Fields> fields){ this.fields.addAll(fields); }

    public void setField_num(int field_num) { this.field_num = (byte)field_num; }
    public byte getField_num() { return field_num; }

    //public void setMax_len(byte max_len) { this.max_len = max_len; }
    //public byte getMax_len() { return max_len; }

    public List<Fields> getFields(){
        return fields;
    }
}

class Record implements Comparable<Record>{
    private byte[] bitmap = new byte[Global_Variables.bitmap_bytes];
    private List<byte[]> fields = new ArrayList<>();
    private int next_pointer;

    public byte[] getBitmap() { return bitmap; }
    public List<byte[]> getFields() { return fields; }
    public int getNext_pointer() { return next_pointer; }

    public void setBitmap(byte[] bitmap) { this.bitmap = bitmap; }
    public void addField(byte[] field) { this.fields.add(field); }
    public void setFields(List<byte[]> fields) { this.fields = fields; }
    public void setNext_pointer(int next_pointer) { this.next_pointer = next_pointer; }

    @Override
    public int compareTo(Record other_record){
        for(int i = 0 ; i < fields.get(0).length; i++){
            if(this.fields.get(0)[i] == other_record.fields.get(0)[i]) continue;
            else return this.fields.get(0)[i] - other_record.fields.get(0)[i];
        }
        return 1;
    }
}

class Header_Content {
    private int field_num;
    private int block_number;
    private List<String> field_names = new ArrayList<>();
    private int[] field_lengths;
    private byte[] field_orders;
    private int first_record_offset;

    public void SetFieldNum(int field_num) { this.field_num = field_num; }
    public void SetField_names(List<String> field_names) { this.field_names = field_names; }
    public void SetField_lengths(int[] field_lengths) { this.field_lengths = field_lengths; }
    public void SetField_orders(byte[] field_orders) { this.field_orders = field_orders; }
    public void Setblock_number(int block_number) { this.block_number = block_number; }
    public void SetFirst_record_offset(int first_record_offset) {this.first_record_offset = first_record_offset; }

    public int getFieldNum() { return field_num; }
    public List<String> getFieldNames() { return field_names; }
    public int[] getFieldLengths() { return field_lengths; }
    public byte[] getFieldOrders() { return field_orders; }
    public int getBlock_number() { return block_number; }
    public int getFirst_record_offset() { return first_record_offset; }
}