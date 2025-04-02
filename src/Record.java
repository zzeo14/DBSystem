public class Record {
    private byte bitmap;
    private byte[][] fields;
    private int pointer;

    public Record(byte bitmap, byte[][] fields, int pointer, int field_num){
        this.bitmap = bitmap;
        this.fields = fields;
        this.pointer = pointer;
    }
}
