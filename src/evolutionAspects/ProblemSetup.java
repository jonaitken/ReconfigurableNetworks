package evolutionAspects;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Vector;

import org.python.core.PyList;

import networkcalculus.ConnectionsInformation;
import networkcalculus.DependabilityAssessment;
import networkcalculus.NetworkCalculusManager;
import ec.*;
import ec.simple.*;
import ec.util.Parameter;
import ec.vector.*;

public class ProblemSetup extends Problem implements SimpleProblemForm
    {
	
	private ArrayList myPossibleConnections;
	private ArrayList myActualPhysicalConnections;
	private ArrayList myPhysicalLocations;
	private ArrayList myServerConnections;
	private NetworkCalculusManager myNCManager;
	private Vector<String> vectorSources;
	private Vector<String> vectorSinks;
	private int intTotalConnections;
	private double doublePropSpeed=2e8;
	private double doubleLengthFiddleFactor=1.5;
	private PrintStream printStream;
	
	private static int intSmallestNetwork=-1;
	private static boolean booleanFoundNetwork=false;
	private static double doubleDelta=0;
	
	
	public void setup(EvolutionState state, Parameter base) {
		super.setup(state, base);
		
		this.myNCManager = new NetworkCalculusManager();
		myNCManager.evolutionaryLoopPreparation();
		ArrayList myPossibleConnections = myNCManager.getTotalInterfaces();
		this.myPossibleConnections=new ArrayList(myPossibleConnections);
		this.myActualPhysicalConnections = myNCManager.getPhysicalConnections();
		this.myPhysicalLocations=myNCManager.getLocations();
		this.myServerConnections=myNCManager.getServerConnections();
		
		System.out.println(this.myServerConnections);
		System.out.println(this.myPhysicalLocations);
		

		vectorSinks=this.findSinks(this.myServerConnections);
		vectorSources=this.findSources(this.myServerConnections);
		
		System.out.println("Sinks "+vectorSinks);
		System.out.println("Sources "+vectorSources);
		
		
		this.intTotalConnections=vectorSinks.size()+vectorSources.size();
		System.out.println("Total Connections: "+this.intTotalConnections);
		int intMaxPossibleAddress=myPossibleConnections.size()-1;
		
		state.parameters.set(new Parameter("pop.subpop.0.species.max-gene"), new Integer(intMaxPossibleAddress).toString());
		state.parameters.set(new Parameter("pop.subpop.0.species.min-gene"), new Integer(-1).toString());
		//state.parameters.set(new Parameter("pop.subpop.0.species.genome-size"), new Integer(myActualPhysicalConnections.size()).toString());
		System.out.println(state.parameters.toString());
		System.out.println("Connections:" +this.myPossibleConnections);
		
		File file = new File("log.log");
		try {
			this.printStream=new PrintStream(new FileOutputStream(file));
			System.setOut(this.printStream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation, final int threadnum) {
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
            			rawfitness+=(1/(double)this.intTotalConnections);
            		}
            		
            		if(this.isInVector(this.vectorSources, stringDestDevice)||this.isInVector(this.vectorSinks, stringDestDevice)) {
            			rawfitness+=(1/(double)this.intTotalConnections);
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
        double doubleConnectionPointFitness=rawfitness;
        //System.out.println("GenomeConnectionList: "+myGenomeConnectionList);
        //boolean boolValidList=this.myNCManager.evolutionaryGenerateNetwork(this.myActualPhysicalConnections);
        this.processArrayList(myGenomeConnectionList);
        boolean boolValidList=this.myNCManager.evolutionaryGenerateNetwork(myGenomeConnectionList);
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
						}
						//System.out.println(myConnectionsInformation.getTotalFlowHeadroom());
						//System.out.println("0: "+doubleHeadroom[0]+" 1: "+doubleHeadroom[1]+" 2: "+doubleHeadroom[2]+" 3: "+doubleHeadroom[3]+" 4: "+doubleHeadroom[4]);
					}
				}
			}
			
			if(boolMeetsHeadroom==false) {
				doubleDelayParam=0;
			}
			else {
				doubleDelayParam=1;
			}
			
			//System.out.println(doubleDelayParam);
			//rawfitness+=(1000/(myGenomeConnectionList.size()/2))-(intDepCounter/intTotalDeps);	
			
			//Paper fitness calculation
			//rawfitness=((1000-4*intDepCounter)*this.intTotalConnections)+(10000/(myGenomeConnectionList.size()/2))+10*doubleDelayParam;	
			
			//Updated fitness
			//rawfitness=1+(5-(5*(intDepCounter)/intTotalDeps))+doubleDelayParam+(1-(((double)myGenomeConnectionList.size()/2)/((double)this.myPossibleConnections.size()/2)));
			
			double doubleNotionalFittestSolution=15+doubleConnectionPointFitness;
			//Stepped Fitness function
			if(intDepCounter==0) {
				rawfitness+=10;
				
				if(doubleDelayParam==1) {
					int intNetworkSize=(int)(myGenomeConnectionList.size()/2);
					if(this.booleanFoundNetwork==false && intNetworkSize>0) {
						this.booleanFoundNetwork=true;
						this.intSmallestNetwork=intNetworkSize;
					}
					rawfitness+=5;
					
					double doublePartialDelta=1-((double)intNetworkSize/(double)this.intSmallestNetwork);
					//Network found is smaller, increase the fitness and increment the delta
					if(doublePartialDelta>0) {
						rawfitness+=1-((double)intNetworkSize/(double)this.intSmallestNetwork) + this.doubleDelta;
						this.doubleDelta+=doublePartialDelta;
					}
					//Network found is the same size as the smallest one, increment by delta to give equal fitness
					if(doublePartialDelta==0) {
						rawfitness+=1-((double)intNetworkSize/(double)this.intSmallestNetwork) + this.doubleDelta;
					}
					//Network is smaller, this will decrement the fitness a bit
					else {
						rawfitness+=1-((double)intNetworkSize/(double)this.intSmallestNetwork);
					}
				
					
					if(this.booleanFoundNetwork==true) {
						if(this.intSmallestNetwork>intNetworkSize && intNetworkSize>0) {
							this.intSmallestNetwork=intNetworkSize;
						}
					}
				}
				
				
			}
			
			if(intDepCounter!=0) {
				rawfitness+=(10-(10*(intDepCounter)/intTotalDeps));
			}
			
			
			
			//rawfitness=((1000-4*intDepCounter))+(50/(myGenomeConnectionList.size()/2));	
			//if((rawfitness/ind2.genome.length)>(5.6)) {
				System.out.println("Dep: "+intDepCounter+" :"+intTotalDeps+" :"+doubleTotalDistance+" :"+myGenomeConnectionList.size()/2+" :"+doubleDelayParam+" :"+(float)(((double)rawfitness))+" :"+this.intSmallestNetwork);
			//}
        }
        
        //for(int intListCounter=0;intListCounter<myGenomeConnectionList.size();intListCounter++) {
		//	System.out.println("A:" + myGenomeConnectionList.get(intListCounter));
		//}
        
        // We finish by taking the ABS of rawfitness.  By the way,
        // in SimpleFitness, fitness values must be set up so that 0 is <= the worst
        // fitness and +infinity is >= the ideal possible fitness.  Our raw fitness
        // value here satisfies this. 
        //if (rawfitness < 0) rawfitness = -rawfitness;
        if (!(ind2.fitness instanceof SimpleFitness))
            state.output.fatal("Whoa!  It's not a SimpleFitness!!!",null);
        ((SimpleFitness)ind2.fitness).setFitness(state,
            // what the heck, lets normalize the fitness for genome length
            // so it's within float range
            //(float)(((double)rawfitness)/ind2.genome.length),
        		(float)(((double)rawfitness)),
            ///... is the individual ideal?  Indicate here...
            false);
        ind2.evaluated = true;
    }
    
    
    private int findPairInArrayList(ArrayList<String[]> myArrayList, String string0, String string1) {
    	int intPosition=-1;
    	for(int intCounter=0; intCounter<myArrayList.size();intCounter++) {
    		if((string0.contains(myArrayList.get(intCounter)[0]))&&(string1.contains(myArrayList.get(intCounter)[1]))) {
    			intPosition=intCounter;
    		}
    	}
    	return intPosition;
    }
    
    private int[] processGenome(int[] individualToProcess) {
    	
    	/*System.out.println("Genome: ");
        for(int intCounter=0;intCounter<individualToProcess.length;intCounter++) {
        	System.out.println(individualToProcess[intCounter]);	
        }*/
    	
    	//Remove anything from being connected to itself - i.e it's own port
    	ArrayList<Integer> seenNumbers= new ArrayList<Integer>();
    	
    	for (int intCounter=0;intCounter<individualToProcess.length;intCounter++) {
    		if(!(this.isInArrayList(individualToProcess[intCounter],seenNumbers))) {
    			seenNumbers.add(new Integer(individualToProcess[intCounter]));
    		}
    		else {
    			individualToProcess[intCounter]=-1;
    		}
    	}
    	
    	
    	//Figure out how many -1s there are in the array
    	int intTrimmedLength=0;
    	
    	for (int intCounter=0;intCounter<individualToProcess.length;intCounter+=2) {
    		if((individualToProcess[intCounter]!=-1)&&(individualToProcess[intCounter+1]!=-1)) {
    			intTrimmedLength+=2;
    		}
    	}
    	
    	
    	//Create a new individual without them
    	int intInsertPosition=0;
    	int[] processedIndividual=new int[intTrimmedLength];
    	
    	for (int intCounter=0;intCounter<individualToProcess.length;intCounter+=2) {
    		if((individualToProcess[intCounter]>0)&&(individualToProcess[intCounter+1]>0)) {
    			processedIndividual[intInsertPosition]=individualToProcess[intCounter];
    			processedIndividual[intInsertPosition+1]=individualToProcess[intCounter+1];
    			intInsertPosition+=2;
    		}
    	}
    	
    	//System.out.println("OrigLength: "+individualToProcess.length+"\t ProcLength: "+processedIndividual.length);
    	
    	/*System.out.println("GenomeProc: ");
        for(int intCounter=0;intCounter<processedIndividual.length;intCounter++) {
        	System.out.println(processedIndividual[intCounter]);	
        }*/
    	
    	
    	return processedIndividual;
    }
    
    
    private boolean isInArrayList(int intSeachNumber, ArrayList<Integer> myArrayList) {
    	boolean isIn=false;
    	
    	for(int intCounter=0;intCounter<myArrayList.size();intCounter++) {
    		int intInList=myArrayList.get(intCounter).intValue();
    		if(intInList==intSeachNumber) {
    			isIn=true;
    		}
    	}
    	return isIn;
    }
    
    private int findInArrayList(ArrayList<String> myArrayList, String stringToFind) {
    	int intPosition=-1;
    	
    	for(int intCounter=0;intCounter<myArrayList.size();intCounter++) {
    		if(stringToFind.contains(myArrayList.get(intCounter))) {
    			intPosition=intCounter;
    		}
    	}
    	
    	return intPosition;
    }
    
    private void processArrayList(ArrayList arrayListToProcess) {
    	for (int intCounter=0;intCounter<arrayListToProcess.size();intCounter++) {
    		String stringSource=(String)((PyList)arrayListToProcess.get(intCounter)).get(0);
			String stringDestination=(String)((PyList)arrayListToProcess.get(intCounter)).get(1);
			//System.out.println("SRC: "+stringSource);
			//System.out.println("DST: "+stringDestination);
    	}    	
    }
    
    private double calculateDistance(double doubleLat1, double doubleLong1, double doubleLat2, double doubleLong2) {
		double doubleDistance=0;
		double doubleEarthRadius=6372.8e3;
		doubleLat1=doubleLat1*Math.PI/180;
		doubleLong1=doubleLong1*Math.PI/180;
		doubleLat2=doubleLat2*Math.PI/180;
		doubleLong2=doubleLong2*Math.PI/180;
		
		double deltaLong = doubleLong2 - doubleLong1;
		double deltaLat = doubleLat2 - doubleLat1;
		double a = Math.pow((Math.sin(deltaLat/2)),2) + (Math.cos(doubleLat1) * Math.cos(doubleLat2) * (Math.pow(Math.sin(deltaLong/2),2)));
		double c = 2 * Math.asin(Math.sqrt(a));
		doubleDistance = doubleEarthRadius * c;

		return doubleDistance;
	}
    
    private Vector<String> findSources(ArrayList myConnections) {
    	Vector<String> vectorSources=new Vector<String>();
    	for(int intCounter=0; intCounter<myConnections.size();intCounter++) {
    		String stringSource=(String)((PyList)myConnections.get(intCounter)).get(1);
    		
    		if(!this.isInVector(vectorSources, stringSource)) {
        		vectorSources.addElement(stringSource);	
    		}
    	}
    	
    	return vectorSources;
    }
    
    private Vector<String> findSinks(ArrayList myConnections) {
    	Vector<String> vectorSinks=new Vector<String>();
    	for(int intCounter=0; intCounter<myConnections.size();intCounter++) {
    		String stringSink=(String)((PyList)myConnections.get(intCounter)).get(0);
    		
    		if(!this.isInVector(vectorSinks, stringSink)) {
    			vectorSinks.addElement(stringSink);
    		}
    	} 	
    	
    	return vectorSinks;
    }
    
    private boolean isInVector(Vector<String> myVector, String myString) {
    	boolean isIn=false;
    	
    	for(int intCounter=0;intCounter<myVector.size();intCounter++) {
    		if(myVector.elementAt(intCounter).contains(myString)) {
    			isIn=true;
    		}
    	}
    	
    	return isIn;
    }
    
    private double[] findPosition(ArrayList myArrayList, String stringPosToFind) {
    	double[] doublePosition=new double[2];
    	
    	for(int intCounter=0;intCounter<myArrayList.size();intCounter++) {
    		String stringDevice=(String)((PyList)myArrayList.get(intCounter)).get(0);
    		if(stringDevice.contains(stringPosToFind)) {
    			doublePosition[0]=((Double)((PyList)myArrayList.get(intCounter)).get(1)).doubleValue();
    			doublePosition[0]=((Double)((PyList)myArrayList.get(intCounter)).get(2)).doubleValue();
    		}
    	}
    	
    	return doublePosition;
    }
    
    
}
