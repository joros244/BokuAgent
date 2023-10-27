package heuristics;

import game.types.board.SiteType;
import other.context.Context;
import other.state.container.ContainerState;
import other.topology.Cell;
import other.topology.TopologyElement;

import java.util.*;

public class BoardHeuristics {


    public int[][] getBoardScores() {
        return boardScores;
    }

    public final static HashMap<String, Float> parameters = new HashMap<>();

    static {
        parameters.put("adjacencyBonus", 4f);
        parameters.put("adjacencyBonusOpponent", 2f);
        parameters.put("lineBonus", 5f);
        parameters.put("lineBonusOpponent", 0f);
        parameters.put("capturingBonus", 1f);
    }

    public void setBoardScores(int[][] boardScores) {
        this.boardScores = boardScores;
    }

    private int[][] boardScores;

    public BoardHeuristics() {
        // Board base scores given from the degree of freedom of each cell scaled
        this.boardScores = new int[][]{{0, 1, 1, 1, 1, 0, 1, 3, 4, 4, 4, 3, 1, 1, 4, 6, 7, 7, 6, 3, 1, 1, 4, 7, 9, 10, 9,
                6, 3, 1, 1, 4, 7, 10, 12, 12, 9, 6, 3, 0, 0, 3, 6, 9, 12, 12, 10, 7, 4, 1, 1, 3, 6, 9, 10, 9, 7, 4, 1, 1,
                3, 6, 7, 7, 6, 4, 1, 1, 3, 4, 4, 4, 3, 1, 0, 1, 1, 1, 1, 0}, {0, 1, 1, 1, 1, 0, 1, 3, 4, 4, 4, 3, 1, 1, 4,
                6, 7, 7, 6, 3, 1, 1, 4, 7, 9, 10, 9, 6, 3, 1, 1, 4, 7, 10, 12, 12, 9, 6, 3, 0, 0, 3, 6, 9, 12, 12, 10, 7,
                4, 1, 1, 3, 6, 9, 10, 9, 7, 4, 1, 1, 3, 6, 7, 7, 6, 4, 1, 1, 3, 4, 4, 4, 3, 1, 0, 1, 1, 1, 1, 0}};
    }

    public BoardHeuristics(BoardHeuristics boardHeuristics) {
        int[][] copyBoardSc = new int[2][];
        copyBoardSc[0] = Arrays.copyOf(boardHeuristics.getBoardScores()[0], 80);
        copyBoardSc[1] = Arrays.copyOf(boardHeuristics.getBoardScores()[1], 80);
        this.boardScores = copyBoardSc;

    }


    public void updateBoardAndScores(Context context, int site, String action, int[][] boardSc, int[] scores, int playerToMove) {
        // We will update the scores (with last move made) and we will update the board for the subsequent moves
        List<Cell> cells = context.board().topology().cells();
        int opponent = playerToMove == 1 ? 2 : 1;
        ContainerState containerState = context.state().containerStates()[0];


        if (action.equals("Add")) {
            // Last move was an Add move, so we increase our score with the value of the cell
            scores[playerToMove - 1] += boardSc[playerToMove - 1][site];
            Cell placed = cells.get(site);

            // We will update the values of adjacent cells, and the cells in the same row, column and diagonal

            //Adjacent
            List<Cell> adjacent = cells.get(site).adjacent();
            for (Cell cell : adjacent) {
                boardSc[playerToMove - 1][cell.index()] += parameters.get("adjacencyBonus");
                boardSc[opponent - 1][cell.index()] += parameters.get("adjacencyBonusOpponent");
            }

            //Row
            List<TopologyElement> row = context.topology().rows(SiteType.Cell).get(placed.row());
            updateRegion(row, playerToMove, opponent, boardSc, containerState);

            //Column
            List<TopologyElement> column = context.topology().columns(SiteType.Cell).get(placed.col());
            updateRegion(column, playerToMove, opponent, boardSc, containerState);

            //Diagonal

            //Compute diagonal
            List<TopologyElement> diagonal = new ArrayList<>();
            int r = placed.row();
            int c = placed.col();
            int min = Math.min(r, c);
            int lim = 10 - Math.abs(r - c);
            for (int k = min; k > min - lim; k--) {
                int a1 = r - k;
                int b1 = c - k;
                Optional<Cell> mycell = cells.stream().filter(a -> a.matchCoord(a1, b1)).findFirst();
                diagonal.add(mycell.get());

            }
            updateRegion(diagonal, playerToMove, opponent, boardSc, containerState);

        } else {
            scores[opponent - 1] -= (int) (parameters.get("capturingBonus") * boardSc[opponent - 1][site]);
        }

    }

    private void updateRegion(List<TopologyElement> region, int playerToMove, int opponent, int[][] boardSc, ContainerState containerState) {
        int count = 0;
        List<Integer> positions = new ArrayList<>();

        int size = region.size();
        int nextIndex;
        int prevIndex;

        for (int k = 0; k < size; k++) {
            Cell toCheck = (Cell) region.get(k);
            int index = toCheck.index();

            if (containerState.whoCell(index) == playerToMove) {
                count++;
                positions.add(index);
                if (k != 0 && k != size - 1) {
                    nextIndex = region.get(k + 1).index();
                    prevIndex = region.get(k - 1).index();
                    if (containerState.whoCell(prevIndex) != playerToMove) {
                        // We don't own the previous cell, so we will be interested in it.
                        positions.add(prevIndex);
                    }
                    if (containerState.whoCell(nextIndex) != playerToMove) {
                        // We don't own the next cell, so we will be interested in it.
                        positions.add(nextIndex);
                    }
                }
                if (k == size - 1) {
                    prevIndex = region.get(k - 1).index();
                    if (containerState.whoCell(prevIndex) != playerToMove) {
                        positions.add(prevIndex);
                    }

                }
                if (k == 0) {
                    nextIndex = region.get(k + 1).index();

                    if (containerState.whoCell(nextIndex) != playerToMove) {
                        positions.add(nextIndex);
                    }
                }
            } else if (containerState.whoCell(index) == opponent) {
                // If opponent has the position then the line is
                // broken
                count = 0;

            }
        }
        if (count > 1) {
            for (Integer posi : positions) {
                if (containerState.whoCell(posi) == playerToMove) {
                    // We own the cell, and it's inside a line so its value gets updated
                    boardSc[playerToMove - 1][posi] = (int) Math.pow(parameters.get("lineBonus"), (count));
                } else {
                    // We don't own the cell, and it's interesting to us, so we update its value
                    boardSc[playerToMove - 1][posi] += (int) (parameters.get("lineBonus") * (count - 1));
                }

                // If an empty cell is interesting to us, then it's potentially interesting for our opponent
                boardSc[opponent - 1][posi] += (int) (parameters.get("lineBonusOpponent") * (count - 1));
            }
        }

    }
}
