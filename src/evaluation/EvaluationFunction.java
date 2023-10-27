package evaluation;

public abstract class EvaluationFunction {


    public int[] getScores() {
        return scores;
    }

    public void setScores(int[] scores) {
        this.scores = scores;
    }

    protected int[] scores;

    public EvaluationFunction(int[] scores) {
        this.scores = scores;
    }


    public abstract EvaluationFunction copyEvaluationFunction();


    public abstract float evaluate(int currentPlayerToMove, int opponent);
}
