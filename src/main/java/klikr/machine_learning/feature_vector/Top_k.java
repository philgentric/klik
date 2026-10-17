package klikr.machine_learning.feature_vector;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Top_k<T>
//**********************************************************
{
    private final int k;
    private final double[] scores; // Sorted ASC
    private final Object[] items;
    private int count = 0;

    //**********************************************************
    public Top_k(int k)
    //**********************************************************
    {
        this.k = k;
        this.scores = new double[k];
        this.items = new Object[k];
        Arrays.fill(scores, Double.MAX_VALUE);
    }

    //**********************************************************
    public void add(double score, T item)
    //**********************************************************
    {
        if (score >= scores[k - 1]) {
            return;
        }
        int i = k - 2;
        while (i >= 0 && scores[i] > score) {
            scores[i + 1] = scores[i];
            items[i + 1] = items[i];
            i--;
        }
        scores[i + 1] = score;
        items[i + 1] = item;
        if (count < k) count++;
    }

    //**********************************************************
    public List<Result<T>> get_results()
    //**********************************************************
    {
        List<Result<T>> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (items[i] != null) {
                results.add(new Result<>((T) items[i], scores[i]));
            }
        }
        return results;
    }

    public record Result<T>(T item, double score) {}
}
