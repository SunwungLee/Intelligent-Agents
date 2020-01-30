package group16;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.DomainImpl;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;



public class Agent16OpponentModel {

	private Domain d;
	private Integer[][] counter;  // track of the frequency of the issue values
	private Double[] issueWeights;
	private Double[][] optionOrder;
	private int numberOfBids = 0;
	private HashMap<ArrayList<Integer>,String> discreteValueIndex = new HashMap<ArrayList<Integer>,String>();
	private HashMap<String,ArrayList<Integer>> reverseDiscreteValueIndex = new HashMap<String,ArrayList<Integer>>();
	private HashMap<Integer,Integer> issueEvaluator = new HashMap<Integer,Integer>();


	public Agent16OpponentModel(Domain d) {
		this.d = d;
		createIndexAndCounter();
	}

	public static void main(String[] args) throws IOException {

		DomainImpl d = new DomainImpl("domaintest.xml");

		Bid[] listOfBids = new Bid[3];

		Agent16OpponentModel om = new Agent16OpponentModel(d);


		for(int i = 0; i < 20; i ++)
		{

			om.recievedBid(d.getRandomBid(new Random()));


		}




	}

	public Double opponentBidUtility(Bid b) // for a bid , get the opponent's utility 
	{
		List<Issue> issueList = d.getIssues();
		Double utility = 0.0;



		for(int i = 0; i < b.getIssues().size(); i ++)
		{
			Value bidValue = b.getValue(issueEvaluator.get(i));
			ValueDiscrete vd = (ValueDiscrete) bidValue;

			ArrayList<Integer> tableLookup = reverseDiscreteValueIndex.get(vd.getValue());
			utility += issueWeights[tableLookup.get(0)] * optionOrder[tableLookup.get(0)][tableLookup.get(1)];

		}


		return utility;
	}

	private void createIndexAndCounter()
	{
		List<Issue> issueList = d.getIssues();
		counter = new Integer[issueList.size()][];
		discreteValueIndex = new HashMap<ArrayList<Integer>,String>();
		issueEvaluator = new HashMap<Integer,Integer>();

		ArrayList<Integer> valueKey;

		//create counter
		for(Integer i = 0; i < issueList.size(); i ++)
		{
			Issue iss = issueList.get(i);
			issueEvaluator.put(i, iss.getNumber());

			IssueDiscrete id = (IssueDiscrete) issueList.get(i);
			counter[i] = new Integer[id.getNumberOfValues()];
			for(Integer p= 0; p < id.getNumberOfValues(); p ++)
			{
				valueKey = new ArrayList<Integer>();
				valueKey.add(i);
				valueKey.add(p);
				discreteValueIndex.put(valueKey, id.getValue(p).getValue());
				reverseDiscreteValueIndex.put(id.getValue(p).getValue(), valueKey);
				counter[i][p] = 0;
			}
		}



	}


	public void recievedBid(Bid b)
	{
		ArrayList<Integer> valueKey;

		for(int i = 0; i< counter.length; i ++)
		{
			for(int p = 0; p < counter[i].length; p ++)
			{

				Value bidValue = b.getValue(issueEvaluator.get(i));
				ValueDiscrete vd = (ValueDiscrete) bidValue;
				valueKey = new ArrayList<Integer>();
				valueKey.add(i);
				valueKey.add(p);
				if(vd.getValue().equals(discreteValueIndex.get(valueKey)))
				{
					counter[i][p] ++;
				}

			}
		}
		numberOfBids += 1;
		orderOfOptions();
		issueWeights();

		if(numberOfBids == 200)
		{
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream("200bid.csv"), "utf-8"))) {

				for(int i = 0; i< optionOrder.length; i ++)
				{
					for(int p = 0; p < optionOrder[i].length; p ++)
					{
						Double e = issueWeights[i] * optionOrder[i][p];
						writer.write(e.toString() + System.lineSeparator());
					}
				}

			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}


	/* creates freq from nothing (not required anymore)
	private void findFrequency()
	{

		counter = emptyCounter.clone();
		//find freq

		ArrayList<Integer> valueKey;

		for(int i = 0; i< counter.length; i ++)
		{
			for(int p = 0; p < counter[i].length; p ++)
			{
				for(Bid b: listOfBids)
				{
					Value bidValue = b.getValue(issueEvaluator.get(i));

					ValueDiscrete vd = (ValueDiscrete) bidValue;
					valueKey = new ArrayList<Integer>();
					valueKey.add(i);
					valueKey.add(p);
					if(vd.getValue().equals(discreteValueIndex.get(valueKey)))
					{
						counter[i][p] ++;
					}




				}
			}
		}
	}
	 */
	private void orderOfOptions()
	{

		optionOrder = new Double[ counter.length][];
		for(int i = 0; i<  counter.length; i ++)
		{
			ArrayIndexComparator comparator = new ArrayIndexComparator(counter[i]);
			Integer[] indexes = comparator.createIndexArray();
			Arrays.sort(indexes, comparator);	

			optionOrder[i] = new Double[indexes.length];
			for(int p = 0; p < indexes.length;p ++)
			{
				Double d = (double) (indexes.length - indexes[p] );
				d /= (double) indexes.length;
				optionOrder[i][p] = d;
			}

		}



	}

	private void issueWeights()
	{
		Double[] unnormalisedIssueWeights = new Double[counter.length];

		double sumWeights = 0;
		for(int i = 0; i < counter.length;i ++)
		{
			unnormalisedIssueWeights[i] = 0.0;
			for(int p = 0; p < counter[i].length; p ++)
			{

				if(numberOfBids != 0 && counter[i][p] != 0)
				{
					unnormalisedIssueWeights[i] += (Math.pow(counter[i][p], 2) / (Math.pow(numberOfBids, 2) ));
				}
			}

			sumWeights += unnormalisedIssueWeights[i];
		}


		issueWeights = new Double[counter.length];
		for(int i = 0; i < counter.length;i ++)
		{
			issueWeights[i] = unnormalisedIssueWeights[i] / sumWeights;
		}

	}

	private class ArrayIndexComparator implements Comparator<Integer>
	{
		private final Integer[] array;

		public ArrayIndexComparator(Integer[] array)
		{
			this.array = array;
		}

		public Integer[] createIndexArray()
		{
			Integer[] indexes = new Integer[array.length];
			for (int i = 0; i < array.length; i++)
			{
				indexes[i] = i; // Autoboxing
			}
			return indexes;
		}

		@Override
		public int compare(Integer index1, Integer index2)
		{
			// Autounbox from Integer to int to use as array indexes
			return array[index2].compareTo(array[index1]);
		}
	}


}

