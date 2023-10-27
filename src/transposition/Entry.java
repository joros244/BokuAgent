package transposition;

public class Entry {
    // Will consider Two Deep replacement scheme,
    // but we can extend it to use a Bucket scheme

    public TableData[] data;

    public Entry(TableData[] data) {
        this.data = data;
    }
}