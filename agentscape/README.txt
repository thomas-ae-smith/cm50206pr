===================================
AgentScape
http://www.iids.org
http://systemsdesign.tbm.tudelft.nl
http://www.d-cis.nl
===================================


Introduction
------------

The main goal of the AgentScape project is the development
of a large-scale, secure, open, distributed multi agent platform.
In the AgentScape project many partners have joined forces, including
the NLNet Foundation, Delft University of Technology, D-CIS Lab,
University of Bath, University of Warwick, Cardiff University, and the
Book Depository Ltd.

Many versions of AgentScape have been released over the years.
Currently, AgentScape is in the process of a complete rewrite.
Even though the current AgentScape reuses much of the existing
code of AgentScape, they are not binary compatible. However, AgentScape
applications (later than version 0.9.2) will probably only need minor
modifications to be able to run on the current AgentScape.

NOTE: More information on agent development can be found on the AgentScape
      website: http://www.agentscape.org.


AgentScape Middleware
---------------------

  AgentScape is a middleware system that provides support for
  large-scale, distributed multi-agent systems. Initially Agentscape
  was a research project at the Intelligent Interactive
  Distributed Systems Group, Section Computing Systems, Department of
  Computer Science, Vrije Universiteit Amsterdam. This research is
  funded by Stichting NLnet (NLnet Foundation, www.nlnet.nl). In the
  current state, AgentScape is designed and developed by many partners,
  foremost the Delft University of Technology (TU-Delft) and D-CIS Lab.


Requirements for Running AgentScape
-----------------------------------

  Some version of this software has been found to work on the following
  platforms:

    Linux (x86/32-bit and x86/64-bit)
    MacOS X 10.6 (Intel) and later
    Solaris 8 and 9 (Sparc), 10 (x86)
    Windows XP, Vista, 7

  AgentScape is pure Java, so getting it to run on another platform 
  should require relatively little effort.

  Before installation of AgentScape, you must make sure you have
  (already) installed the following software:

  * Java, at least version 1.5 (Standard Edition)
    Running AgentScape only requires the java runtime environment. 
    Rebuilding AgentScape requires the java development kit.
    See: http://java.sun.com/downloads/

  If you want to rebuild AgentScape, you will also need:
  
  * Apache Maven (tested with 2.2.1)

AgentScape world
----------------

  An 'AgentScape world' consists of a 'lookup service' and zero or
  more 'AgentScape locations' (each identified by a 'location ID').
  Each AgentScape location consists of one or more 'AgentScape
  platforms' (see Fig. 1). An AgentScape platform runs on a single
  host and consists of an 'AOS kernel' (a Java process) and several
  supporting 'AgentScape system services' (each a Java process). An
  agent is initially 'injected' into a location. From its initial
  location it may migrate to other locations. At each location the
  agent physically runs on one of the AgentScape platforms of the
  location. Which platform in a particular location actually hosts
  the agent is determined by the location, and is beyond the agent's
  control.

  An AgentScape world is defined by its lookup service. Locations and
  platforms that exist in a particular AgentScape world are able to
  find each other by means of the lookup service of that world.
  Locations and platforms in different AgentScape worlds are not
  visible to one another.


      +-----------------------------------------------------+
      | Location A                                          |
      |  +------------+   +------------+    +------------+  |
      |  | AgentScape |   | AgentScape |    | AgentScape |  |
      |  +------------+   +------------+    +------------+  |
      |  +------------+   +------------+    +------------+  |
      |  | Windows 2k |   |   Linux    |    | Solaris 9  |  |
      |  +------------+   +------------+    +------------+  |
      |                                                     |
      +-----------------------------------------------------+


             +-----------------------------------------------------+
             | Location B                                          |
             |  +------------+   +------------+    +------------+  |
             |  | AgentScape |   | AgentScape |    | AgentScape |  |
             |  +------------+   +------------+    +------------+  |
             |  +------------+   +------------+    +------------+  |
             |  |   Linux    |   | Solaris 9  |    |   Linux    |  |
             |  +------------+   +------------+    +------------+  |
             |                                                     |
             +-----------------------------------------------------+


        Figure 1: AgentScape platforms organized in locations.


  For more background information, please have a look at the AgentScape
  website, which contains extensive documentation.


Quickstart on using AgentScape
------------------------------

  In the 2.0 release there is no GUI for startup (yet). To start 
  AgentScape and to run agents, you need to use the commandline
  for now. From the installation directory, you can execute several
  administrative commands.

  These commands are located in the installation directory in the
  'lib' folder.


  lib/asstartup.jar   (starts an AgentScape host/location)
  -----------------

  This command can be used to start a new host and location. The
  most basic use of this command is:


    java -jar lib/asstartup.jar my_location


  which stars a new location 'my_location' and a single host
  manager in the location. Locations in AgentScape can consist of
  multiple hosts. If you do not want to start a new location, but
  to start a host in an existing location, use

 
    java -jar lib/asstartup.jar -j my_location


  The '-j' switch joins an existing location. To read more about hosts and
  locations, or other AgentScape concepts, please visit the website.

 
  lib/injector.jar    (starts an agent on a location)
  ----------------

  Agents can be started as soon as a location is up and running. The agents
  can be 'injected' into a running location using the injector command. Some
  of the demo agents are available in the agents/ directory.

  To inject one of the agents in the new location, use:


    java -jar lib/injector.jar agents/agent-demo-migration.jar my_location


  For more details see the sections below.


