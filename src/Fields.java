public class Fields {
    private byte[] field_name = new byte[8];
    private byte[] field_type = new byte[8];
    private byte[] field_num = new byte[4];
    private byte[] field_size = new byte[4];
    private byte[] field_order = new byte[4];

    public byte[] getField_name() {
        return field_name;
    }
    public byte[] getField_type() {
        return field_type;
    }
    public byte[] getField_num() {
        return field_num;
    }
    public byte[] getField_size() {
        return field_size;
    }
    public byte[] getField_order() {
        return field_order;
    }

    public void setField_name(byte[] field_name) {
        this.field_name = field_name;
    }
    public void setField_type(byte[] field_type) {
        this.field_type = field_type;
    }
    public void setField_num(byte[] field_num) {
        this.field_num = field_num;
    }
    public void setField_size(byte[] field_size) {
        this.field_size = field_size;
    }
    public void setField_order(byte[] field_order) {
        this.field_order = field_order;
    }
}
