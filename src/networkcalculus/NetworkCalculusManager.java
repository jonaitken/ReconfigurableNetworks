package networkcalculus;

import org.python.antlr.ast.boolopType;
import org.python.core.PyList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import org.plyjy.factory.JythonObjectFactory;

import com.kenai.jffi.Array;

import jython.interfaces.MyParserType;

public class NetworkCalculusManager {
	
	private ArrayList myArrayPhysicalConnections;
	private ArrayList myArrayDeviceNames;
	private ArrayList myArrayServerInformation;
	private ArrayList myArrayServerConnections;
	private ArrayList myArrayClientRequirements;
	private ArrayList myArrayLocations;

	private ArrayList myArrayTotalInterfaces;
	
	private Vector vectorPossibleConnections=new Vector();
	
	private Network myNetwork;
	
	private Vector<DependabilityAssessment> vectorDependabilityAssessment;
	
	
	/* possibleConnections holds all a list of all of the possible ports in the network that can be linked.
	 * The GA uses this list to develop the genotype - a pair of numbers representing a connection (it'll be assumed to bi-directional even if not stated)
	 * That means the GA representation is a vector of int pairs. This is then of extendible length as more connections can be formed.
	 * In effect this is the physical connections arraylist (the transformation between the two is small)
	 * 
	 */
	private String[] possibleConnections;
	
	public NetworkCalculusManager() {
		
	}
	
	public void innerLoop() {
		this.getData();
    	
    	Network myNetwork = new Network(myArrayDeviceNames, myArrayPhysicalConnections, myArrayServerInformation, myArrayServerConnections, myArrayClientRequirements);
    	
    	Vector vectorConnectionsInfo = myNetwork.getConnectionsInformation();
    	
    	this.outputanalysisViaText(vectorConnectionsInfo);
	}
	
	public void middleLoop() {
		this.getData();
		
		Vector vectorDependabilityAssessment=new Vector();
		
		//Calculate network with no vertices removed:
		Network myNetwork = new Network(myArrayDeviceNames, myArrayPhysicalConnections, myArrayServerInformation, myArrayServerConnections, myArrayClientRequirements);
		Vector vectorConnectionsInfo = myNetwork.getConnectionsInformation();
		
		DependabilityAssessment thisNetworkDependAssessment=new DependabilityAssessment();
		
		thisNetworkDependAssessment.setValidNetwork(true);
		thisNetworkDependAssessment.setDestinationRemoved("None");
		thisNetworkDependAssessment.setSourceRemoved("None");
		thisNetworkDependAssessment.setConnectionsInformation(vectorConnectionsInfo);
		vectorDependabilityAssessment.addElement(thisNetworkDependAssessment);
		
		
		this.outputanalysisViaText(vectorConnectionsInfo);
		
		for(int intConnectionsCounter=0;intConnectionsCounter<this.myArrayPhysicalConnections.size();intConnectionsCounter++) {
			ArrayList myArrayRemoveablePhysicalConnections = new ArrayList(this.myArrayPhysicalConnections);
			System.out.println("Size: "+myArrayRemoveablePhysicalConnections.size());
			String stringSource=(String)((PyList)myArrayRemoveablePhysicalConnections.get(intConnectionsCounter)).get(0);
			String stringDestination=(String)((PyList)myArrayRemoveablePhysicalConnections.get(intConnectionsCounter)).get(1);

			System.out.println("Removing src: "+stringSource);
			System.out.println("Removing dest: "+stringDestination);
			boolean invalidatesServers=this.isCriticalServerConnection(stringSource, stringDestination, myArrayRemoveablePhysicalConnections,myArrayServerConnections);
			if(!invalidatesServers) {
				myArrayRemoveablePhysicalConnections.remove(intConnectionsCounter);
				int intReverseElement=this.findReversePhysicalConnection(stringSource, stringDestination, myArrayRemoveablePhysicalConnections);
				String stringSourceReverse=(String)((PyList)myArrayRemoveablePhysicalConnections.get(intReverseElement)).get(0);
				String stringDestinationReverse=(String)((PyList)myArrayRemoveablePhysicalConnections.get(intReverseElement)).get(1);
	
				System.out.println("Removing src: "+stringSourceReverse);
				System.out.println("Removing dest: "+stringDestinationReverse);
				
				myArrayRemoveablePhysicalConnections.remove(intReverseElement);

				myNetwork = new Network(myArrayDeviceNames, myArrayRemoveablePhysicalConnections, myArrayServerInformation, myArrayServerConnections, myArrayClientRequirements);
				vectorConnectionsInfo = myNetwork.getConnectionsInformation();
				this.outputanalysisViaText(vectorConnectionsInfo);
				
				thisNetworkDependAssessment=new DependabilityAssessment();
				thisNetworkDependAssessment.setValidNetwork(invalidatesServers);
				thisNetworkDependAssessment.setDestinationRemoved(stringSource);
				thisNetworkDependAssessment.setSourceRemoved(stringDestination);
				thisNetworkDependAssessment.setConnectionsInformation(vectorConnectionsInfo);
				vectorDependabilityAssessment.addElement(thisNetworkDependAssessment);
				
			}
			
			/*if(invalidatesServers) {
				thisNetworkDependAssessment=new DependabilityAssessment();
				thisNetworkDependAssessment.setValidNetwork(invalidatesServers);
				thisNetworkDependAssessment.setDestinationRemoved(stringSource);
				thisNetworkDependAssessment.setSourceRemoved(stringDestination);
				vectorDependabilityAssessment.addElement(thisNetworkDependAssessment);
			}*/
		}
	}
	
