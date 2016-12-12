package com.ai.alg;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.ai.model.*;
import com.ai.sim.*;
import com.ai.*;


public class SARSA extends RacetrackLearner{
    double learningRate;
    private static final double LEARNING_RATE = 0.85;
    private static final double GAMMA = 0.2;
    private static final int TIMES_TO_VISIT = 30;

    private int iterationCount = 0;
    private Map<State, Map<Action, Double>> qTable = new HashMap<>();
    private Map<State, Integer> timesVisited = new HashMap<>();
    private MDPActionSimulator aSim;
    private Policy policy;
    private int iterationLimit;

    public SARSA(Racetrack racetrack, CollisionModel collisionModel){
	super(racetrack, collisionModel);

	aSim = new MDPActionSimulator(new RacetrackMDP(racetrack, collisionModel));

	policy = new SARSAPolicy();

	iterationLimit = racetrack.getWidth()*racetrack.getHeight()*2;

	//set value of finish state to 0
    }

    class SARSAPolicy implements Policy{
	public Action getAction(State state){
	    if(Math.random() < 1/TIMES_TO_VISIT*timesVisited.getOrDefault(state,0)){//get epsilon
		double bestUtility = Double.MAX_VALUE;
		Action argMax = new Action(0,0);//
		Map<Action, Double> actions = qTable.get(state);
		for(Action action : actions.keySet()){
		    if(actions.get(action) < bestUtility) {
			argMax = action;
			bestUtility = actions.get(action);
		    }
		}
		return argMax;
	    }else{
		return getRandomAction();
	    }
	}

	public Action getRandomAction(){
	    return new Action((int)(Math.random()*2-1), (int)(Math.random()*2-1));
	}
    }
    
    //do another increment of learning
    public void next(){
	int xPos, yPos, xVel, yVel;
	Position curPos;
	Velocity curVel;
	State curState;
	Action curAction;
	List<Action> actions = new ArrayList<>();
	List<State> states = new ArrayList<>();
	//determine random starting location and velocity
	do{
	    xPos = (int)(Math.random()*racetrack.getWidth());
	    yPos = (int)(Math.random()*racetrack.getHeight());
	    curPos = new Position(xPos, yPos);
	}while(!racetrack.isSafe(curPos) || racetrack.finishLine().contains(curPos));
	
	xVel = (int)(Math.random()*11-5);
	yVel = (int)(Math.random()*11-5);
	do {
	    states.clear();
	    actions.clear();
	    
	    curPos = new Position(xPos, yPos);
	    curVel = new Velocity(xVel, yVel);
	    curState = new State(curPos, curVel);

	    //while we have not crossed the finish or reached our iteration limit
	    for(int i=0; curState != null && i<iterationLimit; i++){
		//get next action
		curAction = policy.getAction(curState);
		actions.add(curAction);

		states.add(curState);
		curState = aSim.getNextState(curState, curAction);
	    }
	} while (curState != null);

	for (int i = 0; i < states.size(); i++) {
	    State state = states.get(i);
	    Action action = actions.get(i);
	    double nextStateActionUtility = 0;

	    if (i < states.size() - 1) {
		State nextState = states.get(i + 1);
		Action nextAction = actions.get(i + 1);		
		nextStateActionUtility = qTable.get(nextState).get(nextAction);
	    }
	    
	    qTable.get(state).put(action, ((1 - LEARNING_RATE) * qTable.get(state).get(action) +
						 LEARNING_RATE * (1 + GAMMA * nextStateActionUtility)));
	}
		
    }
    public boolean finished(){
	return false;
    }

    public Policy getPolicy(){
	return policy;
    }
    public int getIterationCount(){
	return iterationCount;
    }
}