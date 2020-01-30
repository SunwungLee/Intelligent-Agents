package group16;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.ExperimentalUserModel;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;
import genius.core.utility.UncertainAdditiveUtilitySpace;
import sun.awt.SunHints;

import javax.swing.*;
import javax.swing.undo.AbstractUndoableEdit;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


/**
 * A simple example agent that makes random bids above a minimum target utility. 
 */
public class Agent16 extends AbstractNegotiationParty 
{
	private final String description = "Group 16";

    private Agent16BiddingStrategy biddingStrategy;
    private Agent16OpponentModel opponentModel;
    private Agent16AcceptanceStrategy acceptanceStrategy;
    private NegotiationInfo info;
    private Bid lastReceivedOffer; // Current offer on the table
    private Bid myLastOffer; // Latest offer made by the agent
    private double utilityThreshold;
    // Initial idea - Threshold decreased linearly to begin with then try exponential
    // Trying to set threshold to Nash point instead
    private double proportionBidsToEstimate = 0.5;
    private Bid bestOfferSoFar = null; // Best bid offered so far from opponent
    public Bid hightest;
    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        this.info = info;
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));


        AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
        AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;

        List< Issue > issues = additiveUtilitySpace.getDomain().getIssues();

        for (Issue issue : issues) {
            int issueNumber = issue.getNumber();
            System.out.println(">> " + issue.getName() + " weight: " + additiveUtilitySpace.getWeight(issueNumber));

            // Assuming that issues are discrete only
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                System.out.println(valueDiscrete.getValue());
                System.out.println("Evaluation(getValue): " + evaluatorDiscrete.getValue(valueDiscrete));
                try {
                    System.out.println("Evaluation(getEvaluation): " + evaluatorDiscrete.getEvaluation(valueDiscrete));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    	if (userModel != null) {
    		System.out.println("Preference uncertainty is enabled.");
    		BidRanking bidRanking = userModel.getBidRanking();
    		System.out.println("The agent ID is:"+info.getAgentID());
    		System.out.println("Total number of possible bids:" +userModel.getDomain().getNumberOfPossibleBids());
    		hightest = bidRanking.getMaximalBid();
    		System.out.println("The number of bids in the ranking is:" + bidRanking.getSize());
    		System.out.println("The lowest bid is:"+bidRanking.getMinimalBid());
    		System.out.println("！！！！！All of the bids is:！！！！" + bidRanking.getBidOrder());
//            System.out.println("The highest bid is:"+bidRanking.getMaximalBid());
            System.out.println("The highest bid is:"+hightest);
            System.out.println("The highest bid issue 1 value is" + hightest.getValue(1));
    		//System.out.println("The elicitation costs are:"+user.getElicitationCost());
    		List<Bid> bidList = bidRanking.getBidOrder();
    		System.out.println("The 5th bid in the ranking is:"+bidList.get(5));
    	}
        
        // This is where the utility estimation is done - at the start only
        utilitySpace = estimateUtilitySpace();

        try {
            // Setting utility threshold as high as possible to begin with
            // This means it's unlikely any bids will be accepted to begin with and gives the agent
            // a chance to model the opponent to find the Nash point
            utilityThreshold = this.getUtility(this.getUtilitySpace().getMaxUtilityBid());
        } catch (Exception e) {
            e.printStackTrace();
            // Random high threshold set if cannot access maximum possible utility
            utilityThreshold = 0.95;
        }

        opponentModel = new Agent16OpponentModel(this.getDomain());
        acceptanceStrategy = new Agent16AcceptanceStrategy(this);
        biddingStrategy = new Agent16BiddingStrategy(this);
    }

    /**
     * When this function is called, it is expected that the Party chooses one of the actions from the possible
     * action list and returns an instance of the chosen action.
     *
     * @param list
     * @return
     */
    @Override
    public Action chooseAction(List<Class<? extends Action>> list) {
        // Using Stacked Alternating Offers Protocol so only actions are Accept, Offer and EndNegotiation
        // EndNegotiation not used - Reservation value is zero so our agent prefers to accept any deal rather than end the negotiation

        if (acceptanceStrategy.accept(lastReceivedOffer)) {
            return new Accept(this.getPartyId(), lastReceivedOffer);
        } else {
            try {
                myLastOffer = biddingStrategy.getBid();
            } catch(Exception e) {
                e.printStackTrace();
                // Fallback in case exception occurred getting bid, always offer something
                myLastOffer = generateRandomBid();
            }
            // Double fallback in case no exception when generating bid but bid returned still null, always offer something
            if (myLastOffer == null) {
                myLastOffer = generateRandomBid();
            }
            return new Offer(this.getPartyId(), myLastOffer);
        }
    }

    @Override
    public AbstractUtilitySpace estimateUtilitySpace() {
        Domain domain = getDomain();
        Agent16UtilityEstimator factory = new Agent16UtilityEstimator(domain);
        BidRanking bidRanking;
        bidRanking = userModel.getBidRanking();

        factory.estimateUsingBidRanks(bidRanking);

        AbstractUtilitySpace us = factory.getUtilitySpace();

        return us;
    }

    /**
     * This method is called to inform the party that another NegotiationParty chose an Action.
     * @param sender
     * @param act
     */
    @Override
    public void receiveMessage(AgentID sender, Action act) {
        super.receiveMessage(sender, act);

        if (act instanceof Offer) { // sender is making an offer
            Offer offer = (Offer) act;
            // storing last received offer
            lastReceivedOffer = offer.getBid();
            opponentModel.recievedBid(offer.getBid());
            // Storing the best bid offered by the opponent (i.e. the one with highest utility for us)
            if (bestOfferSoFar == null) {
                bestOfferSoFar = lastReceivedOffer;
            } else {
                if (this.getUtility(lastReceivedOffer) > this.getUtility(bestOfferSoFar)) {
                    bestOfferSoFar = lastReceivedOffer;
                }
            }
        }
    }

    /**
     * A human-readable description for this party.
     * @return agent description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Get the current utility threshold of the agent
     * @return utility threshold
     */
    
    public double getUtilityThreshold() {
        return utilityThreshold;
    }

    /**
     * Get the last offer the agent received
     * @return last received offer
     */
    public Bid getLastReceivedOffer() {
        return lastReceivedOffer;
    }

    /**
     * Get the last offer this agent made
     * @return the agent's last offer
     */
    public Bid getMyLastOffer() {
        return myLastOffer;
    }

    /**
     * Get the best offer made so far from the opponent
     * @return opponent's best offer so far
     */
    public Bid getBestOfferSoFar() {
        return bestOfferSoFar;
    }

    /**
     * Set the utility threshold of this agent
     * @param threshold new utility threshold
     */
    public void setUtilityThreshold(double threshold) {
        this.utilityThreshold = threshold;
    }

    /**
     * Get the model for the agent's opponent
     * @return opponent model
     */
    public Agent16OpponentModel getOpponentModel() {
        return opponentModel;
    }

    private void evaluateEstimatedUtilitySpace(){
        AbstractUtilitySpace ours = utilitySpace;
        AbstractUtilitySpace real;
        if (userModel != null) {
            real = ((ExperimentalUserModel) userModel).getRealUtilitySpace();
        } else {
            real = this.info.getUtilitySpace();
        }
        System.out.println("Ours: \n" + ours);
        System.out.println("Real: \n" + real);

        try {
            System.out.println("Our Best: " + ours.getMaxUtilityBid());
            System.out.println("Real Best: " + real.getMaxUtilityBid());
            System.out.println("Our Worst: " + ours.getMinUtilityBid());
            System.out.println("Real Worst: " + real.getMinUtilityBid());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("ours,real");
        for (int i = 0; i < 10000; i++) {
            Bid randomBid =  getDomain().getRandomBid(new Random());
            System.out.println(real.getUtility(randomBid) + "," + ours.getUtility(randomBid));
        }
    }
	
	

}