	public void evolutionaryLoopPreparation() {
		this.getData(); //for rdf reader
		//this.loadData(); //for simple load using infos
	}
	
	public boolean evolutionaryGenerateNetwork(ArrayList myArrayListEvolvedConnections, ArrayList myArrayDeviceNames) {
		//System.err.println("Devices: "+myArrayDeviceNames.toString());
		
		boolean boolValidNetwork=false;
		this.vectorDependabilityAssessment=new Vector();
		try {
			this.myNetwork = new Network(myArrayDeviceNames, myArrayListEvolvedConnections, myArrayServerInformation, myArrayServerConnections, myArrayClientRequirements);
			Vector vectorConnectionsInfo = myNetwork.getConnectionsInformation();
			
			DependabilityAssessment thisNetworkDependAssessment=new DependabilityAssessment();
			
			thisNetworkDependAssessment.setValidNetwork(true);
			thisNetworkDependAssessment.setDestinationRemoved("None");
			thisNetworkDependAssessment.setSourceRemoved("None");
			thisNetworkDependAssessment.setConnectionsInformation(vectorConnectionsInfo);
			this.vectorDependabilityAssessment.addElement(thisNetworkDependAssessment);
			
			for(int intConnectionsCounter=0;intConnectionsCounter<myArrayListEvolvedConnections.size();intConnectionsCounter+=2) {
				//System.err.println(intConnectionsCounter+" of "+myArrayListEvolvedConnections.size());
				ArrayList myArrayRemoveablePhysicalConnections = new ArrayList(myArrayListEvolvedConnections);
				//System.out.println("Size: "+myArrayRemoveablePhysicalConnections.size());
				String stringSource=(String)((PyList)myArrayRemoveablePhysicalConnections.get(intConnectionsCounter)).get(0);
				String stringDestination=(String)((PyList)myArrayRemoveablePhysicalConnections.get(intConnectionsCounter)).get(1);
				
				//System.out.println("Removing src: "+stringSource);
				//System.out.println("Removing dest: "+stringDestination);
				boolean invalidatesServers=this.isCriticalServerConnection(stringSource, stringDestination, myArrayRemoveablePhysicalConnections,myArrayServerConnections);
				if(!invalidatesServers) {
					//System.out.println("Removing Connections: "+stringSource+"\t"+stringDestination);
					myArrayRemoveablePhysicalConnections.remove(intConnectionsCounter);
					int intReverseElement=this.findReversePhysicalConnection(stringSource, stringDestination, myArrayRemoveablePhysicalConnections);
					String stringSourceReverse=(String)((PyList)myArrayRemoveablePhysicalConnections.get(intReverseElement)).get(0);
					String stringDestinationReverse=(String)((PyList)myArrayRemoveablePhysicalConnections.get(intReverseElement)).get(1);
		
					//System.out.println("Removing src: "+stringSourceReverse);
					//System.out.println("Removing dest: "+stringDestinationReverse);
					
					myArrayRemoveablePhysicalConnections.remove(intReverseElement);

					myNetwork = new Network(myArrayDeviceNames, myArrayRemoveablePhysicalConnections, myArrayServerInformation, myArrayServerConnections, myArrayClientRequirements);
					vectorConnectionsInfo = myNetwork.getConnectionsInformation();
					//this.outputanalysisViaText(vectorConnectionsInfo);
					
					thisNetworkDependAssessment=new DependabilityAssessment();
					thisNetworkDependAssessment.setValidNetwork(invalidatesServers);
					thisNetworkDependAssessment.setDestinationRemoved(stringSource);
					thisNetworkDependAssessment.setSourceRemoved(stringDestination);
					thisNetworkDependAssessment.setConnectionsInformation(vectorConnectionsInfo);
					this.vectorDependabilityAssessment.addElement(thisNetworkDependAssessment);
					
				}
				
				/*if(invalidatesServers) {
					thisNetworkDependAssessment=new DependabilityAssessment();
					thisNetworkDependAssessment.setValidNetwork(invalidatesServers);
					thisNetworkDependAssessment.setDestinationRemoved(stringSource);
					thisNetworkDependAssessment.setSourceRemoved(stringDestination);
					vectorDependabilityAssessment.addElement(thisNetworkDependAssessment);
				}*/
			}
			
			boolValidNetwork=true;
		}
		//catch(edu.uci.ics.jung.exceptions.ConstraintViolationException cve) {
			//System.out.println(cve.getViolatedConstraint());
			//Set fitness to 0 as it's a bogus network
		//}
		catch(java.lang.IndexOutOfBoundsException iobe) {
			//iobe.printStackTrace();
		}
		
		catch(java.util.NoSuchElementException nsee) {
			
		}
		
		catch(java.lang.NullPointerException npe) {
			
		}
		
		catch(java.lang.OutOfMemoryError ome) {
			ome.printStackTrace();
		}
		
		return boolValidNetwork;
	}
	
