package transposition;

import other.move.Move;

public class TableData {


    public float value;
    public TranspositionBound boundType;

    public Move bestMove;

    public int depth;

    public long hash;


    public TableData(float value, TranspositionBound boundType, Move bestMove, int depth, long hash) {
        this.value = value;
        this.boundType = boundType;
        this.bestMove = bestMove;
        this.depth = depth;
        this.hash = hash;
    }
}
