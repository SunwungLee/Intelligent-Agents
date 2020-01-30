package group16;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.issue.Issue;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Agent16UtilityEstimator extends AdditiveUtilitySpaceFactory {

    private Domain domain;
    private AdditiveUtilitySpace u;

    /**
     * Generates an simple Utility Space on the domain, with equal weights and zero values.
     * Everything is zero-filled to already have all keys contained in the utility maps.
     *
     * @param d
     */
    public Agent16UtilityEstimator(Domain d) {
        super(d);
        domain = d;
    }
   
//	public void normalizeWeightsByMaxValues()
//	{
//		for (Issue i : getIssues())
//		{
//			if (i!= null);//my add
//			 EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
//			 evaluator.normalizeAll();
//		}
//		for (Issue i : getIssues())
//		{
//			if (i!= null);// my add
//			 EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
//			 evaluator.scaleAllValuesFrom0To1();
//		}
//		u.normalizeWeights();
//	}

    @Override
    public void estimateUsingBidRanks(BidRanking r) {
        HashMap<Issue, HashMap<ValueDiscrete, List<Integer>>> issueValues = new HashMap<>();

        int position = 0;
        for(Bid b : r.getBidOrder()){
            position++;
            List<Issue> issues = b.getIssues();
            for(Issue i: issues) {
                Integer issueNo = i.getNumber();

                ValueDiscrete v = (ValueDiscrete) b.getValue(issueNo);//这个bid 的这个issure的这个value
                HashMap<ValueDiscrete, List<Integer>> valuePositions = issueValues.computeIfAbsent(i, k -> new HashMap<>()); // TODO: Confirm equality works here
                List<Integer> positionList = valuePositions.computeIfAbsent(v, k -> new ArrayList<>());
                positionList.add(position);
            }
        }

        HashMap<Issue, HashMap<ValueDiscrete, Double>> means = new HashMap<>();
        HashMap<Issue, List<Double>> stddevs = new HashMap<>();
        for(Issue issue: issueValues.keySet()){
            HashMap<ValueDiscrete, List<Integer>> valuePositions = issueValues.get(issue);
            ArrayList<Double> stddevlist = new ArrayList<>();
            for(ValueDiscrete value : valuePositions.keySet()){
                List<Integer> positions  = valuePositions.get(value);
                double mean = positions.stream().mapToInt(e -> e).average().orElseThrow(() -> new RuntimeException("Value exists with no positions for it"));
                double stddev = 0d;
                for(int v : positions){
                    stddev += (v - mean) * (v - mean);
                }

                stddev /= (double) positions.size();
                stddev = Math.sqrt(stddev);

               // mean = r.getSize() - mean;//22
            //    means.computeIfAbsent(issue, k -> new HashMap<>()).put(value, mean);
                //stddevs.computeIfAbsent(issue, k -> new ArrayList<>()).add(stddev);//11
                stddevlist.add(stddev);
                setUtility(issue, value, mean);

            }

            Double maxStddev = stddevlist.stream().mapToDouble(e -> e).max().getAsDouble();
            setWeight(issue, 1d / maxStddev);
        }

        scaleAllValuesFrom0To1();
        normalizeWeights();


    }



}