	private boolean isCriticalServerConnection(String stringToRemoveSource, String stringToRemoveDest, ArrayList myArrayPhysConnections, ArrayList myServerConnections) {
		boolean boolIsCritical=false;
		
		String destDevice=this.getDeviceFromConnection(stringToRemoveDest);
		String sourceDevice=this.getDeviceFromConnection(stringToRemoveSource);
				
    	for(int intListCounter=0;intListCounter<myServerConnections.size();intListCounter++) {
			//System.out.println("Y:" + myServerConnections.get(intListCounter));
    		String stringMyVidClient=(String)((PyList)myServerConnections.get(intListCounter)).get(0);
    		String stringMyVidServ=(String)((PyList)myServerConnections.get(intListCounter)).get(1);
    		
    		//Test to see whether connection effects Server
    		if(stringMyVidClient.contains(sourceDevice)||stringMyVidServ.contains(sourceDevice)) {
    			boolIsCritical=true;
    		}
    		    		
    		//Test to see whether connection effects Client
    		if(stringMyVidClient.contains(destDevice)||stringMyVidServ.contains(destDevice)) {
    			boolIsCritical=true;
    		}
    		
		}
    	//System.out.println("boolIsCritical: "+boolIsCritical);
		
		return boolIsCritical;
	}
	
	private String getDeviceFromConnection(String stringConnection) {
		String myDevice=new String();
				
		String delimsLS = "[#]+";
		
		String[] tokens = stringConnection.split(delimsLS);
		
		myDevice=tokens[0];
		
		return myDevice;
	}
	
