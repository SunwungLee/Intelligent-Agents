package group16;

import genius.core.Bid;



//If the negotiation is nearing the end, the last offer if accepted to avoid a utility of 0
public class Agent16AcceptanceStrategy {

    private Agent16 agent;

    /**
     * Constructor to pass reference to the agent
     * @param agent - the reference to group16.Agent16
     */
    public Agent16AcceptanceStrategy(Agent16 agent) {
        this.agent = agent;
    }

    /**
     * A method to return if the agent should accept the current offer
     *
     * No bid below the threshold is accepted and
     * if the negotiation is near the end the last offer is accepted - an agreement is better than nothing
     * @return true = accept, false = make new offer
     */
    public boolean accept(Bid offer) {
        // Accept offer if greater than threshold or time running out (last 5% of time) to avoid 0 utility    
    	return (agent.getUtility(offer) >= agent.getUtilityThreshold()) || (agent.getTimeLine().getTime() > 0.975);
    }
}
