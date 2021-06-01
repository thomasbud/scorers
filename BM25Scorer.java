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
 * Skeleton code for the implementation of a BM25 scorer in Task 2.
 */
public class BM25Scorer extends AScorer {

    /*
     *  TODO: You will want to tune these values
     */
    double titleweight  = 2.2;
    double bodyweight = 0.1;

    // BM25-specific weights
    double btitle = 0.1;
    double bbody = 0.8;

    double k1 = 1.4;
    double pageRankLambda = 0.7;
    double pageRankLambdaPrime = 0.4;

    // query -> url -> document
    Map<Query,Map<String, Document>> queryDict;

    // BM25 data structures--feel free to modify these
    // ds.Document -> field -> length
    Map<Document,Map<String,Double>> lengths;

    // field name -> average length
    Map<String,Double> avgLengths;

    // ds.Document -> pagerank score
    Map<Document,Double> pagerankScores;

    /**
     * Construct a scorer.BM25Scorer.
     * @param utils Index utilities
     * @param queryDict a map of query to url to document
     */
    public BM25Scorer(IndexUtils utils, Map<Query,Map<String, Document>> queryDict) {
        super(utils);
        this.queryDict = queryDict;
        this.calcAverageLengths();
    }

    /**
     * Set up average lengths for BM25, also handling PageRank.
     */
    public void calcAverageLengths() {
        lengths = new HashMap<>();
        avgLengths = new HashMap<>(); 
        pagerankScores = new HashMap<>();

        /*
         * TODO : Your code here
         * Initialize any data structures needed, perform
         * any preprocessing you would like to do on the fields,
         * accumulate lengths of fields.
         * handle pagerank.  
         */
        for(Query qu : queryDict.keySet()) {
            for (String ur : queryDict.get(qu).keySet()) {
                pagerankScores.put(queryDict.get(qu).get(ur), (double) queryDict.get(qu).get(ur).page_rank);
            }
        }

        double T_cum = 0;
        double B_cum = 0;
        double T_count = 0;
        double B_count = 0;

        for (String tfType : this.TFTYPES) {
            /*
             * TODO : Your code here
             * Normalize lengths to get average lengths for
             * each field (body, title).
             */
            for(Query qu : queryDict.keySet()){
                for(String ur : queryDict.get(qu).keySet()){
                    Map<String,Double> titleMap = lengths.get(queryDict.get(qu).get(ur));
                    if (tfType.equals("title")){
                        lengths.put(queryDict.get(qu).get(ur), new HashMap(){{put(tfType, (double) queryDict.get(qu).get(ur).title_length);}});
                        T_cum += queryDict.get(qu).get(ur).title_length;
                        T_count++;
                    }
                    else{ //if tfType.equals("body")
                        titleMap.put(tfType, (double)queryDict.get(qu).get(ur).body_length);
                        lengths.put(queryDict.get(qu).get(ur), titleMap);
                        B_cum += queryDict.get(qu).get(ur).body_length;
                        B_count++;
                    }
                }
            }
        }

        avgLengths.put("title", T_cum/T_count);
        avgLengths.put("body", B_cum/B_count);
    }

    /**
     * Get the net score.
     * @param tfs the term frequencies
     * @param q the ds.Query
     * @param tfQuery
     * @param d the ds.Document
     * @return the net score
     */
    public double getNetScore(Map<String,Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery, Document d) {

        double score = 0.0;

        /*
         * TODO : Your code here
         * Use equation 3 first and then equation 4 in the writeup to compute the overall score
         * of a document d for a query q.
         */
        double weight = 0;

        String vjFunction = "log";
        double vj = 0;
        switch(vjFunction)
        {
            case "log":
                vj = Math.log10(pageRankLambdaPrime + d.page_rank);
                break;
            case "saturation":
                vj = d.page_rank/(pageRankLambdaPrime + d.page_rank);
                break;
            case "sigmoid":
                vj = 1/(pageRankLambdaPrime + Math.exp(-d.page_rank * pageRankLambdaPrime));
                break;
        }

        for (String s : q.queryWords) {
            weight = titleweight * tfs.get("title").get(s.toLowerCase()) + bodyweight * tfs.get("body").get(s.toLowerCase());
            score += (weight/(k1+weight)) * tfQuery.get(s) + pageRankLambda * vj;
        }

        return score;
    }

    /**
     * Do BM25 Normalization.
     * @param tfs the term frequencies
     * @param d the ds.Document
     * @param q the ds.Query
     */
    public void normalizeTFs(Map<String,Map<String, Double>> tfs, Document d, Query q) {
        /*
         * TODO : Your code here
         * Use equation 2 in the writeup to normalize the raw term frequencies
         * in fields in document d.
         */
        Map<String,Integer> done = new HashMap<>();
        Map<String,Integer> done2 = new HashMap<>();

        for(String s : q.queryWords) {
            if (!done.containsKey(s.toLowerCase())) {
                tfs.get("title").put(s.toLowerCase(), tfs.get("title").get(s.toLowerCase()) / ((1-btitle)+btitle*(lengths.get(d).get("title")/avgLengths.get("title"))));
                done.put(s.toLowerCase(), 1);
            }
        }

        for(String s : q.queryWords) {
            if (!done2.containsKey(s.toLowerCase())) {
                tfs.get("body").put(s.toLowerCase(), tfs.get("body").get(s.toLowerCase()) / ((1-bbody)+bbody*(lengths.get(d).get("body")/avgLengths.get("body"))));
                done2.put(s.toLowerCase(), 1);
            }
        }
    }

    /**
     * Write the tuned parameters of BM25 to file.
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
                    "titleweight", "bodyweight", "btitle",
                    "bbody", "k1", "pageRankLambda", "pageRankLambdaPrime"
            };
            double[] values = {
                    this.titleweight, this.bodyweight, this.btitle,
                    this.bbody, this.k1, this.pageRankLambda,
                    this.pageRankLambdaPrime
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
    /**
     * Get the similarity score.
     * @param d the ds.Document
     * @param q the ds.Query
     * @return the similarity score
     */
    public double getSimScore(Document d, Query q) {
        Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);
        this.normalizeTFs(tfs, d, q);
        Map<String,Double> tfQuery = getQueryFreqs(q);

        // Write out the tuned BM25 parameters
        // This is only used for grading purposes.
        // You should NOT modify the writeParaValues method.
        writeParaValues("bm25Para.txt");
        return getNetScore(tfs,q,tfQuery,d);
    }

}