	private int findReversePhysicalConnection(String stringSource, String stringDest, ArrayList myArrayRemoveablePhysicalConnections) {
		int intReverseConnection=-1;
		
		for(int intCounter=0;intCounter<myArrayRemoveablePhysicalConnections.size();intCounter++) {
			String stringReverseSource=(String)((PyList)myArrayRemoveablePhysicalConnections.get(intCounter)).get(0);
			String stringReverseDestination=(String)((PyList)myArrayRemoveablePhysicalConnections.get(intCounter)).get(1);
			
			if(stringSource.contains(stringReverseDestination)&&stringDest.contains(stringReverseSource)) {
				intReverseConnection=intCounter;
			}
		}
		
		return intReverseConnection;
	}
	
public void loadData() {
		
		//private ArrayList myArrayLocations;
		File file = new File("myArrayLocations.info");
		this.myArrayLocations=new ArrayList();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			
			while((line = reader.readLine())!=null) {
				String node = line.split("\t")[0];
				Double loc1 = new Double(line.split("\t")[1]);
				Double loc2 = new Double(line.split("\t")[2]);
				
				PyList elementList = new PyList();
				elementList.add(node);
				elementList.add(loc1);
				elementList.add(loc2);
				
				this.myArrayLocations.add(elementList);
				
				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//private ArrayList myArrayPhysicalConnections;
		file = new File("myArrayPhysicalConnections.info");
		this.myArrayPhysicalConnections=new ArrayList();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			
			while((line = reader.readLine())!=null) {
				String node1 = line.split("\t")[0];
				String node2 = line.split("\t")[1];
				Double val1 = new Double(line.split("\t")[2]);
				Double val2 = new Double(line.split("\t")[3]);
				
				PyList elementList = new PyList();
				elementList.add(node1);
				elementList.add(node2);
				elementList.add(val1);
				elementList.add(val2);
				this.myArrayPhysicalConnections.add(elementList);				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//private ArrayList myArrayDeviceNames;
		file = new File("myArrayDeviceNames.info");
		this.myArrayDeviceNames=new ArrayList();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			
			while((line = reader.readLine())!=null) {
				this.myArrayDeviceNames.add(line);				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//private ArrayList myArrayServerInformation;
		file = new File("myArrayServerInformation.info");
		this.myArrayServerInformation=new ArrayList();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			
			while((line = reader.readLine())!=null) {
				String node1 = line.split("\t")[0];
				Double val0 = new Double(line.split("\t")[1]);
				Double val1 = new Double(line.split("\t")[2]);
				Double val2 = new Double(line.split("\t")[3]);
				
				PyList elementList = new PyList();
				elementList.add(node1);
				elementList.add(val0);
				elementList.add(val1);
				elementList.add(val2);
				this.myArrayServerInformation.add(elementList);				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//private ArrayList myArrayServerConnections;
		file = new File("myArrayServerConnections.info");
		this.myArrayServerConnections=new ArrayList();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			
			while((line = reader.readLine())!=null) {
				String node1 = line.split("\t")[0];
				String node2 = line.split("\t")[1];
				
				PyList elementList = new PyList();
				elementList.add(node1);
				elementList.add(node2);
				this.myArrayServerConnections.add(elementList);				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//private ArrayList myArrayClientRequirements;
		file = new File("myArrayClientRequirements.info");
		this.myArrayClientRequirements=new ArrayList();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			
			while((line = reader.readLine())!=null) {
				String node1 = line.split("\t")[0];
				Double val0 = new Double(line.split("\t")[1]);
				
				PyList elementList = new PyList();
				elementList.add(node1);
				elementList.add(val0);
				this.myArrayClientRequirements.add(elementList);				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//private ArrayList myArrayTotalInterfaces;
		file = new File("myArrayTotalInterfaces.info");
		this.myArrayTotalInterfaces=new ArrayList();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			
			while((line = reader.readLine())!=null) {
				String node1 = line.split("\t")[0];
				String node2 = line.split("\t")[1];
				
				PyList elementList = new PyList();
				elementList.add(node1);
				elementList.add(node2);
				this.myArrayTotalInterfaces.add(elementList);				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	String[] possibleConnections = new String[myArrayTotalInterfaces.size()];
		for(int intListCounter=0;intListCounter<myArrayTotalInterfaces.size();intListCounter++) {
			//System.out.println("W:" + myArrayTotalInterfaces.get(intListCounter));
			
			PyList elementList = (PyList)myArrayTotalInterfaces.get(intListCounter);
			
			//System.out.println((String)elementList.get(0)+"\t"+(String)elementList.get(1));
			possibleConnections[intListCounter]=(String)((PyList)myArrayTotalInterfaces.get(intListCounter)).get(1);
		}
		
		//Process Connections to sort them into something that they can be dealt with...
    	for(int intDeviceCounter=0;intDeviceCounter<this.myArrayDeviceNames.size();intDeviceCounter++) {
    		String stringDeviceToSearchFor=(String)myArrayDeviceNames.get(intDeviceCounter);
    		
    		DeviceInterfaceInformation deviceInterfaceInfo = new DeviceInterfaceInformation(stringDeviceToSearchFor);
    		
    		for(int intTotalInterfaceCounter=0;intTotalInterfaceCounter<this.myArrayTotalInterfaces.size();intTotalInterfaceCounter++) {
    			String stringDevice=(String)((PyList)myArrayTotalInterfaces.get(intTotalInterfaceCounter)).get(0);
    			String stringDeviceInterface=(String)((PyList)myArrayTotalInterfaces.get(intTotalInterfaceCounter)).get(1);
    			if(stringDevice.contains(stringDeviceToSearchFor)) {
    				deviceInterfaceInfo.addInterfaces(stringDeviceInterface);
    			}
    		}
    		
    		this.vectorPossibleConnections.addElement(deviceInterfaceInfo);
    		
    	}
		
		
	}
	
	@SuppressWarnings("unchecked")
	private void getData() {
		JythonObjectFactory factory = JythonObjectFactory.getInstance();
    	MyParserType myParser = (MyParserType) JythonObjectFactory.createObject(MyParserType.class, "MyParser");

    	myParser.parse();
    	
    	//Extract Physical Locations from the parser
    	this.myArrayLocations =myParser.getLocations();
    	for(int intListCounter=0;intListCounter<myArrayLocations.size();intListCounter++) {
			//System.out.println("Q:" + myArrayLocations.get(intListCounter));
		}
    	
    	//Extract Device Connections from the parser
    	this.myArrayPhysicalConnections = myParser.getPhysicalConnectionInformation();
    	for(int intListCounter=0;intListCounter<myArrayPhysicalConnections.size();intListCounter++) {
			//System.out.println("R:" + myArrayPhysicalConnections.get(intListCounter));
		}
    	
    	//Extract Device Names from the parser
    	this.myArrayDeviceNames = myParser.getDeviceNames();
    	for(int intListCounter=0;intListCounter<myArrayDeviceNames.size();intListCounter++) {
			//System.out.println("S:" + myArrayDeviceNames.get(intListCounter));
		}
    	
    	//Extract Server Information from the parser
    	this.myArrayServerInformation = myParser.getVideoServerInformation();
    	for(int intListCounter=0;intListCounter<myArrayServerInformation.size();intListCounter++) {
			//System.out.println("T:" + myArrayServerInformation.get(intListCounter));
		}
    	
    	//Extract Server Connections from parser
    	this.myArrayServerConnections = myParser.getVideoServerConnectionInformation();
    	for(int intListCounter=0;intListCounter<myArrayServerConnections.size();intListCounter++) {
			//System.out.println("U:" + myArrayServerConnections.get(intListCounter));
		}
    	
    	//Extract Client Requirements from the parser
    	this.myArrayClientRequirements = myParser.getVideoClientRequirements();
    	for(int intListCounter=0;intListCounter<myArrayClientRequirements.size();intListCounter++) {
			//System.out.println("V:" + myArrayClientRequirements.get(intListCounter));
		}
    	
    	//Extract Possible Connections from the parser
    	this.myArrayTotalInterfaces = myParser.getTotalInterfaceList();
    	possibleConnections=new String[myArrayTotalInterfaces.size()];
    	for(int intListCounter=0;intListCounter<myArrayTotalInterfaces.size();intListCounter++) {
			//System.out.println("W:" + myArrayTotalInterfaces.get(intListCounter));
			possibleConnections[intListCounter]=(String)((PyList)myArrayTotalInterfaces.get(intListCounter)).get(1);
		}
     	
    	//Process Connections to sort them into something that they can be dealt with...
    	for(int intDeviceCounter=0;intDeviceCounter<this.myArrayDeviceNames.size();intDeviceCounter++) {
    		String stringDeviceToSearchFor=(String)myArrayDeviceNames.get(intDeviceCounter);
    		
    		DeviceInterfaceInformation deviceInterfaceInfo = new DeviceInterfaceInformation(stringDeviceToSearchFor);
    		
    		for(int intTotalInterfaceCounter=0;intTotalInterfaceCounter<this.myArrayTotalInterfaces.size();intTotalInterfaceCounter++) {
    			String stringDevice=(String)((PyList)myArrayTotalInterfaces.get(intTotalInterfaceCounter)).get(0);
    			String stringDeviceInterface=(String)((PyList)myArrayTotalInterfaces.get(intTotalInterfaceCounter)).get(1);
    			if(stringDevice.contains(stringDeviceToSearchFor)) {
    				deviceInterfaceInfo.addInterfaces(stringDeviceInterface);
    			}
    		}
    		
    		this.vectorPossibleConnections.addElement(deviceInterfaceInfo);
    		
    	}
    	
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList getTotalInterfaces() {
		return new ArrayList(this.myArrayTotalInterfaces);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList getDeviceNames() {
		return new ArrayList(this.myArrayDeviceNames);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ArrayList getLocations() {
		return new ArrayList(this.myArrayLocations);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList getPhysicalConnections() {
		return new ArrayList(this.myArrayPhysicalConnections);
	}
	
	public ArrayList getServerConnections() {
		return new ArrayList(this.myArrayServerConnections);
	}
	
	private void outputanalysisViaText(Vector vectorConnectionsInfo) {
		for(int intCounter=0;intCounter<vectorConnectionsInfo.size();intCounter++) {
    		ConnectionsInformation thisConnectionInfo=(ConnectionsInformation)vectorConnectionsInfo.elementAt(intCounter);
    		boolean boolResultCharny=thisConnectionInfo.testCharnyDelayBound();
    		boolean boolResultFQ=thisConnectionInfo.testFairQueueingDelayBound();
    		boolean boolResultPMOO=thisConnectionInfo.testPMOOAnalysisDelayBound();
    		boolean boolResultSFA=thisConnectionInfo.testSeparatedFlowAnalysisDelayBound();
    		boolean boolResultTFA=thisConnectionInfo.testTotalFlowAnalysisDelayBound();
    		
    		if(!boolResultCharny) {
    			System.out.println("Requirements don't Meet Charny for: "+thisConnectionInfo.getStringSource()+" to "+thisConnectionInfo.getStringDest());
    		}
    		
    		if(!boolResultFQ) {
    			System.out.println("Requirements don't Meet Fair Queueing for: "+thisConnectionInfo.getStringSource()+" to "+thisConnectionInfo.getStringDest());
    		}
    		
    		if(!boolResultPMOO) {
    			System.out.println("Requirements don't Meet PMOO for: "+thisConnectionInfo.getStringSource()+" to "+thisConnectionInfo.getStringDest());
    		}
    		
    		if(!boolResultSFA) {
    			System.out.println("Requirements don't Meet Separated Flow Analysis for: "+thisConnectionInfo.getStringSource()+" to "+thisConnectionInfo.getStringDest());
    		}
    		
    		if(!boolResultTFA) {
    			System.out.println("Requirements don't Meet Total Flow Analysis for: "+thisConnectionInfo.getStringSource()+" to "+thisConnectionInfo.getStringDest());
    		}
    		
    		if(boolResultCharny&&boolResultFQ&&boolResultPMOO&&boolResultSFA&&boolResultTFA) {
    			System.out.println("Requirements met for: "+thisConnectionInfo.getStringSource()+" to "+thisConnectionInfo.getStringDest());
    		}
    	}
	}
	
	public Vector<DependabilityAssessment> getDependabilityAssessment() {
		return new Vector<DependabilityAssessment>(this.vectorDependabilityAssessment);
	}
	
}
