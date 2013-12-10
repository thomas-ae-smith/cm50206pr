package rover;

import java.util.List;
import java.util.Stack;
import java.util.Random;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import org.iids.aos.log.Log;
import org.iids.aos.directoryservice.*;
import org.iids.aos.messagecenter.Envelope;
import org.iids.aos.exception.AgentScapeException;
import org.iids.aos.systemservices.communicator.structs.AgentHandle;


public class RoverS5 extends Rover {

	// constant max values for this scenario
	private static final int SPEED = 4;
	private static final int SCAN = 4;
	private static final int LOAD = 1;


	private static final int AGENTS = 10;
	private static final int SCOUTS = 6;

	// message flags
	private static final String AVAILABLE = "available";
	private static final String LOCATION = "location";
	private static final String BEQUEATH = "bequeath";

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

	// comms data
	private int id;
	private boolean scout;
	private ArrayList<AgentHandle> handles;

	//patrol is a series of concentric hexagons
	private int rank;		//the current ring; 0 is the home base
	private int hour;		//the current side: 0:bottom-left, 1:left, 2:top-left, etc. to 5:bottom-right
	private int minute;		//the current step along that side; MINUTEMAX = rank
	private static final int HOURMAX = 6;	//the maximum number of sides

	public RoverS5() {
		Log.console("RoverS5 start");
		
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
		Log.console("BEGIN at " + hour + "!");
		
		try {
			//scan the base location
			if (scout) {
				scan(SCAN);
			} else {
				send(AVAILABLE + ",true");
			}
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

		// Log.console("Remaining Power: " + getEnergy());
		
		if(pr.getResultStatus() == PollResult.FAILED) {
			Log.console("Ran out of power...");
			if (!handles.isEmpty()) {
				sendOne(BEQUEATH + "," + rank + "," + hour + "," + minute, handles.get(new Random().nextInt(handles.size())));
			}
			return;
		}
		
		switch(pr.getResultType()) {
		case PollResult.MOVE:
			//move finished
			// Log.console(STATES[state] + " move complete.");
			
			switch(state) {
				case PATROL:
					//now scan
					try {
						// Log.console("Scanning...");
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
						// e.printStackTrace();
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
			// Log.console(STATES[state] + " scan complete");

			for(ScanItem item : pr.getScanItems()) {
				if(item.getItemType() == ScanItem.RESOURCE) {
					Log.console("Resource found at: " + item.getxOffset() + ", " + item.getyOffset());
					//store offset relative to home
					if (!handles.isEmpty()) {
						sendOne(LOCATION + "," + (item.getxOffset() - homeX) + "," + (item.getyOffset() - homeY), handles.get(new Random().nextInt(handles.size())));
					} else {
						targetX.push(item.getxOffset() - homeX);
						targetY.push(item.getyOffset() - homeY);
						//switch into rescue mode and continue
						state = RESCUE;
					}
				}
			}
			//if we didn't find anything
			if (targetX.empty()) {
				getNextPatrolPoint();
			}

			//go to the found item if extant, or the target otherwise
			try {
				// Log.console("Moving...");
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
			state = PATROL;
			if (!targetX.empty()) {
				state = RESCUE;

			} else if (!scout) {
				send(AVAILABLE + ",true");
				return;
			} else {
				getNextPatrolPoint();
			}

			try {
				// Log.console(STATES[state] + " some more...");
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
		int zig = (rank % 2 == 0)? -1 : 1;

		minute += 1;

		if (minute >= rank) {
			minute = 0;
			hour = (hour + zig) % HOURMAX;
			rank += 1;
		}
		targetX.push(rank * PATHX[(HOURMAX+hour)%HOURMAX] + minute * PATHX[(HOURMAX + hour + 2*zig)%HOURMAX]);
		targetY.push(rank * PATHY[(HOURMAX+hour)%HOURMAX] + minute * PATHY[(HOURMAX + hour + 2*zig)%HOURMAX]);

		if (Math.abs(targetX.peek()) > 0.6 * getWorldWidth() || Math.abs(targetY.peek()) > 0.6 * getWorldHeight()) {
			Log.console("Skipping overlapped patrol node.");
			getNextPatrolPoint();
		}
		// Log.console("It's day " + rank + " at " + hour + ":" + minute + "; going to " + targetX + ", " + targetY);
	}

	//The run method of the agent
	@Override
	public void run() {
		super.run();
		// Log.console("I'm running.");

		DirectoryService ds = null;
		DSRecordIdentifier rid = null;
		
		try {
			//bind to the DS
			ds = this.getServiceBroker().bind(DirectoryService.class);
			 
			//create a new record in the DS
			rid = new DSRecordIdentifier(getPrimaryHandle());
			ds.createRecord(rid);
			
			//add team name and handle to this entry
			ds.addEntry(rid, "team", getTeam());
			ds.addEntry(rid, "handle", getPrimaryHandle());
	 
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
		
		// Log.console("I've added myself.");
		handles = new ArrayList<AgentHandle>();

		//keep checking the DS until the correct number of agents have been found
		while(handles.size() < AGENTS) {

			handles.clear();

			try {
				
				//search the DS for records with our team
				Collection<DSRecord> result;
				result = ds.search("team=" + getTeam());
				
				for (DSRecord record : result) {
					//add the handle from each record to the list
					handles.add((AgentHandle) record.getValue("handle"));
				}
				
			} catch (QuerySyntaxException e) {
				e.printStackTrace();
				return;
			}

			try {
				//sleep for a bit
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}

		// Log.console("I've found all the agents.");
		//now remove the records from the DS
		//so we can restart the world and not find
		//old agents
		try {
			ds.deleteRecord(rid);
		} catch (DirectoryServiceException e1) {
			e1.printStackTrace();
			return;
		}

		//sort the array
		Collections.sort(handles, new Comparator<AgentHandle>() {
			@Override
			public int compare(AgentHandle o1, AgentHandle o2) {
				return o1.toString().compareTo(o2.toString());
			}			
		});

		for (AgentHandle h : handles) {
			if (h.equals(getPrimaryHandle())) {
				id = handles.indexOf(h);
			}
		}

		Log.console("I'm agent " + id);
		hour = (id)+1; //haven't started yet

		if (id < SCOUTS) {
			scout = true;
			handles.clear();
		} else {
			scout = false;
		}

		listenForMessages();
	}

	void listenForMessages() {
	
		//keep listening while agent is running
		while(agentRunning()) {
			
			try {
				//non-blocking call to receive.
				List<Envelope> envs = receiveMessages(false);
				for(Envelope env : envs) 
				{
					// Log.console("Message: " + env.getData() + ":" + id);
					String[] bits = (""+env.getData()).split(",");

						if (bits[0].equals(AVAILABLE) && bits[1].equals("true")) {
							handles.add(env.getFromHandle());
						} else if (bits[0].equals(AVAILABLE) && bits[1].equals("false")) {
							handles.remove(env.getFromHandle());
						}
					if (!scout) {

						if(bits[0].equals(LOCATION)) {
							targetX.push(Double.parseDouble(bits[1]));
							targetY.push(Double.parseDouble(bits[2]));
						} else if (bits[0].equals(BEQUEATH)) {
							Log.console("Go on, " + id);
							rank = Integer.parseInt(bits[1]);
							hour = Integer.parseInt(bits[2]);
							minute = Integer.parseInt(bits[3]);
							getNextPatrolPoint();
						}

					}
					
				}
			} catch (AgentScapeException e) {
				e.printStackTrace();
				break;
			}
			
			try {
				//sleep for a bit
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}

			if (!scout && !targetX.empty() && state == PATROL) {
				Log.console("I'm going for it " + id);
				state = (rank == 0)? RESCUE : PATROL;
				scout = (rank == 0)? false : true;
				send(AVAILABLE + ",false");
				try {
					// Log.console("Moving...");
					gotoTarget();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}
		
	}

	void send(String msg) {
		try {
			Envelope out = new Envelope();
			//identify my handle
			AgentHandle myHandle = getPrimaryHandle();
			//set data
			out.setData(msg);
			//set handle
			out.setFromHandle(myHandle);
			//add each agent's handle in time
			for(AgentHandle ag : handles){
				//send if not myself
				if(! ag.equals(myHandle)){
					//create an envelop for each
					out.addTarget(ag);
				}
			}
			
			sendMessage(out);
			//sleep for a bit
			Thread.sleep(100);
		} catch (AgentScapeException e){
			   Log.console("Problem sending message, caused by " +
						e.getCause().getClass().getName() + ", reason: "
						+ e.getMessage());
		}catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	void sendOne(String msg, AgentHandle ag) {
		Log.console("Go get " + msg);
		try {
			Envelope out = new Envelope();
			//identify my handle
			AgentHandle myHandle = getPrimaryHandle();
			//set data
			out.setData(msg);
			//set handle
			out.setFromHandle(myHandle);
			out.addTarget(ag);
			
			sendMessage(out);
			//sleep for a bit
			Thread.sleep(100);
		} catch (AgentScapeException e){
			   Log.console("Problem sending message, caused by " +
						e.getCause().getClass().getName() + ", reason: "
						+ e.getMessage());
		}catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
