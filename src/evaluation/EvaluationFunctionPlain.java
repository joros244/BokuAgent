package evaluation;

import java.util.Arrays;

public class EvaluationFunctionPlain extends EvaluationFunction {
    public EvaluationFunctionPlain(int[] scores) {
        super(scores);
    }

    @Override
    public EvaluationFunction copyEvaluationFunction() {
        return new EvaluationFunctionPlain(Arrays.copyOf(this.getScores(), 2));
    }


    @Override
    public float evaluate(int currentPlayerToMove, int opponent) {
        return (scores[currentPlayerToMove - 1] - scores[opponent - 1]);
    }
}
