package heuristics;

import evaluation.EvaluationFunction;
import other.context.Context;
import other.move.Move;

import java.util.ArrayList;
import java.util.List;

public class Heuristics {
    private BoardHeuristics boardHeuristics;
    private EvaluationFunction evaluationFunction;

    public Heuristics(BoardHeuristics boardHeuristics, EvaluationFunction evaluationFunction) {
        this.boardHeuristics = boardHeuristics;
        this.evaluationFunction = evaluationFunction;
    }

    public Heuristics(Heuristics heuristics) {

        this.boardHeuristics = new BoardHeuristics(heuristics.getBoardHeuristics());
        this.evaluationFunction = heuristics.getEvaluationFunction().copyEvaluationFunction();

    }

    public BoardHeuristics getBoardHeuristics() {
        return boardHeuristics;
    }

    public void setBoardHeuristics(BoardHeuristics boardHeuristics) {
        this.boardHeuristics = boardHeuristics;
    }

    public EvaluationFunction getEvaluationFunction() {
        return evaluationFunction;
    }

    public void setEvaluationFunction(EvaluationFunction evaluationFunction) {
        this.evaluationFunction = evaluationFunction;
    }

    public List<Move> getLegalMovesSorted(Context context) {
        // Potential moves are sorted based on their cell value
        int currentPlayerToMove = context.state().mover();

        ArrayList<Move> moveList = new ArrayList<>();
        context.game().moves(context).moves().forEach(moveList::add);

        moveList.sort((a, b) -> {
            int index1 = this.boardHeuristics.getBoardScores()[currentPlayerToMove - 1][a.getToLocation().site()];
            int index2 = this.boardHeuristics.getBoardScores()[currentPlayerToMove - 1][b.getToLocation().site()];
            return Integer.compare(index1, index2);
        });

        return moveList;


    }

    public void rebuildHeuristics(Context context) {
        // Rebuild boardHeuristics and evaluationFunction from the moves played so far. (Allows undo)
        boardHeuristics = new BoardHeuristics();
        evaluationFunction.setScores(new int[]{0, 0});


        if (context.trial().numMoves() > 0) {
            int numMoves = context.trial().numMoves();
            Move m = null;
            for (int i = 0; i < numMoves; i++) {
                m = context.trial().getMove(i);
                boardHeuristics.updateBoardAndScores(context, m.getToLocation().site(), m.actionType().name(),
                        boardHeuristics.getBoardScores(), evaluationFunction.getScores(), m.mover());

            }

        }

    }

    public void updateHeuristics(Context context, Move m, int currentPlayerToMove) {
        // Update with last move
        boardHeuristics.updateBoardAndScores(context, m.getToLocation().site(), m.actionType().name(),
                boardHeuristics.getBoardScores(), evaluationFunction.getScores(), currentPlayerToMove);

    }
}
