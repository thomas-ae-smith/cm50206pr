package rover;

import java.util.Random;

import org.iids.aos.log.Log;


public class RoverS1scan extends Rover {

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
	private double targetX;
	private double targetY;
	// current FSM state
	private int state;

	//patrol is a series of concentric hexagons
	private int rank;		//the current ring; 0 is the home base
	private int hour;		//the current side: 0:bottom-left, 1:left, 2:top-left, etc. to 5:bottom-right
	private int minute;		//the current step along that side; MINUTEMAX = rank
	private static final int HOURMAX = 6;	//the maximum number of sides
	private static final int HOURSTEP = 6;	//the number of sides to cover before incrementing rank

	public RoverS1scan() {
		Log.console("RoverS1scan start");
		
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
			
			//now scan
			try {
				Log.console("Scanning...");
				scan((state == PATROL)? SCAN : 1);	//full scan if patrolling, narrow otherwise
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			break;

		case PollResult.SCAN:
			Log.console(STATES[state] + " scan complete");

			//temp variable for if we find something
			ScanItem si = null;
			//if we're on the hunt for a resource
			if (state == PATROL || state == RESCUE) {
				for(ScanItem item : pr.getScanItems()) {
					if(item.getItemType() == ScanItem.RESOURCE) {
						Log.console("Resource found at: " + item.getxOffset() + ", " + item.getyOffset());
						//store reference to the found resource
						si = item;
						//store offset relative to home
						targetX = item.getxOffset() - homeX;
						targetY = item.getyOffset() - homeY;
						//switch into rescue mode and cease scanning (in scenario #1)
						state = RESCUE;
						break;
					}
				}
				//if we didn't find anything
				if (si == null) {
					getNextPatrolPoint();
				}
			}
			//if there should be a resource nearby
			if (state == RESCUE && si != null) {
				//check if it's worth trying to collect
				if (Math.sqrt(si.getxOffset() * si.getxOffset() + si.getyOffset() * si.getyOffset()) < .1) {
					try {
						Log.console("Collecting...");
						collect();
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				}
			} else if (state == RETURN) {	//if we should be close to home
				for(ScanItem item : pr.getScanItems()) {
					//locate the base
					if(item.getItemType() == ScanItem.BASE) {
						Log.console("Base found at: " + item.getxOffset() + ", " + item.getyOffset());
						si = item;
						break;
					}
				}
				//check if it's worth trying to deposit
				if (Math.sqrt(si.getxOffset() * si.getxOffset() + si.getyOffset() * si.getyOffset()) < .1) {
					try {
						Log.console("Depositing...");
						deposit();
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				}
			}

			//go to the found item if extant, or the target otherwise
			try {
				Log.console("Moving...");
				gotoXY((si != null)? si.getxOffset() : homeX+targetX, (si != null)? si.getyOffset() : homeY+targetY);
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
					gotoXY(homeX, homeY);
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
				} catch (Exception e) {
					e.printStackTrace();
					state = RESCUE;
				}
			} else {
				state = RESCUE;
			}
			
			if (state == RESCUE) {
				try {
					Log.console("Rescuing more...");
					//(we should already be at home, here, but just in case add the home offset...)
					gotoXY(homeX+targetX, homeY+targetY);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			break;
		}
		
	}

	void gotoXY(double xOffset, double yOffset) throws Exception {
		//C = B-A, or something like that. Vectors.
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
		// targetX = rank * PATHX[hour];
		targetX = rank * PATHX[hour] + minute * PATHX[(hour+2)%HOURMAX];
		// targetY = rank * PATHY[hour];
		targetY = rank * PATHY[hour] + minute * PATHY[(hour+2)%HOURMAX];
		// Log.console("It's day " + rank + " at " + hour + ":" + minute + "; going to " + targetX + ", " + targetY);

	}

}
