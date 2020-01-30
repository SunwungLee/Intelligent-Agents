package group16;

import java.util.ArrayList;
import java.util.List;

import genius.core.Bid;
import genius.core.BidIterator;


public class Agent16BiddingStrategy {
	  private Agent16 agent;
	    // all deadlines must add to less than 1!
	    private double modellingDeadline = 0.15; // Deadline for initial opponent modelling to stop
	    private double nashOfferDeadline = modellingDeadline + 0.1; // Deadline for Nash computed bids to stop 纳什计算得出的出价停止的时间
	    private double closestToNashInitialThreshold;
	    private double closestToNashMidThreshold;
	    private double closestToNashMidDeadline = nashOfferDeadline + 0.55;
	    private double closestToNashEndThreshold;
	    private double closestToNashEndDeadline = nashOfferDeadline + 0.675; // Deadline for calculating closest to Nash to stop
	    private NashPointGenerator nashPointGenerator;
	    private List<Bid> alreadyOffered; //store the previous offers

	    /**
	     * Constructor to pass reference to the agent
	     * @param agent - the reference to group14.Agent16
	     */
	    public Agent16BiddingStrategy(Agent16 agent) {
	        this.agent = agent;
	        nashPointGenerator = new NashPointGenerator(agent.getDomain(), agent.getUtilitySpace(), agent.getOpponentModel()); // generate the nashPoint
	        alreadyOffered = new ArrayList<Bid>();
	        closestToNashInitialThreshold = agent.getUtilityThreshold();
	        closestToNashMidThreshold = closestToNashInitialThreshold * 0.9;
	        closestToNashEndThreshold = closestToNashInitialThreshold * 0.75;
	    }

	    /**
	     * Method to get the initial bid of the agent (i.e. at time=0)
	     * Always starts with best bid (highest utility for itself)
	     * @return initial bid
	     */
	    private Bid getInitialBid() {
	        try {
	            return agent.getUtilitySpace().getMaxUtilityBid(); // return the highest utility bid
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return null;
	    }

	    /**
	     * Method to get the next bid the agent should make
	     * Uses it's estimated utility threshold to find bids and
	     * generates a list of bids above that threshold to get all bids with best utility for itself
	     * It then picks the most likely to have greatest utility for opponent using the opponent model
	     * @return next bid
	     */
	    private Bid getNashBid() {
	        // Assumes opponent model updated with every offer
	        nashPointGenerator.updateBidSpace(agent.getOpponentModel()); // update the opponent's bid space
	        return nashPointGenerator.getNashPoint();
	    }
	    // decrease the utilityThreshold over time .
	    private double getTimeBasedUtilityThreshold(){
	        double time = this.agent.getTimeLine().getTime();
	        if(time > closestToNashEndDeadline) {
	            return closestToNashEndThreshold;
	        }
	        if(time > closestToNashMidDeadline){
	            double timePassed = time - closestToNashMidDeadline;
	            double totalTime = closestToNashEndDeadline - closestToNashMidDeadline;
	            double timePercent = timePassed / totalTime;
	            double increment = closestToNashMidThreshold - closestToNashEndThreshold;
	            double newUtility = closestToNashMidDeadline - (increment * timePercent);
	            return newUtility;
	        }
	        if(time > nashOfferDeadline){
	            double timePassed = time - nashOfferDeadline;
	            double totalTime = closestToNashMidDeadline - nashOfferDeadline;
	            double timePercent = timePassed / totalTime;
	            double increment = closestToNashInitialThreshold - closestToNashMidThreshold;
	            double newUtility = closestToNashInitialThreshold - (increment * timePercent);
	            return newUtility;
	        }
	        return closestToNashInitialThreshold;

	    }

	    private Bid getNextBid() {
	        // Slowly lowering utility threshold
	        agent.setUtilityThreshold(this.getTimeBasedUtilityThreshold());

	        Bid closestToNash = null;
	        double closestDistance = 1; // Max distance possible
	        BidIterator iterator = new BidIterator(agent.getDomain()); //generate all the bids from this domain 
	        
	        
	       //find the next bid which has the nearest nsdistance
	        while (iterator.hasNext()) {   //? hasNext ; if has the next bid?
	            Bid b = iterator.next();
	            // Only consider those bids above the threshold
	            // Also trying to avoid sending the same bid again if it wasn't accepted the first time
	            if (!alreadyOffered.contains(b) && agent.getUtility(b) >= agent.getUtilityThreshold()) {
	                double distanceToNash = nashPointGenerator.distanceToNash(b);
	                // If distance is -1, no Nash point exists
	                // If the distance is 0, this bid is the Nash point (which has already been offered before) //? previous nash points
	                if (distanceToNash != -1 && distanceToNash != 0
	                        && distanceToNash < closestDistance) {
	                    closestDistance = distanceToNash;
	                    closestToNash = b;
	                }
	            }
	        }
	        // If no bid returned - all offered already, try sending Nash bid again
	        return closestToNash == null ? getNashBid() : closestToNash;
	    }

	    /**
	     * A method to get the bid the agent should make which is dependent on time.
	     * @return the bid the agent should offer
	     */
	    public Bid getBid() {
	        double time = agent.getTimeLine().getTime();
	        Bid returnBid = null;
	        // First bid made by this agent
	        // The agent is stubborn for the first 10% of the time, offering only it's initial bid
	        // This is so the agent has a chance to generate a good model of the opponent (no discount factor)
	        if ((agent.getLastReceivedOffer() == null || agent.getMyLastOffer() == null) || time < modellingDeadline) {
	           returnBid = getInitialBid();
	        } else if (time < nashOfferDeadline) {
	            returnBid = getNashBid();
	            // Utility threshold updated to the last bid offered
	            // for the majority of the time, this will be the bid at the nash point
	            // This means the agent will not accept anything with utility lower than at the Nash point
	            agent.setUtilityThreshold(nashPointGenerator.getNashUtility()); // when time < nashOfferDeadline, set the utilityThreshold as nashUtility
	        } else if (time < closestToNashEndDeadline) {
	            // Finished offering Nash point so now lower utility threshold and offer bids closest to the Nash point
	            returnBid = getNextBid();
	        } else if (time < 1){
	            // Otherwise must be in the last stretch of the negotiation
	            returnBid = agent.getBestOfferSoFar();
	        }
	        alreadyOffered.add(returnBid);
	        return returnBid;
	    }
}