Running an AgentScape world
---------------------------
  
  You have a choice between joining the AgentScape world that we keep
  running at agentscape.org, or creating your own. The address of the
  default lookup server is: http://lookup2.agentscape.org.
  
  To run your own AgentScape world all you need to do is start a 
  'lookup service'. You can run the Lookup Server, as follows:


    java -jar lib/aslookup.jar [ [ host ] portnr ]


  By default, the lookup server will attempt to listen on the public IP
  address of the local host machine on port 80 (http://<your-ip>:80). 
  Note that use of port 80 may require super-user permissions. You can change
  the default listen port by specifying it on the commandline.

  All hosts & locations that connect to the same lookup service are able
  to find each other. Hosts using a different lookup service cannot be
  seen.

  To tell AgentScape which lookup service to use, supply the following
  JVM argument to asstartup:


    java -Dorg.iids.aos.lookup=http://yourhost:port -jar asstartup.jar my_location


  As always, for more information, visit the documentation section on the
  project website: www.agentscape.org/documentation



Monitoring Agents
-----------------

  Querying the lookup server with a browser will show all the agents.
  Type the address of the lookup server into a browser. The lookup server
  will show all known locations and agents. However, the information is not
  meant for humans directly, so the information may look a little cryptic.

  If you type http://<lookup_server_host>/agents in a browser, you will
  get a nice overview of all known agents. Note however that the information
  in the lookup server may be outdated as information from the lookup server
  is never deleted, but is allowed to `expire'. Therefore, agents that
  have already stopped may still be listed in the lookupserver for a
  while until their informatin expires (usually within a minute or so).


Servlets
--------


  AgentScape provides some support for agents to provide servlets to users.
  This requires the 'Servlet service' to be installed and activated. Once
  this is done, the agent servlets can be accessed through a web browser.

  Point your browser to http://localhost:8008, which will show the available
  servlets of the agents running on the local host. 

  For more on agents and an example, see the AgentScape documentation
  at http://www.agentscape.org.


Demo Agents
-----------

  The AgentScape distribution includes a number of example Java
  agents, which you may find in ./agents. Some agents are able to
  migrate to different AgentScape locations, others are stationary.
  Some agents pop up a GUI when they arrive at an AgentScape platform.
  Note that such a GUI is only visible at that particular platform.

  Chat agent
  ----------

    Messenger agents in the same AgentScape world send and receive
    text messages on behalf of users.
    
    When you run a messenger agent, a GUI appears. After you have
    entered your name, you will be able to receive messages from other
    users.

    To send a message to some other user, select the user from the
    'Destination ID' drop-down menu, and enter the text you wish to
    send in the 'Your message' field, then press 'Send'. The other
    user will see the message in the 'Received messages' field. Note
    that currently the sender of a received message is not properly
    identified.

    This agent does not migrate.

  migrating agent
  ---------------

    This agent demonstrates migration. When a user runs a
    migratingagent, a GUI appears. The user can select an 'ID of
    destination location' to migrate to and press 'Migrate' to go there.
    When the agent arrives at the chosen location, a GUI will appear
    at that location. Additionally, an 'auto-migration' feature allows
    a user to migrate the agent between the current location, and the
    destination location automatically a given number of times. To
    this end, the user must enter a '# of hops' value in the gui,
    ranging between 0 and 50.


  fishtank
  --------

    Fishtank puts a fishtank on the screen in which fish can swim.
    Periodically, a fish will look for fishtanks in other agentscape
    locations and migrate to them.

    First, start one and only one(!) fishtank at each location. Then
    insert fish from within the fishtank agent. You can insert as many 
    fish as you like at each location. When you start a fish a 
    default "gif"-image will be used to draw the fish in a fishtank. 
    You can use your own fish-images by selecting a new fish image. 


  Developing other Agents
  -----------------------

    A brief tutorial for agent development is available on the 
    website of agentscape: http://www.agentscape.org

    This tutorial briefly explains the basics of agent development
    including a few simple examples, available in ./agents

    To develop a new agent we recommend modelling the agent after one
    of the existing agents.



Rebuilding AgentScape
---------------------

  Building AgentScape 2.0 is done using Maven. In principle, the only
  commands you need to know are:

  
    'mvn install'  to build jar files and install them in your local repository

    'mvn clean'    to clean up built files and directories


  In addition, mvn install also installs the jar files in a distribution folder
  on your local machine. This will by default be at $HOME/agentscape, but it can
  be specified when installing.

    mvn install -Dorg.iids.aos.install.home=your-install-folder

  To avoid running the tests, you can add -DskipTests as well.


  The source distribution contains the following components:

    aos-util/                      Some basic utilities   
    aos-kernel/                    Implementation of AOS kernel
    agentscape-core/               Core AgentScape components (system services, etc)
    agentscape-example-agents/     Sources for the example and demo agents
    agentscape-dist/               POM files that create a distribution



Questions
---------

  Please submit questions to agentscape@iids.org and/or
  visit the forums at http://www.agentscape.org/forum.

  Have fun!
