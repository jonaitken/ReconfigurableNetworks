package evolutionAspects;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Vector;

import org.python.core.PyList;

import cern.colt.function.IntComparator;

import networkcalculus.ConnectionsInformation;
import networkcalculus.DependabilityAssessment;
import networkcalculus.NetworkCalculusManager;

import ec.*;
import ec.simple.*;
import ec.util.Parameter;
import ec.vector.*;
import ec.multiobjective.MultiObjectiveFitness;
import java.util.Date;

public class ProblemSetupMoo extends Problem implements SimpleProblemForm {

	private ArrayList myPossibleConnections;
	private ArrayList myActualPhysicalConnections;
	private ArrayList myPhysicalLocations;
	private ArrayList myServerConnections;
	private ArrayList myDeviceNames;
	private NetworkCalculusManager myNCManager;
	private Vector<String> vectorSources;
	private Vector<String> vectorSinks;
	private Vector<String> vectorRouters;
	private ArrayList arrayListSources;
	private ArrayList arrayListSinks;
	private ArrayList arrayListRouters;
	private int intTotalConnections;
	private double doublePropSpeed = 2e8;
	private double doubleLengthFiddleFactor = 1.5;
	private PrintStream printStream;
	
	private static boolean boolSetVals=false;
	
	private static int intCurrentJob=-1;

	private static int intSmallestNetwork = -1;
	private static int intFirstNetwork = -1;
	private static boolean booleanFoundNetwork = false; //Don't change this!
	private static double doubleDelta = 0;
	private static double doubleIndividual=0;
	
	private static double doubleDeltaSize=0;
	
	private static double doubleShortestNetwork = -1;
	private static double doubleFirstNetwork = -1;
	private static long lDateTime;

