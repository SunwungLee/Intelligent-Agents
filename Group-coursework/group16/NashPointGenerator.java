package group16;

import java.util.ArrayList;
import java.util.List;

import genius.core.Bid;
import genius.core.BidIterator;
import genius.core.Domain;
import genius.core.utility.UtilitySpace;



public class NashPointGenerator {
	
	

    /*
     * A point for a bilateral negotiation which can be interpreted as a point on a graph of the bid space
     * Each point is associated with a bid and the utilities of each agent
     */
    private class BidPoint {

        private Bid bid;
        private double agentUtility;
        private double opponentUtility;

        /**
         * A constructor to build a bid point form a bid and the associated utilities
         *
         * @param bid             the bid to represent
         * @param agentUtility    the utility of Agent Smith for that bid
         * @param opponentUtility the utility of the opponent for that bid
         */
        public BidPoint(Bid bid, double agentUtility, double opponentUtility) {
            this.bid = bid;
            this.agentUtility = agentUtility;
            this.opponentUtility = opponentUtility;
        }

        /**
         * A method to determine is this bid point is dominated by another
         * To dominate, the other bid point must have utilities which are greater than or equal to this one
         * with at least one value strictly greater
         *
         * @param other the bid point to compare against
         * @return if this bid point is dominated by the other
         */
        public boolean isDominatedBy(BidPoint other) {
            boolean dominated = false;
            if (other != this) {
                if ((other.getAgentUtility() < getAgentUtility()) || (other.getOpponentUtility() < getOpponentUtility())) {
                    // One of the utilities is smaller
                    dominated = false;
                } //else if ((other.getAgentUtility() > getAgentUtility()) || (other.getOpponentUtility() > getOpponentUtility())) {
                    // At least one utility is strictly greater, i don't think so .
                else {
                    dominated = true;
                }
            }
            return dominated;
        }

        /**
         * Returning the product of the agent and opponent utilities for this bid point
         * If reservation value is zero for both, this can be used to calculate the Nash point
         *
         * @return the product of the utilities
         */
        public double getUtilityProduct() {
            return agentUtility * opponentUtility;
        }

        public Bid getBid() {
            return bid;
        }

        public double getAgentUtility() {
            return agentUtility;
        }

        public double getOpponentUtility() {
            return opponentUtility;
        }

        public void setOpponentUtility(double utility) {
            this.opponentUtility = utility;
        }
    } // end of BidPoint class

    
    
    
    
    private Domain domain;
    private UtilitySpace agentUtilitySpace;
    private Agent16OpponentModel opponentModel;
    private List<BidPoint> bidSpace; // All bid points
    private List<BidPoint> paretoFrontier;
    private BidPoint nashPoint;
    private static final int ITERATION_LIMIT = 25000; // Maximum number of iterations before it cuts off
    private boolean bidSpaceUpdated;

    /**
     * A constructor to set the attributes needed to calculate the nash point
     * @param domain the domain of the negotiation
     * @param agentUtilitySpace the utility space for Agent Smith - this should not change after initially estimated
     * @param opponentModel the opponent model for the opponent - this will change during the negotiation
     */
    public NashPointGenerator(Domain domain, UtilitySpace agentUtilitySpace, Agent16OpponentModel opponentModel) {
        this.domain = domain;
        this.agentUtilitySpace = agentUtilitySpace;
        this.opponentModel = opponentModel;
        paretoFrontier = new ArrayList<BidPoint>();
    }

    /**
     * A method to create all the bid points// bidSpace
     */
    private void createBidSpace() {
        bidSpace = new ArrayList<BidPoint>();
        BidIterator iterator = new BidIterator(domain);
        int iterations = 0;

        while (iterator.hasNext() && iterations < ITERATION_LIMIT) {
            Bid bid = iterator.next();
            bidSpace.add(new BidPoint(bid, agentUtilitySpace.getUtility(bid), opponentModel.opponentBidUtility(bid)));
            iterations++;
        }
        bidSpaceUpdated = true;
    }

    /*
     * A method to update the bid space
     * This should be called whenever the opponent utility space is modified
     * @param opponentModel the estimated model for the opponent
     */
    public void updateBidSpace(Agent16OpponentModel opponentModel) {//update the opponent BidSpace
        this.opponentModel = opponentModel;
        if (bidSpace == null) {
            createBidSpace();
        } else {
            for (BidPoint bp : bidSpace) {
                bp.setOpponentUtility(this.opponentModel.opponentBidUtility(bp.getBid())); 
            } // update the opponent utility using the newest bit
        }
        bidSpaceUpdated = true;
    }

