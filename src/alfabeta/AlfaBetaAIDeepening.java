package alfabeta;

import evaluation.EvaluationFunction;
import evaluation.EvaluationFunctionPlain;
import game.Game;
import heuristics.BoardHeuristics;
import heuristics.Heuristics;
import other.AI;
import other.context.Context;
import other.move.Move;
import transposition.TableData;
import transposition.TranspositionBound;
import transposition.TranspositionTable;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class AlfaBetaAIDeepening extends AI {
    protected int player = -1;

    private Heuristics heuristics;

    private int nodes;

    private long timer;
    final int C = 3;
    final int M = 10;
    final int R = 2;

    private TranspositionTable table;

    private Hashtable<Integer, Queue<Move>> killerMoves;

    public AlfaBetaAIDeepening() {
        this.friendlyName = "Iterative Deepening AlfaBeta";

    }

    @Override
    public void initAI(final Game game, final int playerID) {
        heuristics = new Heuristics(new BoardHeuristics(), new EvaluationFunctionPlain(new int[]{0, 0}));
        this.timer = (600 * 1000);
        this.player = playerID;
        nodes = 0;
        table = new TranspositionTable(12);
        killerMoves = new Hashtable<>();

    }


    public Move selectAction(final Game game, final Context context, final double maxSeconds, final int maxIterations, final int maxDepth) {

        nodes = 0;
        long startOfPlay = System.currentTimeMillis();
        Move lastReturnedMove = null;


        // Slow but allows undo
        heuristics.rebuildHeuristics(context);


        lastReturnedMove = iterativeDeepening(game, context, 15, Integer.MAX_VALUE);

        //Clear table
        table.clear();

        //Clear killer moves
        killerMoves.clear();

        long lengthofPlay = System.currentTimeMillis() - startOfPlay;


        timer -= lengthofPlay;
        System.out.println("Time remaining: " + ((timer) / 1000) / 60 + "m " + ((timer) / 1000) % 60 + "s ");
        System.out.println("Nodes: " + (int) nodes / 1000 + "K nodes");
        System.out.println("Scores: " + heuristics.getEvaluationFunction().getScores()[0] + " " + heuristics.getEvaluationFunction().getScores()[1]);


        System.out.println();

        return lastReturnedMove;

    }


    public Move iterativeDeepening
            (
                    final Game game,
                    final Context context,
                    final double maxSeconds,
                    final int maxDepth) {

        //Initialize the timer
        final long start = System.currentTimeMillis();
        long stopTime = start + (long) (maxSeconds * 1000);

        int currentPlayerToMove = context.state().mover();
        int opponent = currentPlayerToMove == 1 ? 2 : 1;


        // Get legal moves and sort them
        List<Move> moveList = heuristics.getLegalMovesSorted(context);
        int numMoves = moveList.size();

        // Storing scores and movements for move ordering
        HashMap<Move, Float> scoredMoves = new HashMap<>();

        int searchDepth = 1; // We will start at depth 1

        Move bestMoveGlobal = moveList.get(numMoves - 1);
        EvaluationFunction newEval = heuristics.getEvaluationFunction().copyEvaluationFunction();
        newEval.setScores(new int[]{0, 0});
        Heuristics newBestHeuristic = new Heuristics(new BoardHeuristics(), newEval);
        int nonLoosingMoves = 0;

        while (searchDepth < maxDepth) {
            System.out.println("DEPTH: " + searchDepth);


            float score = Float.NEGATIVE_INFINITY;
            float alpha = Float.NEGATIVE_INFINITY;
            final float beta = Float.POSITIVE_INFINITY;

            // best move during this particular search
            Move bestMove = moveList.get(numMoves - 1);


            for (int i = numMoves - 1; i > -1; --i) {
                nodes += 1;
                // We are going to change the context, so let's make a copy
                final Context copyContext = copyContext(context);
                // Get the move to play
                final Move m = moveList.get(i);
                // Heuristics will change with the move so let's copy them
                Heuristics copyHeuristic = new Heuristics(heuristics);

                // Play the move
                game.apply(copyContext, m);

                // Update the heuristics with the move
                copyHeuristic.updateHeuristics(copyContext, m, currentPlayerToMove);


                //SEARCH
                float value = 0;
                if (copyContext.state().mover() == currentPlayerToMove) {
                    // If we are the next mover (we added a marble, and we will remove one from opponent's marbles)
                    value = AlfaBetaSearchTimeConstrained(copyContext, searchDepth, alpha, beta, new Heuristics(copyHeuristic), stopTime);
                } else {
                    // Else is a typical Add move
                    value = -AlfaBetaSearchTimeConstrained(copyContext, searchDepth - 1, -beta, -alpha, new Heuristics(copyHeuristic), stopTime);
                }

                if (System.currentTimeMillis() >= stopTime)    // Check for the timer
                {
                    bestMove = null;
                    break;
                }


                scoredMoves.put(m, value);

                if (value > Float.NEGATIVE_INFINITY) {
                    nonLoosingMoves += 1;
                }

                if (value > score) {
                    // Found a better move
                    score = value;
                    bestMove = m;
                    // Will update the real board and scores with the new move
                    newBestHeuristic.setBoardHeuristics(copyHeuristic.getBoardHeuristics());
                    newBestHeuristic.setEvaluationFunction(copyHeuristic.getEvaluationFunction());

                }

                if (score > alpha) {
                    alpha = score;
                }

                if (score >= beta) {
                    // Prune
                    break;
                }

            }


            if (bestMove != null) {
                // We found a move in this level

                if (score == Float.POSITIVE_INFINITY) {
                    // Proved win
                    heuristics.setEvaluationFunction(newBestHeuristic.getEvaluationFunction());
                    heuristics.setBoardHeuristics(newBestHeuristic.getBoardHeuristics());
                    System.out.println("Proved win!");
                    return bestMove;
                } else if (score == Float.NEGATIVE_INFINITY) {
                    // Proved loss
                    heuristics.setEvaluationFunction(newBestHeuristic.getEvaluationFunction());
                    heuristics.setBoardHeuristics(newBestHeuristic.getBoardHeuristics());
                    System.out.println("Proved loss!");
                    return bestMoveGlobal;
                }

                if (nonLoosingMoves == 1) {
                    // Forced move! We will have to play it.
                    heuristics.setEvaluationFunction(newBestHeuristic.getEvaluationFunction());
                    heuristics.setBoardHeuristics(newBestHeuristic.getBoardHeuristics());

                    System.out.println("Forced move!");

                    return bestMove;

                }

                nonLoosingMoves = 0;
                bestMoveGlobal = bestMove;
                searchDepth += 1;
            } else {
                // We didn't find a move in this level (unfinished search)
                searchDepth -= 1;
            }

            if (System.currentTimeMillis() >= stopTime) {
                // Check the timer

                heuristics.setEvaluationFunction(newBestHeuristic.getEvaluationFunction());
                heuristics.setBoardHeuristics(newBestHeuristic.getBoardHeuristics());

                return bestMoveGlobal;
            }

            // Ordering moves for next search

            List<Move> tempList = new ArrayList<>(scoredMoves.keySet().stream().toList());


            // According to evaluation
            tempList.sort((a, b) -> {
                float value1 = scoredMoves.get(a);
                float value2 = scoredMoves.get(b);
                return Float.compare(value1, value2);
            });


            // Get rid of the old scored moves
            scoredMoves.clear();

            // Fill with new ordered moves (best move -> last )
            moveList = new ArrayList<>(tempList);


        }
        // Update
        heuristics.setEvaluationFunction(newBestHeuristic.getEvaluationFunction());
        heuristics.setBoardHeuristics(newBestHeuristic.getBoardHeuristics());


        return bestMoveGlobal;
    }


    private float AlfaBetaSearchTimeConstrained(Context context, int depth, double alpha, double beta, Heuristics heuristics, long stopTime) {

        double copyBeta = beta;
        long hash = context.state().fullHash(context);
        TableData data;
        data = table.get(hash);

        // Get data from TT
        if (data != null && data.depth >= depth) {
            switch (data.boundType) {
                case EXACT -> {
                    return data.value;
                }
                case LOWER -> {
                    alpha = Math.max(alpha, data.value);
                }
                case UPPER -> {
                    beta = Math.min(beta, data.value);
                }
            }
            if (alpha >= beta) {
                return data.value;
            }
        }

        int currentPlayerToMove = context.state().mover();
        int opponent = currentPlayerToMove == 1 ? 2 : 1;

        if (context.trial().over()) {
            // Proved win, loss or draw. (+infinity, -infinity, 0)
            int winner = context.trial().status().winner();
            if (winner == currentPlayerToMove) {
                return Float.POSITIVE_INFINITY;
            } else if (winner == opponent) {
                return Float.NEGATIVE_INFINITY;
            }
            return 0;

        } else if (depth == 0) {
            return heuristics.getEvaluationFunction().evaluate(currentPlayerToMove, opponent);
        }

        float score = Float.NEGATIVE_INFINITY;
        final Game game = context.game();

        // Get legal moves sorted
        List<Move> moveList = heuristics.getLegalMovesSorted(context);
        final int numMoves = moveList.size();

        //Refresh moveList with killer moves
        Queue<Move> killers = killerMoves.get(depth);

        if (killers != null) {
            int size = killers.size();
            for (int i = size - 1; i > -1; i--) {
                //First killer move will be the best, so it should be placed last
                Move killer = killers.poll();
                if (killer != null) {
                    seekAndReplace(moveList, numMoves, killer);
                }
            }
        }

        //Refresh moveList with TT
        if (data != null) {
            seekAndReplace(moveList, numMoves, data.bestMove);
        }


        //Multi-cut
        if (depth - 1 - R >= 0) {
            Float beta1 = multiCut(context, depth, alpha, beta, heuristics, stopTime, numMoves, moveList, game, currentPlayerToMove);
            if (beta1 != null) {
                return beta1;
            }
        }

        // Generate children and search
        Move bestMove = moveList.get(numMoves - 1);

        for (int i = numMoves - 1; i > -1; i--) {
            nodes += 1;
            // We are going to change the context, so let's make a copy
            final Context copyContext = copyContext(context);
            // Get the move to play
            final Move m = moveList.get(i);

            // Heuristics will change with the move so let's copy them
            Heuristics copyHeuristic = new Heuristics(heuristics);


            //PLAY
            game.apply(copyContext, m);

            // Update the heuristics with the move
            copyHeuristic.updateHeuristics(copyContext, m, currentPlayerToMove);

            //SEARCH
            float value = 0;
            if (copyContext.state().mover() == currentPlayerToMove) {
                // If we are the next mover (we added a marble, and we will remove one from opponent's marbles)
                value = AlfaBetaSearchTimeConstrained(copyContext, depth, alpha, beta, new Heuristics(copyHeuristic), stopTime);
            } else {
                // Else is a typical Add move
                value = -AlfaBetaSearchTimeConstrained(copyContext, depth - 1, -beta, -alpha, new Heuristics(copyHeuristic), stopTime);
            }

            if (System.currentTimeMillis() >= stopTime) {
                // We run out of time, return the worst score possible (overall)
                return 0;
            }

            if (value > score) {
                // Found a better move
                bestMove = m;
                score = value;
            }
            if (score > alpha) {
                alpha = score;
            }
            if (score >= beta) {
                // Prune
                if (killers != null) {
                    if (!killers.offer(m)) {
                        //Queue is full
                        killers.remove();
                        killers.offer(m);
                    }
                } else {
                    // Queue does not exist -> create it and add the new killer move
                    killers = new ArrayBlockingQueue<>(2);
                    killers.offer(m);
                    killerMoves.put(depth, killers);
                }
                break;
            }
        }
        if (score <= copyBeta) {
            table.save(new TableData(score, TranspositionBound.UPPER, bestMove, depth, hash));
        } else if (score >= alpha) {
            table.save(new TableData(score, TranspositionBound.LOWER, bestMove, depth, hash));
        } else {
            table.save(new TableData(score, TranspositionBound.EXACT, bestMove, depth, hash));
        }

        return score;

    }

    private Float multiCut(Context context, int depth, double alpha, double beta, Heuristics heuristics, long stopTime,
                           int numMoves, List<Move> moveList, Game game, int currentPlayerToMove) {
        int c = 0, mm = 0;
        for (int i = numMoves - 1; i > -1 && mm < M; --i) {
            final Context copyContext = copyContext(context);
            final Move m = moveList.get(i);
            Heuristics copyHeuristic = new Heuristics(heuristics);
            game.apply(copyContext, m);
            copyHeuristic.updateHeuristics(copyContext, m, currentPlayerToMove);

            float value = 0;
            if (copyContext.state().mover() == currentPlayerToMove) {
                value = AlfaBetaSearchTimeConstrained(copyContext, depth, alpha, beta, new Heuristics(copyHeuristic), stopTime);
            } else {
                value = -AlfaBetaSearchTimeConstrained(copyContext, depth - 1 - R, -beta, -alpha, new Heuristics(copyHeuristic), stopTime);
            }
            if (value >= beta) {
                c++;
                if (c >= C) {
                    return (float) beta;
                }
            }
            mm++;
        }

        return null;
    }

    private void seekAndReplace(List<Move> moveList, int numMoves, Move killer) {
        for (int j = numMoves - 1; j > -1; j--) {
            if (moveList.get(j).equals(killer)) {
                Move temp = moveList.get(j);
                moveList.remove(temp);
                moveList.add(temp);
                break;
            }
        }
    }

}