	public void setup(EvolutionState state, Parameter base) {
		
		//Reset everything...
		boolSetVals=false;
				
		intSmallestNetwork = -1;
		intFirstNetwork = -1;
		booleanFoundNetwork = false; //Don't change this!
		doubleDelta = 0;
		doubleIndividual=0;
		
		doubleDeltaSize=0;
		
		doubleShortestNetwork = -1;
		doubleFirstNetwork = -1;
		
		super.setup(state, base);
		
		this.myNCManager = new NetworkCalculusManager();
		myNCManager.evolutionaryLoopPreparation();
		ArrayList myPossibleConnections = myNCManager.getTotalInterfaces();
		this.myPossibleConnections = new ArrayList(myPossibleConnections);
		this.myActualPhysicalConnections = myNCManager.getPhysicalConnections();
		this.myPhysicalLocations = myNCManager.getLocations();
		this.myServerConnections = myNCManager.getServerConnections();
		this.myDeviceNames = myNCManager.getDeviceNames();
		//System.out.println("SCs: "+this.myServerConnections);
		//System.out.println(this.myPhysicalLocations);
		
		File file = new File("Results//job_info.log");
		try {
			this.printStream = new PrintStream(new FileOutputStream(file));
			System.setOut(this.printStream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Basic Info:");
		System.out.println("Number of Devices: "+this.myDeviceNames.size());
		System.out.println("Number of Connection Ports: "+this.myPossibleConnections.size());

		this.vectorSinks = this.findSinks(this.myServerConnections);
		this.vectorSources = this.findSources(this.myServerConnections);
		
		this.arrayListSinks=this.convertToDevices(this.vectorSinks, this.myDeviceNames);
		this.arrayListSources=this.convertToDevices(this.vectorSources, this.myDeviceNames);
		this.arrayListRouters=this.findRouters(myDeviceNames, this.arrayListSources, this.arrayListSinks);
		
		//System.out.println("Sinks " + vectorSinks);
		//System.out.println("Sources " + vectorSources);
		
		
		System.out.println("Number sinks: "+this.arrayListSinks.size());
		System.out.println("Number sources: "+this.arrayListSources.size());
		System.out.println("Number routers: "+this.arrayListRouters.size());
		
		System.out.println("Number connections: "+this.myActualPhysicalConnections.size()/2);
		
		this.intTotalConnections = vectorSinks.size() + vectorSources.size();
		System.out.println("Total Software Pair Connections: " + this.myServerConnections.size());
		
		System.out.println("");
		System.out.println("Devices: "+this.myDeviceNames);
		System.out.println("Sinks: " + this.arrayListSinks);
		System.out.println("Sources: " + this.arrayListSources);
		System.out.println("Connections: " + this.myServerConnections);
		
		int intMaxPossibleAddress = myPossibleConnections.size() - 1;
		state.parameters.set(new Parameter("pop.subpop.0.species.max-gene"),
				new Integer(intMaxPossibleAddress).toString());
		state.parameters.set(new Parameter("pop.subpop.0.species.min-gene"),
				new Integer(-1).toString());
		
		// state.parameters.set(new
		// Parameter("pop.subpop.0.species.genome-size"), new
		// Integer(myActualPhysicalConnections.size()).toString());
		//System.out.println(state.parameters.toString());
		//System.out.println("Connections:" + this.myPossibleConnections);
		
		
		lDateTime = new Date().getTime();
	    //System.out.println("Time: " + lDateTime);
	}

	public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation, final int threadnum) {
		
		//System.out.println(ProblemSetupMoo.doubleIndividual);

		if(ProblemSetupMoo.intCurrentJob!=(((Integer)(state.job[0])).intValue())) {
			File file = new File("Results//log"+(((Integer)(state.job[0])).intValue())+".log");
			try {
				this.printStream = new PrintStream(new FileOutputStream(file));
				System.setOut(this.printStream);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ProblemSetupMoo.intCurrentJob=(((Integer)(state.job[0])).intValue());
		}
		
        if(ProblemSetupMoo.boolSetVals==false) {
        	System.err.println("Setting");
        	int[] genomeToAlter = ((IntegerVectorIndividual)ind).genome;
        	int[] alteredGenome = this.setupGenomeOnActualconnections(this.myActualPhysicalConnections, this.myPossibleConnections,genomeToAlter.length);
        	((IntegerVectorIndividual)ind).genome=alteredGenome;
        	ProblemSetupMoo.boolSetVals=true;
        	System.err.println("Set");
        }
		
		
		if (ind.evaluated) return;

        if (!(ind instanceof IntegerVectorIndividual))
            state.output.fatal("Whoa!  It's not a IntegerVectorIndividual!!!",null);
        
        IntegerVectorIndividual ind2 = (IntegerVectorIndividual)ind;
        //System.out.println("");
        //System.out.println("Genome: ");
        //for(int intCounter=0;intCounter<ind2.genome.length;intCounter++) {
        //	System.out.println(ind2.genome[intCounter]);	
        //}

        double rawfitness = 0;
        double doubleConnectionsPoint=0;
        float[] fitnesses = ((MultiObjectiveFitness)ind.fitness).getObjectives();
        int[] individualGenome = this.processGenome(ind2.genome);
        //Evaluate Fitness
        ArrayList myGenomeConnectionList=new ArrayList();
        
        ArrayList<String[]> myDevicesConnections=new ArrayList<String[]>();
        
        
        PyList pyListInternalConnection = new PyList();
        String stringSource=new String();
        String stringDest=new String();
        String stringSourceDevice=new String();
        String stringDestDevice=new String();
        double doubleTotalDistance=0;
        for(int x=0; x<individualGenome.length; x++){
        	//System.out.println("STR: "+individualGenome[x]);
        	if(x%2==0) {		
        		stringSource=(String)((PyList)this.myPossibleConnections.get(individualGenome[x])).get(1);
        		stringSourceDevice=(String)((PyList)this.myPossibleConnections.get(individualGenome[x])).get(0);
        		//System.out.println("SS: "+stringSource);
        		
        		//This is a source
        	}
        	if(x%2==1) {
        		stringDest=(String)((PyList)this.myPossibleConnections.get(individualGenome[x])).get(1);
        		stringDestDevice=(String)((PyList)this.myPossibleConnections.get(individualGenome[x])).get(0);
        		//System.out.println("SD: "+stringDest);
        		
        		//Forward Connection
        		int intPosition1=this.findPairInArrayList(myDevicesConnections, stringSourceDevice, stringDestDevice);
        		
        		//Reverse Connection - We'll add this later before everything gets packaged up.
        		int intPosition2=this.findPairInArrayList(myDevicesConnections, stringDestDevice, stringSourceDevice);
        		
        		
        		//System.out.println(stringSourceDevice);
        		//System.out.println(stringDestDevice);
        		//System.out.println(intPosition1);
        		//System.out.println(intPosition2);
        		boolean boolIsOriginal=false;
        		if(intPosition1<0 && intPosition2<0) {
        			String[] newConnection=new String[2];
        			newConnection[0]=stringSourceDevice;
        			newConnection[1]=stringDestDevice;
        			myDevicesConnections.add(newConnection);
        			boolIsOriginal=true;
        		}
        		
        		//Check this isn't an internal loop, if it is bin it.
        		if(!stringDestDevice.contains(stringSourceDevice)&&boolIsOriginal) {
        			//Approximate Connection delay:
        			double[] doubleSourcePosition=this.findPosition(this.myPhysicalLocations, stringSourceDevice);
            		double[] doubleDestPosition=this.findPosition(this.myPhysicalLocations, stringDestDevice);
        			
            		double doubleDistance=this.calculateDistance(doubleSourcePosition[0], doubleSourcePosition[1], doubleDestPosition[0], doubleDestPosition[1]);
            		doubleTotalDistance+=doubleDistance;
            		double doubleDelay=(this.doubleLengthFiddleFactor)*doubleDistance/(this.doublePropSpeed);
            		
        			pyListInternalConnection.add(stringSource);
            		pyListInternalConnection.add(stringDest);
            		pyListInternalConnection.add((float)10e6);
            		pyListInternalConnection.add(doubleDelay);
            		myGenomeConnectionList.add(pyListInternalConnection);
            		pyListInternalConnection = new PyList();
            		
            		//Check to see whether neccessary connections are present.
            		
            		if(this.isInVector(this.vectorSources, stringSourceDevice)||this.isInVector(this.vectorSinks, stringSourceDevice)) {
            			doubleConnectionsPoint+=(1/(double)this.intTotalConnections);
            		}
            		
            		if(this.isInVector(this.vectorSources, stringDestDevice)||this.isInVector(this.vectorSinks, stringDestDevice)) {
            			doubleConnectionsPoint+=(1/(double)this.intTotalConnections);
            		}
            		
            		//Setup and add reverse Connection
            		pyListInternalConnection.add(stringDest);
            		pyListInternalConnection.add(stringSource);
            		pyListInternalConnection.add((float)10e6);            		
            		pyListInternalConnection.add(doubleDelay);
            		myGenomeConnectionList.add(pyListInternalConnection);
            		pyListInternalConnection = new PyList();
        		}
        	}
        	
        }
        //System.out.println("rawfit before: "+rawfitness);
        //System.out.println("doubleConnectionsPoint: "+doubleConnectionsPoint);

        //System.out.println("fitnesses bef: "+fitnesses[0]);
        fitnesses[4]=(float)doubleConnectionsPoint;
        //System.out.println("fitnesses aft: "+fitnesses[0]);
        //System.out.println("GenomeConnectionList: "+myGenomeConnectionList);
        //boolean boolValidList=this.myNCManager.evolutionaryGenerateNetwork(this.myActualPhysicalConnections);
        this.processArrayList(myGenomeConnectionList);
        
        //Once the connections are sorted we can filter for the devices that are actually part of the network!
                
        ArrayList myNetworkDevices=new ArrayList(this.myDeviceNames); //Just copies all elements in...
        //For every element in the connection list - populate the device names with all of the elements in the connection list
        myNetworkDevices=this.findDevicesByConnectionList(myGenomeConnectionList, this.arrayListSinks, this.arrayListSources, this.arrayListRouters);
        //System.err.println("Devices: "+myNetworkDevices.toString()); 
        
 
		             
        boolean boolValidList=this.myNCManager.evolutionaryGenerateNetwork(myGenomeConnectionList, myNetworkDevices);
        //rawfitness=0;
        if(!boolValidList) {
        	//See how well matched the network actually is to the servers and sources that are in place and score the fitness 
        	//appropriately intTotalConnections=Fully Connected
        	//rawfitness/=doubleTotalDistance;
        }
        else {
        	//Have a network that can be simulated across all single point failures.
        	//However this doesn't mean that all of the nodes are connected end to end - flow's can still be sourced but not sinked
        	//Any partial route that exists will present a null when myConnectionsInformation.getResults() is called
        	//doubleTotalDistance contains the complete cabling length;
        	int intDepCounter=0;
        	int intTotalDeps=0;
        	
        	int intTotalHeadRoomCounter=0;
        	int intHeadroomFailedCounter=0;
        	
        	double[] doubleHeadroom=new double[5];
        	double doubleDelayParam=0;
        	boolean boolMeetsHeadroom=true;
        	
        	Vector<DependabilityAssessment> vectorGenomeDependability=this.myNCManager.getDependabilityAssessment();
			for(int intCounterDepenabilityAssessment=0;intCounterDepenabilityAssessment<vectorGenomeDependability.size();intCounterDepenabilityAssessment++) {
				DependabilityAssessment myDependabilityAssessment=vectorGenomeDependability.elementAt(intCounterDepenabilityAssessment);
				Vector<ConnectionsInformation> vectorConnectionsInformation = myDependabilityAssessment.getConnectionsInformation();
				for(int intCounterConnectionsInformation=0;intCounterConnectionsInformation<vectorConnectionsInformation.size();intCounterConnectionsInformation++) {
					ConnectionsInformation myConnectionsInformation = vectorConnectionsInformation.elementAt(intCounterConnectionsInformation);
					//System.out.println(myConnectionsInformation.getStringDest());
					intTotalDeps++;
					intTotalHeadRoomCounter++;
					if(myConnectionsInformation.getResults()==null) {
						intDepCounter++;
					}
					else {
						doubleHeadroom[0]=myConnectionsInformation.getCharnyHeadroom();
						doubleHeadroom[1]=myConnectionsInformation.getFairQueueingHeadroom();
						doubleHeadroom[2]=myConnectionsInformation.getPMOOAnalysisHeadroom();
						doubleHeadroom[3]=myConnectionsInformation.getSeparatedFlowHeadroom();
						doubleHeadroom[4]=myConnectionsInformation.getTotalFlowHeadroom();
						doubleDelayParam+=doubleHeadroom[4];
						
						if(doubleHeadroom[4]<0) {
							boolMeetsHeadroom=false;
							intHeadroomFailedCounter++;							
						}
						//System.out.println(myConnectionsInformation.getTotalFlowHeadroom());
						//System.out.println("0: "+doubleHeadroom[0]+" 1: "+doubleHeadroom[1]+" 2: "+doubleHeadroom[2]+" 3: "+doubleHeadroom[3]+" 4: "+doubleHeadroom[4]);
					}
				}
			}
			
			if(boolMeetsHeadroom==false) {
				fitnesses[3]=0+(float)(1-(intHeadroomFailedCounter/intTotalHeadRoomCounter));
				doubleDelayParam=0;
			}
			else {
				fitnesses[3]=(float)doubleHeadroom[4]+(float)(1-(intHeadroomFailedCounter/intTotalHeadRoomCounter));
				doubleDelayParam=1;
			}
			
			int intNetworkSize=(int)(myGenomeConnectionList.size()/2);
			
			//Stepped Fitness function
			if(intDepCounter==0) {
				
				if(doubleDelayParam==1) {
					if(ProblemSetupMoo.booleanFoundNetwork==false && intNetworkSize>0) {
						ProblemSetupMoo.booleanFoundNetwork=true;
						ProblemSetupMoo.intSmallestNetwork=intNetworkSize;
						ProblemSetupMoo.intFirstNetwork=intNetworkSize;
						ProblemSetupMoo.doubleFirstNetwork=doubleTotalDistance;
						
					}
					
					
										
					double doublePartialDelta=1-((double)intNetworkSize/(double)ProblemSetupMoo.intFirstNetwork);
					fitnesses[1]=(float)doublePartialDelta;
				
					
					if(ProblemSetupMoo.booleanFoundNetwork==true) {
						if(ProblemSetupMoo.intSmallestNetwork>intNetworkSize && intNetworkSize>0) {
							ProblemSetupMoo.intSmallestNetwork=intNetworkSize;
							ProblemSetupMoo.doubleShortestNetwork=doubleTotalDistance;
						}
					}
				}
				//No Delay params not met
				else {
					if(ProblemSetupMoo.booleanFoundNetwork==true) {
						double doublePartialDelta=1-((double)intNetworkSize/(double)ProblemSetupMoo.intFirstNetwork);
						fitnesses[1]=(float)doublePartialDelta;
					}
					else {
						fitnesses[1]=0;
					}
				}
			}
			//Dependability Constraints not met
			else {
				if(ProblemSetupMoo.booleanFoundNetwork==true) {
					double doublePartialDelta=1-((double)intNetworkSize/(double)ProblemSetupMoo.intFirstNetwork);
					fitnesses[1]=(float)doublePartialDelta;
				}
				else {
					fitnesses[1]=0;
				}
			}
			
			
			
			fitnesses[2]=(1-(((float)intDepCounter)/(float)intTotalDeps));
			//fitnesses[0]=(float)(1/doubleTotalDistance);
			
			if(ProblemSetupMoo.doubleFirstNetwork<0) {
				fitnesses[0]=0;
			}
			else {
				fitnesses[0]=(float)(1-(doubleTotalDistance/ProblemSetupMoo.doubleFirstNetwork));	
			}
			//System.out.print("\nfitnesses:");
			for(int intFitnessCounter=0;intFitnessCounter<fitnesses.length;intFitnessCounter++) {
				System.out.print(fitnesses[intFitnessCounter]+"\t");
			}
			
			//rawfitness=((1000-4*intDepCounter))+(50/(myGenomeConnectionList.size()/2));	
			//if((rawfitness/ind2.genome.length)>(5.6)) {
			//System.out.println("\tInd: "+ProblemSetupMoo.doubleIndividual+"\tDep: "+intDepCounter+"\t : "+intTotalDeps+"\t : "+doubleTotalDistance+"\t : "+myGenomeConnectionList.size()/2+"\t : "+doubleDelayParam+"\t : "+(float)(((double)rawfitness))+"\t : "+ProblemSetupMoo.intSmallestNetwork+"\t : "+ProblemSetupMoo.intFirstNetwork+"\tTime: "+new Date().getTime()+"\t 21?: "+this.isInArrayList(myNetworkDevices, "http://rembrandt21.uva.netherlight.nl#Rembrandt21")+"\t 21s?: "+this.numberInArrayList(myGenomeConnectionList, "http://rembrandt21.uva.netherlight.nl#Rembrandt21")/2);
			//System.out.println("\tInd: "+ProblemSetupMoo.doubleIndividual+"\tDep: "+intDepCounter+"\t : "+intTotalDeps+"\t : "+doubleTotalDistance+"\t : "+myGenomeConnectionList.size()/2+"\t : "+doubleDelayParam+"\t : "+(float)(((double)rawfitness))+"\t : "+this.intSmallestNetwork+"\t : "+this.intFirstNetwork+"\tTime: "+(new Date().getTime()-lDateTime));
			/*Output in tabbed form: Columns are:
			1 - Fitness[0]
			2 - Fitness[1]
			3 - Fitness[2]
			4 - Fitness[3]
			5 - Fitness[4]
			6 - Individual Number
			7 - Individual how many cases fail
			8 - Individual how many cases there are
			9 - Total distance of cabling
			10 - number of connections A->B (technically /2 as there's one either way but counts one way direction)
			11 - Delay Parameter
			12 - Raw fitness
			13 - Smallest working network found
			14 - First working network found
			15 - Time
			*/
			System.out.println(ProblemSetupMoo.doubleIndividual+"\t"+intDepCounter+"\t"+intTotalDeps+"\t "+doubleTotalDistance+"\t"+myGenomeConnectionList.size()/2+"\t"+doubleDelayParam+"\t"+(float)(((double)rawfitness))+"\t"+this.intSmallestNetwork+"\t"+this.intFirstNetwork+"\t"+((float)(new Date().getTime()-lDateTime))/1000);
			//}
        }
        
        //for(int intListCounter=0;intListCounter<myGenomeConnectionList.size();intListCounter++) {
		//	System.out.println("A:" + myGenomeConnectionList.get(intListCounter));
		//}
        
        ProblemSetupMoo.doubleIndividual+=1;
        ind2.evaluated = true;
    }

	private int findPairInArrayList(ArrayList<String[]> myArrayList,
			String string0, String string1) {
		int intPosition = -1;
		for (int intCounter = 0; intCounter < myArrayList.size(); intCounter++) {
			if ((string0.contains(myArrayList.get(intCounter)[0]))
					&& (string1.contains(myArrayList.get(intCounter)[1]))) {
				intPosition = intCounter;
			}
		}
		return intPosition;
	}

	private int[] processGenome(int[] individualToProcess) {

		/*
		 * System.out.println("Genome: "); for(int
		 * intCounter=0;intCounter<individualToProcess.length;intCounter++) {
		 * System.out.println(individualToProcess[intCounter]); }
		 */

		// Remove anything from being connected to itself - i.e it's own port
		ArrayList<Integer> seenNumbers = new ArrayList<Integer>();

		for (int intCounter = 0; intCounter < individualToProcess.length; intCounter++) {
			if (!(this.isInArrayList(individualToProcess[intCounter],
					seenNumbers))) {
				seenNumbers.add(new Integer(individualToProcess[intCounter]));
			} else {
				individualToProcess[intCounter] = -1;
			}
		}

		// Figure out how many -1s there are in the array
		int intTrimmedLength = 0;

		for (int intCounter = 0; intCounter < individualToProcess.length; intCounter += 2) {
			if ((individualToProcess[intCounter] != -1)
					&& (individualToProcess[intCounter + 1] != -1)) {
				intTrimmedLength += 2;
			}
		}

		// Create a new individual without them
		int intInsertPosition = 0;
		int[] processedIndividual = new int[intTrimmedLength];

		for (int intCounter = 0; intCounter < individualToProcess.length; intCounter += 2) {
			if ((individualToProcess[intCounter] > 0)
					&& (individualToProcess[intCounter + 1] > 0)) {
				processedIndividual[intInsertPosition] = individualToProcess[intCounter];
				processedIndividual[intInsertPosition + 1] = individualToProcess[intCounter + 1];
				intInsertPosition += 2;
			}
		}

		// System.out.println("OrigLength: "+individualToProcess.length+"\t ProcLength: "+processedIndividual.length);

		/*
		 * System.out.println("GenomeProc: "); for(int
		 * intCounter=0;intCounter<processedIndividual.length;intCounter++) {
		 * System.out.println(processedIndividual[intCounter]); }
		 */

		return processedIndividual;
	}

	private boolean isInArrayList(int intSeachNumber,
			ArrayList<Integer> myArrayList) {
		boolean isIn = false;

		for (int intCounter = 0; intCounter < myArrayList.size(); intCounter++) {
			int intInList = myArrayList.get(intCounter).intValue();
			if (intInList == intSeachNumber) {
				isIn = true;
			}
		}
		return isIn;
	}

	private int findInArrayList(ArrayList<String> myArrayList,String stringToFind) {
		int intPosition = -1;

		for (int intCounter = 0; intCounter < myArrayList.size(); intCounter++) {
			if (stringToFind.contains(myArrayList.get(intCounter))) {
				intPosition = intCounter;
			}
		}

		return intPosition;
	}

	private void processArrayList(ArrayList arrayListToProcess) {
		for (int intCounter = 0; intCounter < arrayListToProcess.size(); intCounter++) {
			String stringSource = (String) ((PyList) arrayListToProcess
					.get(intCounter)).get(0);
			String stringDestination = (String) ((PyList) arrayListToProcess
					.get(intCounter)).get(1);
			// System.out.println("SRC: "+stringSource);
			// System.out.println("DST: "+stringDestination);
		}
	}

	private double calculateDistance(double doubleLat1, double doubleLong1,
			double doubleLat2, double doubleLong2) {
		double doubleDistance = 0;
		double doubleEarthRadius = 6372.8e3;
		doubleLat1 = doubleLat1 * Math.PI / 180;
		doubleLong1 = doubleLong1 * Math.PI / 180;
		doubleLat2 = doubleLat2 * Math.PI / 180;
		doubleLong2 = doubleLong2 * Math.PI / 180;

		double deltaLong = doubleLong2 - doubleLong1;
		double deltaLat = doubleLat2 - doubleLat1;
		double a = Math.pow((Math.sin(deltaLat / 2)), 2)
				+ (Math.cos(doubleLat1) * Math.cos(doubleLat2) * (Math.pow(
						Math.sin(deltaLong / 2), 2)));
		double c = 2 * Math.asin(Math.sqrt(a));
		doubleDistance = doubleEarthRadius * c;

		return doubleDistance;
	}

	private Vector<String> findSources(ArrayList myConnections) {
		Vector<String> vectorSources = new Vector<String>();
		for (int intCounter = 0; intCounter < myConnections.size(); intCounter++) {
			String stringSource = (String) ((PyList) myConnections
					.get(intCounter)).get(1);

			if (!this.isInVector(vectorSources, stringSource)) {
				vectorSources.addElement(stringSource);
			}
		}

		return vectorSources;
	}

	private Vector<String> findSinks(ArrayList myConnections) {
		Vector<String> vectorSinks = new Vector<String>();
		for (int intCounter = 0; intCounter < myConnections.size(); intCounter++) {
			String stringSink = (String) ((PyList) myConnections
					.get(intCounter)).get(0);

			if (!this.isInVector(vectorSinks, stringSink)) {
				vectorSinks.addElement(stringSink);
			}
		}

		return vectorSinks;
	}
	
	private boolean isInVector(Vector<String> myVector, String myString) {
		boolean isIn = false;

		for (int intCounter = 0; intCounter < myVector.size(); intCounter++) {
			if (myVector.elementAt(intCounter).contains(myString)) {
				isIn = true;
			}
		}

		return isIn;
	}
	
	private boolean isInArrayList(ArrayList<String> myArrayList, String myString) {
		boolean isIn = false;

		for (int intCounter = 0; intCounter < myArrayList.size(); intCounter++) {
			if (myArrayList.get(intCounter).contains(myString)) {
				isIn = true;
			}
		}

		return isIn;
	}
	
	private int numberInArrayList(ArrayList myArrayList, String myString) {
		int intNumber = 0;
		
		for (int intCounter = 0; intCounter < myArrayList.size(); intCounter++) {
			String stringSource = (String) ((PyList) myArrayList
					.get(intCounter)).get(0);
			String stringDestination = (String) ((PyList) myArrayList
					.get(intCounter)).get(1);
			
			if (stringSource.contains(myString) || stringDestination.contains(myString)) {
				intNumber++;
			}
		}

		return intNumber;
	}

	private double[] findPosition(ArrayList myArrayList, String stringPosToFind) {
		double[] doublePosition = new double[2];

		for (int intCounter = 0; intCounter < myArrayList.size(); intCounter++) {
			String stringDevice = (String) ((PyList) myArrayList
					.get(intCounter)).get(0);
			if (stringDevice.contains(stringPosToFind)) {
				doublePosition[0] = ((Double) ((PyList) myArrayList
						.get(intCounter)).get(1)).doubleValue();
				doublePosition[0] = ((Double) ((PyList) myArrayList
						.get(intCounter)).get(2)).doubleValue();
			}
		}

		return doublePosition;
	}
	
	private ArrayList<String> convertVector(Vector<String> vectorToConvert) {
		ArrayList<String> myArrayList = new ArrayList<String>();
		for(int intCounter=0;intCounter<vectorToConvert.size();intCounter++) {
			myArrayList.add((String)vectorToConvert.elementAt(intCounter));
		}
		return myArrayList;
	}
	
	private ArrayList convertToDevices(Vector<String> vectorToConvert, ArrayList myDeviceNames) {
		ArrayList myArrayList = new ArrayList<String>();
		for(int intVectorCounter=0; intVectorCounter < vectorToConvert.size(); intVectorCounter++ ) {
			String myElement = (String)vectorToConvert.elementAt(intVectorCounter);
			
			for(int intArrayListCounter=0; intArrayListCounter < myDeviceNames.size(); intArrayListCounter++) {
				String myDevice= (String)myDeviceNames.get(intArrayListCounter);
				
				if(myElement.contains(myDevice) && !this.isInArrayList(myArrayList, myDevice)) {
					myArrayList.add(myDevice);
				}
			}
		}
		return myArrayList;
	}
	
	private ArrayList findRouters(ArrayList myDeviceNames, ArrayList mySources, ArrayList mySinks) {
		ArrayList myRouters = new ArrayList<String>();
		
		for(int intCounter=0; intCounter < myDeviceNames.size(); intCounter++) {
			String myDevice = (String)myDeviceNames.get(intCounter);
			
			if(!this.isInArrayList(mySources, myDevice) && !this.isInArrayList(mySinks, myDevice)) {
				myRouters.add(myDevice);
			}
			
		}
		
		return myRouters;
	}
	
	private ArrayList findDevicesByConnectionList(ArrayList myConnectionList, ArrayList myDeviceNames) {
		ArrayList myCurrentNetworkDevices=new ArrayList();
		
		for(int intConnectionCounter=0; intConnectionCounter < myConnectionList.size(); intConnectionCounter++) {
			String stringSource=(String)((PyList)myConnectionList.get(intConnectionCounter)).get(0);		
			String stringDestination=(String)((PyList)myConnectionList.get(intConnectionCounter)).get(1);
			
			for(int intDeviceCounter=0; intDeviceCounter < myDeviceNames.size(); intDeviceCounter++ ) {
				String myDevice = (String)myDeviceNames.get(intDeviceCounter);
				
				if(stringSource.contains(myDevice) && !this.isInArrayList(myCurrentNetworkDevices, myDevice)) {
					myCurrentNetworkDevices.add(myDevice);
				}
				
				if(stringDestination.contains(myDevice) && !this.isInArrayList(myCurrentNetworkDevices, myDevice)) {
					myCurrentNetworkDevices.add(myDevice);
				}
				
			}
		}
		
		return myCurrentNetworkDevices;
	}
	
	private ArrayList findDevicesByConnectionList(ArrayList myConnectionList, ArrayList mySinks, ArrayList mySources, ArrayList myRouters) {
		ArrayList myCurrentNetworkDevices=new ArrayList();
		
		//Copy Sources and sinks into current list
		
		for(int intCounter=0; intCounter<mySinks.size();intCounter++) {
			myCurrentNetworkDevices.add((String)mySinks.get(intCounter));
		}
		
		
		for(int intCounter=0; intCounter<mySources.size();intCounter++) {
			if(!this.isInArrayList(mySinks, (String)mySources.get(intCounter))) {
				myCurrentNetworkDevices.add((String)mySources.get(intCounter));	
			}
		}
		
		
		
		//Add routers, only if they are a router and they do appear		
		for(int intConnectionCounter=0; intConnectionCounter < myConnectionList.size(); intConnectionCounter++) {
			String stringSource=(String)((PyList)myConnectionList.get(intConnectionCounter)).get(0);		
			String stringDestination=(String)((PyList)myConnectionList.get(intConnectionCounter)).get(1);
			
			for(int intDeviceCounter=0; intDeviceCounter < myRouters.size(); intDeviceCounter++ ) {
				String myDevice = (String)myRouters.get(intDeviceCounter);
				
				if(stringSource.contains(myDevice) && !this.isInArrayList(myCurrentNetworkDevices, myDevice)) {
					myCurrentNetworkDevices.add(myDevice);
				}
				
				if(stringDestination.contains(myDevice) && !this.isInArrayList(myCurrentNetworkDevices, myDevice)) {
					myCurrentNetworkDevices.add(myDevice);
				}		
			}
		}
		return myCurrentNetworkDevices;
	}
	
	private int[] setupGenomeOnActualconnections(ArrayList arrayListPhysical, ArrayList myPotentialConnections, int intLength) {
		int[] myConnections= new int[intLength];
		int intSourceInsertPoint=0;
		int intDestInsertPoint=1;
		
		for(int intCounter=0;intCounter<intLength;intCounter++) {
			myConnections[intCounter]=-1;
		}
		
		
		for(int intCounter=0;intCounter<arrayListPhysical.size();intCounter++) {
			String stringSource=(String)((PyList)arrayListPhysical.get(intCounter)).get(0);
    		String stringDest=(String)((PyList)arrayListPhysical.get(intCounter)).get(1);
    		
    		int intSourcePos = this.findPositionInPotentialConnections(myPotentialConnections, stringSource);
    		int intDestPos = this.findPositionInPotentialConnections(myPotentialConnections, stringDest);
    		
    		myConnections[intSourceInsertPoint]=intSourcePos;
    		myConnections[intDestInsertPoint]=intDestPos;
    		intSourceInsertPoint+=2;
    		intDestInsertPoint+=2;
		}
		
		return myConnections;
	}
	
	private int findPositionInPotentialConnections(ArrayList myPotentialConnections, String stringToFind) {
		int intPosition=-1;
		
		for(int intCounter=0;intCounter<myPotentialConnections.size();intCounter++) {
			String stringConnectionPoint=(String)((PyList)myPotentialConnections.get(intCounter)).get(1);
			if(stringConnectionPoint.contentEquals(stringToFind)) {
				intPosition=intCounter;
			}
		}
		
		return intPosition;
	}

}