    /**
     * A method to get the bid at the nash point
     * @return the nash point bid
     */
    public Bid getNashPoint(){
        // Only compute if bid space has been updated since last computation or no Nash point exists
        if (bidSpaceUpdated || nashPoint == null) {
            bidSpaceUpdated = false;

            // Calculate pareto frontier by finding strictly dominating bids
            for (BidPoint bp : bidSpace) {
                addToFrontier(bp);
            }

            double maxUtilityProduct = -1;
            double currentUtilityProduct = 0;
            // Loop through pareto frontier and find Nash point
            for (BidPoint bp : paretoFrontier) {
                currentUtilityProduct = bp.getUtilityProduct();
                if (currentUtilityProduct > maxUtilityProduct) {
                    nashPoint = bp;
                    maxUtilityProduct = currentUtilityProduct;
                }
            }
        }

        /*if (nashPoint == null) {
        } else {
            try {
                PrintWriter pw = new PrintWriter(new File("bids.csv"));
                StringBuilder sb = new StringBuilder();
                sb.append("Agent Utility");
                sb.append(',');
                sb.append("Opponent Utility");
                sb.append(',');
                sb.append("Bid");
                sb.append(',');
                sb.append("Type");
                sb.append('\n');

                for (BidPoint bp : bidSpace) {
                    String type = "Normal";
                    if (bp.equals(nashPoint)) {
                        type = "Nash";
                    } else if (paretoFrontier.contains(bp)) {
                        type = "Pareto";
                    }
                    sb.append(bp.getAgentUtility());
                    sb.append(',');
                    sb.append(bp.getOpponentUtility());
                    sb.append(',');
                    sb.append(bp.getBid().toString().replace(',',';'));
                    sb.append(',');
                    sb.append(type);
                    sb.append('\n');
                }

                pw.write(sb.toString());
                pw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }*/
        return nashPoint == null ? null : nashPoint.getBid();
    }

    /**
     * Add a bid point to the pareto frontier by finding the strictly dominating bids
     * @param bp bid point to add
     */
    private void addToFrontier(BidPoint bp) {
        for (BidPoint f : paretoFrontier) {
            // If this bid point is dominated, it can't be added so return
            if (bp.isDominatedBy(f)) {
                return;
            }
            // If a point in the frontier is dominated, it needs replacing
            if (f.isDominatedBy(bp)) {
                removeDominated(bp, f);
                return;
            }
        }
        // Otherwise the bid must have been equal so add anyway
        paretoFrontier.add(bp);  // maybe i can delete this sentence
    }

    /**
     * Removing a dominated bid from the frontier and replacing it with the new dominating bid
     * @param bidPointToAdd the dominating bid point to add
     * @param bidPointToRemove the frontier point to remove
     */
    private void removeDominated(BidPoint bidPointToAdd, BidPoint bidPointToRemove) {
        // A bid has been dominated - the old one needs removing and the new one adding
        List<BidPoint> frontierPointsToRemove = new ArrayList<BidPoint>();
        paretoFrontier.remove(bidPointToRemove);
        // Check if any others are also dominated by this new bid
        for (BidPoint f : paretoFrontier) {
            if (f.isDominatedBy(bidPointToAdd)) {
                frontierPointsToRemove.add(f);
            }
        }
        paretoFrontier.removeAll(frontierPointsToRemove);
        paretoFrontier.add(bidPointToAdd);
    }

    /**
     * A method to get the distance of any bid to the calculated Nash point
     * @param b bid
     * @return distance of bid to Nash point
     */
    public double distanceToNash(Bid b) {
        double distance = -1;
        if (nashPoint != null) {
            double agentUtilDiff = nashPoint.getAgentUtility() - agentUtilitySpace.getUtility(b);
            double opponentUtilDiff = nashPoint.getOpponentUtility() - opponentModel.opponentBidUtility(b);
            // calculate Euclidean distance to Nash point
            distance = Math.sqrt(((Math.pow(agentUtilDiff, 2)) + (Math.pow(opponentUtilDiff, 2))));
        }
        return distance;
    }

    /**
     * A method to get the Agent Smith's utility at the Nash point
     * @return agent's utility at Nash point
     */
    public double getNashUtility() {
        return nashPoint.getAgentUtility();
    }
}
