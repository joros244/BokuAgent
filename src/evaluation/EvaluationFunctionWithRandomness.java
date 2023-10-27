package evaluation;

import java.util.Arrays;
import java.util.Random;

public class EvaluationFunctionWithRandomness extends EvaluationFunction {


    private final float randomFactor;

    public EvaluationFunctionWithRandomness(int[] scores, float randomFactor) {
        super(scores);
        this.randomFactor = randomFactor;
    }


    @Override
    public EvaluationFunction copyEvaluationFunction() {
        return new EvaluationFunctionWithRandomness(Arrays.copyOf(this.getScores(), 2), this.randomFactor);
    }

    @Override
    public float evaluate(int currentPlayerToMove, int opponent) {
        // This evaluation function adds a random factor to the score. (Should be around 10%)
        int score = (scores[currentPlayerToMove - 1] - scores[opponent - 1]);
        Random r = new Random();
        int bound = (int) Math.floor(Math.abs(score) * randomFactor);
        int sign = r.nextInt(10) % 2 == 0 ? -1 : 1;
        if (bound > 0) {
            return score + sign * r.nextInt(bound);
        } else {
            return score + sign;
        }
    }
}
