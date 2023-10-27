package transposition;

import other.move.Move;

public class TranspositionTable {
    // TT with scheme 2-Deep generalized

    private Entry[] table;

    private final int keyLength;

    private static final int bucketLength = 128; //Maybe a bit overkill but it avoids collisions

    public TranspositionTable(int keyLength) {
        this.keyLength = keyLength;
        this.table = new Entry[1 << keyLength];
    }

    public void clear() {
        this.table = new Entry[1 << keyLength];
    }

    public TableData get(long hash) {
        Entry entry = table[(int) (hash >>> (64 - keyLength))];

        if (entry != null) {
            for (int i = 0; i < bucketLength; i++) {
                if (entry.data[i] != null && entry.data[i].hash == hash) {
                    return entry.data[i];
                }
            }
        }

        return null;

    }

    public void save(TableData data) {
        // We will use Two deep replacement scheme but generalized
        int index = (int) (data.hash >>> (64 - keyLength));
        Entry entry = table[index];

        if (entry == null) {
            entry = new Entry(new TableData[bucketLength]);
            entry.data[0] = data;
            table[index] = entry;
        } else {
            int indexWorstEntry = 0;
            for (int i = 0; i < bucketLength; i++) {

                if (entry.data[i] == null) {
                    entry.data[i] = data;
                    break;
                }
                if (entry.data[i].hash == data.hash) {
                    // Update
                    if (entry.data[i].depth < data.depth) {
                        // New entry is better -> replace
                        entry.data[i] = data;
                        break;
                    }
                    // New entry was worse -> done
                    break;
                }
                if (entry.data[i].depth < entry.data[indexWorstEntry].depth) {
                    indexWorstEntry = i;

                }

            }
            if (entry.data[indexWorstEntry].depth < data.depth) {
                // Replace the worst entry with the new (and better) one
                entry.data[indexWorstEntry] = data;
            }
        }


    }

}







