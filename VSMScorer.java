package scorer;

import ds.Document;
import ds.Query;
import utils.IndexUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Skeleton code for the implementation of a
 * Cosine Similarity scorer in Task 1.
 */
public class VSMScorer extends AScorer {

    /*
     * TODO: You will want to tune the values for
     * the weights for each field.
     */
    double titleweight  = 0.1;
    double bodyweight = 0.2;

    /**
     * Construct a Cosine Similarity scorer.
     * @param utils Index utilities to get term/doc frequencies
     */
    public VSMScorer(IndexUtils utils) {
        super(utils);
    }

    /**
     * Get the net score for a query and a document.
     * @param tfs the term frequencies
     * @param q the ds.Query
     * @param tfQuery the term frequencies for the query
     * @param d the ds.Document
     * @return the net score
     */
    public double getNetScore(Map<String, Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery, Document d) {
        double score = 0.0;

        /*
         * TODO : Your code here
         * See Equation 1 in the handout regarding the net score
         * between a query vector and the term score vectors
         * for a document.
         */
        for (String s: q.queryWords)
            score += tfQuery.get(s.toLowerCase()) * (titleweight * tfs.get("title").get(s.toLowerCase()) + bodyweight * tfs.get("body").get(s.toLowerCase()));

        return score;
    }

    /**
     * Normalize the term frequencies.
     * @param tfs the term frequencies
     * @param d the ds.Document
     * @param q the ds.Query
     */
    public void normalizeTFs(Map<String,Map<String, Double>> tfs, Document d, Query q) {
        /*
         * TODO : Your code here
         * Note that we should use the length of each field 
         * for term frequency normalization as discussed in the assignment handout.
         */
        Map<String,Integer> done = new HashMap<>();
        Map<String,Integer> done2 = new HashMap<>();

        for(String s : q.queryWords) {
            if (!done.containsKey(s.toLowerCase())) {
                tfs.get("title").put(s.toLowerCase(), tfs.get("title").get(s.toLowerCase()) / d.title_length);
                done.put(s.toLowerCase(), 1);
            }
        }

        for(String s : q.queryWords) {
            if (!done2.containsKey(s.toLowerCase())) {
                tfs.get("body").put(s.toLowerCase(), tfs.get("body").get(s.toLowerCase()) / d.body_length);
                done2.put(s.toLowerCase(), 1);
            }
        }

    }

    /**
     * Write the tuned parameters of vsmSimilarity to file.
     * Only used for grading purpose, you should NOT modify this method.
     * @param filePath the output file path.
     */
    private void writeParaValues(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            String[] names = {
                    "titleweight", "bodyweight"
            };
            double[] values = {
                    this.titleweight, this.bodyweight
            };
            BufferedWriter bw = new BufferedWriter(fw);
            for (int idx = 0; idx < names.length; ++ idx) {
                bw.write(names[idx] + " " + values[idx]);
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    /** Get the similarity score between a document and a query.
     * @param d the ds.Document
     * @param q the ds.Query
     * @return the similarity score.
     */
    public double getSimScore(Document d, Query q) {
        Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);
        this.normalizeTFs(tfs, d, q);
        Map<String,Double> tfQuery = getQueryFreqs(q);

        // Write out tuned vsmSimilarity parameters
        // This is only used for grading purposes.
        // You should NOT modify the writeParaValues method.
        writeParaValues("vsmPara.txt");
        return getNetScore(tfs,q,tfQuery,d);
    }
}
