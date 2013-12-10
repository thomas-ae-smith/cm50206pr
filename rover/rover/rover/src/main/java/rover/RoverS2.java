package rover;

import java.util.Stack;
import org.iids.aos.log.Log;


public class RoverS2 extends Rover {

	// constant max values for this scenario
	private static final int SPEED = 1;
	private static final int SCAN = 3;
	private static final int LOAD = 5;

	// possible states for the FSM
	private static final int PATROL = 0;
	private static final int RETURN = 1;
	private static final int RESCUE = 2;
	private static final String[] STATES = {"Patrol", "Return", "Rescue"};

	private static final double[] PATHX = {0,0,0,0,0,0};	//populated in constructor
	private static final double[] PATHY = {0,0,0,0,0,0};	// based on scan radius

	// relative offset from current position to home, maintained by gotoXY
	private double homeX;
	private double homeY;
	// relative offset from home to target
	private Stack<Double> targetX;
	private Stack<Double> targetY;
	// current FSM state
	private int state;

	//patrol is a series of concentric hexagons
	private int rank;		//the current ring; 0 is the home base
	private int hour;		//the current side: 0:bottom-left, 1:left, 2:top-left, etc. to 5:bottom-right
	private int minute;		//the current step along that side; MINUTEMAX = rank
	private static final int HOURMAX = 6;	//the maximum number of sides
	private static final int HOURSTEP = 6;	//the number of sides to cover before incrementing rank

	public RoverS2() {
		Log.console("RoverS2 start");
		
		//username for team name
		setTeam("taes22");
		
		try {
			//set attributes for this rover
			//speed, scan range, max load
			//has to add up to <= 9
			setAttributes(SPEED, SCAN, LOAD);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//start patrolling
		state = PATROL;
		minute = 0;
		hour = -1; //haven't started yet
		rank = 0;
		
		//calculate the atomic vectors for the patrol
		//cos and sin give us the unit vector directions
		// 16/5*scan gives the distance between centres
		// by analogy to a 3:4:5 right triangle pair
		//	    /|	and the free scan range doubling
		//	 5 /_| 4
		//	   \3| 4
		//		\|
		// Log.console("Vectors:");
		for (int i = 0; i < HOURMAX; i++) {
			PATHX[i] = Math.sin(i*Math.PI/3.0) * 16.0/5.0 * SCAN;
			PATHY[i] = -Math.cos(i*Math.PI/3.0) * 16.0/5.0 * SCAN;
			// Log.console("" + i + ": " + PATHX[i] + ", " + PATHY[i]);
		}

		targetX = new Stack<Double>();
		targetY = new Stack<Double>();
	}

	@Override
	void begin() {
		//called when the world is started
		Log.console("BEGIN!");
		
		try {
			//scan the base location
			scan(SCAN);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	@Override
	void end() {
		// called when the world is stopped
		// the agent is killed after this
		Log.console("END!");
	}

	@Override
	void poll(PollResult pr) {
		// This is called when one of the actions has completed

		Log.console("Remaining Power: " + getEnergy());
		
		if(pr.getResultStatus() == PollResult.FAILED) {
			Log.console("Ran out of power...");
			return;
		}
		
		switch(pr.getResultType()) {
		case PollResult.MOVE:
			//move finished
			Log.console(STATES[state] + " move complete.");
			
			switch(state) {
				case PATROL:
					//now scan
					try {
						Log.console("Scanning...");
						scan(SCAN);
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				case RETURN:
					try {
						Log.console("Depositing...");
						deposit();
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				case RESCUE:
					try {
						Log.console("Collecting...");
						collect();
					} catch (Exception e) {
						e.printStackTrace();
						if (targetX.empty()) {
							getNextPatrolPoint();
							state = PATROL;
						} else {
							state = RESCUE;
						}
						try {
							Log.console("Moving...");
							gotoTarget();
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
					break;
			}
			
			break;

		case PollResult.SCAN:
			Log.console(STATES[state] + " scan complete");

			for(ScanItem item : pr.getScanItems()) {
				if(item.getItemType() == ScanItem.RESOURCE) {
					Log.console("Resource found at: " + item.getxOffset() + ", " + item.getyOffset());
					//store offset relative to home
					targetX.push(item.getxOffset() - homeX);
					targetY.push(item.getyOffset() - homeY);
					//switch into rescue mode and continue
					state = RESCUE;
				}
			}
			//if we didn't find anything
			if (targetX.empty()) {
				getNextPatrolPoint();
			}

			//go to the found item if extant, or the target otherwise
			try {
				Log.console("Moving...");
				gotoTarget();
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case PollResult.COLLECT:
			Log.console("Collect complete.");

			if (getCurrentLoad() < LOAD) {
				try {
					Log.console("Collecting more...");
					collect();
				} catch (Exception e) {
					e.printStackTrace();
					state = RETURN;
				}
			} else {
				state = RETURN;
			}
			
			if (state == RETURN) {
				try {
					Log.console("Returning home...");
					targetX.push(0.0);
					targetY.push(0.0);
					gotoTarget();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			break;
		case PollResult.DEPOSIT:
			Log.console("Deposit complete.");

			if (getCurrentLoad() > 0) {
				try {
					Log.console("Depositing more...");
					deposit();
					return;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (targetX.empty()) {
				getNextPatrolPoint();
				state = PATROL;
			} else {
				state = RESCUE;
			}
		
			try {
				Log.console(STATES[state] + " some more...");
				//(we should already be at home, here, but just in case add the home offset...)
				gotoTarget();
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
		
	}

	void gotoTarget() throws Exception {
		//C = B-A, or something like that. Vectors.
		double xOffset = targetX.pop() + homeX;
		double yOffset = targetY.pop() + homeY;
		homeX -= xOffset;
		homeY -= yOffset;
		move(xOffset, yOffset, SPEED);
	}

	void getNextPatrolPoint() {
		//step to the next minute
		minute += 1;

		if (minute >= rank) {
			minute = 0;
			hour = (hour + 1) % HOURMAX;
			if (hour % HOURSTEP == 0) {
				rank += 1;
			}
		}
		targetX.push(rank * PATHX[hour] + minute * PATHX[(hour+2)%HOURMAX]);
		targetY.push(rank * PATHY[hour] + minute * PATHY[(hour+2)%HOURMAX]);

		if (Math.abs(targetX.peek()) > 0.6 * getWorldWidth() || Math.abs(targetY.peek()) > 0.6 * getWorldHeight()) {
			Log.console("Skipping overlapped patrol node.");
			getNextPatrolPoint();
		}
		// Log.console("It's day " + rank + " at " + hour + ":" + minute + "; going to " + targetX + ", " + targetY);

	}

}
