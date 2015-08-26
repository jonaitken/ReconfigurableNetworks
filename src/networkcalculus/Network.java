package networkcalculus;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.python.core.PyList;

import cern.colt.function.IntComparator;

import unikl.disco.misc.GraphUtils;
import unikl.disco.nc.Curve;
import unikl.disco.nc.Flow;
import unikl.disco.nc.NetworkAnalyser;
import unikl.disco.nc.TurnProhibition;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.SimpleSparseVertex;
import edu.uci.ics.jung.utils.UserData;

public class Network {
	
	private String filename;
	private DirectedSparseGraph network_graph;
	private DirectedSparseGraph server_graph;
	private NetworkAnalyser na;
	
	//private int[][] intEdgetoVerticesBasicNetwork;
	//private int[][] intVerticestoVerticesLSMap;
	
	private String[] stringIntialNameToVerticesMap;
	private String[] stringInitialNameToSourceVertexMap;
	
	private ArrayList myArrayPhysicalConnections;
	
	private ArrayList myArrayServerInformation;
	private ArrayList myArrayServerConnections;
	private ArrayList myArrayClientRequirements;
	
	private ArrayList myArrayDeviceNames;
	
	private int intPlainNetworkEdges=0;
	private int intPlainNetworkVertices=0;
	private Vector myConnectionsInformation=new Vector();
	
	private Vector myVertexProperties=new Vector();
	
	private Vector myEdgeProperties=new Vector();

	public Network(ArrayList myArrayDeviceNames, ArrayList myArrayPhysicalConnections, ArrayList myArrayServerInformation, ArrayList myArrayServerConnections, ArrayList myArrayClientRequirements) {
		//System.out.println();
		//System.out.println("New ONE");
		//System.out.println(myArrayDeviceNames.toString());
		//System.out.println(myArrayPhysicalConnections.toString());
		this.network_graph=this.createNetwork(myArrayPhysicalConnections, myArrayDeviceNames);
		
		this.myArrayDeviceNames=new ArrayList(myArrayDeviceNames);
		
		this.myArrayPhysicalConnections=new ArrayList(myArrayPhysicalConnections);
		
		this.myArrayServerInformation=new ArrayList(myArrayServerInformation);
		this.myArrayServerConnections=new ArrayList(myArrayServerConnections);
		this.myArrayClientRequirements=new ArrayList(myArrayClientRequirements);
		
		//this.vectorServerInfo=new Vector(vectorServerInfo);
		//this.vectorServerConnectioninfo= new Vector(vectorServerConnectionInfo);
		//this.vectorConnectionCapacities= new Vector(vectorConnectionCapacities);
		//this.vectorConnectionLatencies= new Vector(vectorConnectionLatencies);
		this.run();
		
		/*this.network_graph = new DirectedSparseGraph();
		int counter=0;
		while(true) {
			SimpleSparseVertex ssv=new SimpleSparseVertex();
			network_graph.addVertex(ssv);
			counter+=1;
			if(counter%1000==0) {
				System.out.println("Count: "+counter);
			}
			
		}*/
		
	}
	
	public DirectedSparseGraph createNetwork(ArrayList myArrayPhysicalConnections, ArrayList myArrayDeviceNames) {
		this.network_graph = new DirectedSparseGraph();
		this.network_graph.newInstance();
		this.stringIntialNameToVerticesMap = new String[myArrayDeviceNames.size()];
		for (int intCounter = 0; intCounter < myArrayDeviceNames.size(); intCounter++) {
			VertexProperties thisVertexProperties = new VertexProperties();
						
			SimpleSparseVertex ssv=new SimpleSparseVertex();
			network_graph.addVertex(ssv);
			//ssv.addUserDatum("VNAME", "V"+intCounter, UserData.SHARED);
			this.stringIntialNameToVerticesMap[intCounter]=(String)myArrayDeviceNames.get(intCounter);
			
			thisVertexProperties.setStringIdentifier((String)myArrayDeviceNames.get(intCounter));
			//System.out.println((String)myArrayDeviceNames.get(intCounter));
			String stringVNum=ssv.toString();
			
			int intVNum=(new Integer(stringVNum.substring(1, stringVNum.length()))).intValue();
			
			thisVertexProperties.setOriginalVertexID(intVNum);
			
			this.myVertexProperties.addElement(thisVertexProperties);
			
			
		}
		
		Vertex[] vertices = (Vertex[])network_graph.getVertices().toArray(new Vertex[network_graph.numVertices()]);
		
		//System.out.println("Vertices: "+network_graph.getVertices());
				
		int[] verticesTranslationVals = this.verticesTranslationNumberFree(network_graph.getVertices().toString());
		
		//Copy positions to VertexProperties
		for(int intPositionCounter=0;intPositionCounter<verticesTranslationVals.length;intPositionCounter++) {
			int intVertexID=verticesTranslationVals[intPositionCounter];
			
			for(int intVertexCounter=0;intVertexCounter<this.myVertexProperties.size();intVertexCounter++) {
				VertexProperties thisVertexProperties=(VertexProperties)this.myVertexProperties.elementAt(intVertexCounter);
				
				if(thisVertexProperties.getOriginalVertexID()==intVertexID) {
					thisVertexProperties.setPositionInOriginalVetexList(intPositionCounter);
				}
			}
		}
		
		
		
		
		/*for (int intTempCounter=0; intTempCounter<verticesTranslationVals.length;intTempCounter++) {
			System.out.println(intTempCounter+"\t"+verticesTranslationVals[intTempCounter]);
		}*/
		

		/*for(int intCounter=0; intCounter<this.stringIntialNameToVerticesMap.length;intCounter++) {
			System.out.println("Vert Map: "+stringIntialNameToVerticesMap[intCounter]);
		}*/
		
		
		this.stringInitialNameToSourceVertexMap=new String[myArrayPhysicalConnections.size()];
		//System.out.println("Phys Size: "+myArrayPhysicalConnections.size());
		for(int intConnectionCounter=0;intConnectionCounter<myArrayPhysicalConnections.size();intConnectionCounter++) {
			String stringSource=(String)((PyList)myArrayPhysicalConnections.get(intConnectionCounter)).get(0);
			
			String stringDestination=(String)((PyList)myArrayPhysicalConnections.get(intConnectionCounter)).get(1);
			
			/*int intSourceVertexOriginal=this.findIntStringInArray(stringSource, this.stringIntialNameToVerticesMap);
			int intDestinationVertexOriginal=this.findIntStringInArray(stringDestination, this.stringIntialNameToVerticesMap);
			
			for(int intTCounter=0;intTCounter<stringIntialNameToVerticesMap.length;intTCounter++) {
				System.out.println("stringIntialNameToVerticesMap: "+stringIntialNameToVerticesMap[intTCounter]);
			}
			System.out.println("intSourceVertexOriginal: "+intSourceVertexOriginal);
			System.out.println("intDestinationVertexOriginal: "+intDestinationVertexOriginal);
			
			int intSourceVertexSorted=this.findArrayPosition(intSourceVertexOriginal, verticesTranslationVals);
			int intDestinationVertexSorted=this.findArrayPosition(intDestinationVertexOriginal, verticesTranslationVals);*/
			
			int intSourceVertexSorted=findSourcePositionInListInVector(stringSource);
			int intDestinationVertexSorted=findSourcePositionInListInVector(stringDestination);
			
			//int intSourceVertexSorted=verticesTranslationVals[intSourceVertexOriginal];
			//int intDestinationVertexSorted=verticesTranslationVals[intDestinationVertexOriginal];
			
			//System.out.println(stringSource+"\t"+stringDestination+"\t"+intSourceVertexSorted+"\t"+intDestinationVertexSorted);
			
			this.stringInitialNameToSourceVertexMap[intConnectionCounter]=stringSource;
			DirectedSparseEdge newEdge=new DirectedSparseEdge(vertices[intSourceVertexSorted], vertices[intDestinationVertexSorted]);
			//newEdge.addUserDatum("ENAME","E"+intConnectionCounter, UserData.SHARED);
			
			String stringEdgeNum=newEdge.toString();
			//System.out.println(stringEdgeNum);
			int intEdgeNum =this.splitEdgeStringToID(stringEdgeNum);
			
			EdgeProperties thisEdgeProperties = new EdgeProperties();
			
			thisEdgeProperties.setOriginalEdgeID(intEdgeNum);
			thisEdgeProperties.setSource(stringSource);
			thisEdgeProperties.setDestination(stringDestination);
			
			this.myEdgeProperties.addElement(thisEdgeProperties);
			
			network_graph.addEdge(newEdge);			
		}
					
		//System.out.println("Edges1: "+network_graph.getEdges());
		
		//System.out.println(network_graph.getEdges().toString());
		//System.out.println(network_graph.getVertices().toString());
		
		this.edgeTranslation(network_graph.getEdges().toString());
		
		return network_graph;
	}
	
	public int splitEdgeStringToID(String stringEdgeInfo) {
		int intID=-1;
		String phrase = stringEdgeInfo;
		String delims = "[(), ]+";
		String[] tokens = phrase.split(delims);
		
		String stringEdgeNum=tokens[0];
		intID=(new Integer(stringEdgeNum.substring(1, stringEdgeNum.length()))).intValue();
		
		
		return intID;
	}
	
	public int findSourcePositionInListInVector(String stringToFind) {
		int intPosition=-1;
		
		for(int intCounter=0;intCounter<this.myVertexProperties.size();intCounter++) {
			VertexProperties thisVertexProperty = (VertexProperties)myVertexProperties.elementAt(intCounter);
			if(stringToFind.contains(thisVertexProperty.getStringIdentifier())) {
				intPosition=thisVertexProperty.getPositionInOriginalVetexList();
			}
			
		}
		return intPosition;
	}
	
	public int findArrayPosition(int intToFind, int[] intArray) {
		int intPosition=-1;
		
		for(int intCounter=0;intCounter<intArray.length;intCounter++) {
			if(intToFind==intArray[intCounter]) {
				intPosition=intCounter;
			}
		}
		
		return intPosition;
		
	}
	
	public int getMaxInVertices(String[] stringVertices) {
		int intMaxVal=-1;
		for (int intCounter=0;intCounter<stringVertices.length;intCounter++) {
			String stringNumber = stringVertices[intCounter].substring(1,(stringVertices[intCounter].length()));
			Integer integerNumber=new Integer(stringNumber);
			if(integerNumber.intValue()>intMaxVal) {
				intMaxVal=integerNumber.intValue();
			}
		}
		this.intPlainNetworkVertices=intMaxVal+1;
		return intMaxVal;
	}
	
	public int getMinInVertices(String[] stringVertices) {
		int intMinVal=-1;
		for (int intCounter=0;intCounter<stringVertices.length;intCounter++) {
			String stringNumber = stringVertices[intCounter].substring(1,(stringVertices[intCounter].length()));
			Integer integerNumber=new Integer(stringNumber);
			
			if(intMinVal==-1) {
				intMinVal=integerNumber.intValue();
			}
			
			if(integerNumber.intValue()<intMinVal) {
				intMinVal=integerNumber.intValue();
			}
		}
		return intMinVal;
	}
	
	public int[] verticesTranslation(String stringVertices) {
		int[] intTranslation;
		int intMaxValVertices;
		int intMinValVertices;
		
		String phrase = stringVertices;
		phrase=phrase.substring(1,(phrase.length()-1));
		String delims = "[, ]+";
		String[] tokens = phrase.split(delims);
				
		intTranslation = new int[tokens.length];
		
		intMaxValVertices=this.getMaxInVertices(tokens);
		intMinValVertices=this.getMinInVertices(tokens);		
		
		for(int intPosToFind=intMinValVertices;intPosToFind<=intMaxValVertices;intPosToFind++) {
			String stringtoTest = new String("V"+intPosToFind);
			//System.out.println(stringtoTest);
			int intLocation=0;
			for(int intArraySweep=0;intArraySweep<tokens.length;intArraySweep++) {
				if(tokens[intArraySweep].equals(stringtoTest)) {
					intLocation=intArraySweep;
				}
			}
			intTranslation[intPosToFind]=intLocation;
		}
		
		return intTranslation;
	}
	
	public int[] verticesTranslationNumberFree(String stringVertices) {
		int[] intTranslation;
		int intMaxValVertices;
		int intMinValVertices;
		
		String phrase = stringVertices;
		phrase=phrase.substring(1,(phrase.length()-1));
		String delims = "[, ]+";
		String[] tokens = phrase.split(delims);
				
		intTranslation = new int[tokens.length];
		
		intMaxValVertices=this.getMaxInVertices(tokens);
		intMinValVertices=this.getMinInVertices(tokens);
		
		
		for(int intPosToFind=intMinValVertices;intPosToFind<=intMaxValVertices;intPosToFind++) {
			String stringtoTest = new String("V"+intPosToFind);
			//System.out.println(stringtoTest);
			int intLocation=0;
			for(int intArraySweep=0;intArraySweep<tokens.length;intArraySweep++) {
				if(tokens[intArraySweep].equals(stringtoTest)) {
					intLocation=intArraySweep;
				}
			}
			intTranslation[intLocation]=intPosToFind;
		}
		
		return intTranslation;
	}
	
	public int[][] edgeTranslation(String stringEdges) {
		int[][] intTranslation;
		
		String phraseEdges = stringEdges;
		phraseEdges=phraseEdges.substring(1,(phraseEdges.length()-1));
		String delimsEdges = "[, ()]+";
		String[] tokensEdges = phraseEdges.split(delimsEdges);
		
		//Number of edges is 1/3 of length as each specifies start end
		int intNumberEdges=tokensEdges.length/3;
		this.intPlainNetworkEdges=intNumberEdges;
		
		intTranslation=new int[intNumberEdges][2];
		
		/*for (int intEdgesTracker=0;intEdgesTracker<tokensEdges.length;intEdgesTracker++) {
			String stringNumberEdge = tokensEdges[intEdgesTracker].substring(1,(tokensEdges[intEdgesTracker].length()));
			Integer integerNumberEdge = Integer.valueOf(stringNumberEdge);
			
			intEdgesTracker++;
			
			String stringNumberVertSource = tokensEdges[intEdgesTracker].substring(1,(tokensEdges[intEdgesTracker].length()));
			Integer integerNumberVertSource = Integer.valueOf(stringNumberVertSource);
			
			intEdgesTracker++;
			
			String stringNumberVertDest = tokensEdges[intEdgesTracker].substring(1,(tokensEdges[intEdgesTracker].length()));
			Integer integerNumberVertDest = Integer.valueOf(stringNumberVertDest);
			
			intTranslation[integerNumberEdge.intValue()][0]=integerNumberVertSource.intValue();
			intTranslation[integerNumberEdge.intValue()][1]=integerNumberVertDest.intValue();
			
			
		}*/
		
		/*for (int intTCounter=0;intTCounter<intNumberEdges;intTCounter++) {
			System.out.println("E"+intTCounter+" Source: "+intTranslation[intTCounter][0]+" Dest: "+intTranslation[intTCounter][1]);
		}*/
		
		return intTranslation;
		
	}
	
	private int findIntStringInArray(String stringToFind, String[] stringArray) {
		int intPosition=-1;
		
		for(int intCounter=0;intCounter<stringArray.length;intCounter++) {
			if(stringToFind.contains(stringArray[intCounter])) {
				intPosition=intCounter;
			}
		}
		
		return intPosition;
		
	}
	
	public void linkServerTranslation(String stringLinkServerMap) {
		
		//Remove opening and closing braces
		//System.out.println(stringLinkServerMap.toString());
		stringLinkServerMap=stringLinkServerMap.substring(1,(stringLinkServerMap.length()-1));
		
		//System.out.println("LS Map "+stringLinkServerMap);
		
		String delimsLS = "[, =()]+";
				
		String[] tokensLS = stringLinkServerMap.split(delimsLS);
		
		int intNumberLS = tokensLS.length/4;
		
		//element in 1 is source vertices
		//element in 2 is dest vertices
		//element in 3 is new vertices
		
		//Edge to vertices translation comes in stacks of 4 once the delimiters have been stripped out
		//There are "number of edges" elements
		//The first term is the source vertices
		//The second term is the destination vertices
		//The third term is the replacement vertices
		
		
		for (int intEdgeCounter=0;intEdgeCounter<tokensLS.length;intEdgeCounter++) {
			Integer intEdgeNumber;
			Integer intSourceVertices;
			Integer intNewVertices;
			Integer intDestVertices;
			
			String stringEdgeNumber = tokensLS[intEdgeCounter].substring(1,(tokensLS[intEdgeCounter].length()));
			intEdgeNumber=new Integer(stringEdgeNumber);
			
			intEdgeCounter++;
			
			String stringSourceVertices = tokensLS[intEdgeCounter].substring(1,(tokensLS[intEdgeCounter].length()));
			intSourceVertices=new Integer(stringSourceVertices);
			
			intEdgeCounter++;
			
			String stringDestVertices = tokensLS[intEdgeCounter].substring(1, (tokensLS[intEdgeCounter].length()));
			intDestVertices=new Integer(stringDestVertices); 
			
			intEdgeCounter++;
			
			String stringNewVertices = tokensLS[intEdgeCounter].substring(1,(tokensLS[intEdgeCounter].length()));
			intNewVertices=new Integer(stringNewVertices);
			
			//System.out.println("OldSource: "+intSourceVertices.intValue()+"\t OldDest "+intDestVertices.intValue()+"\t New: "+intNewVertices.intValue());
			//Deal with Vertex Mapping
			for(int intVertexCounter=0;intVertexCounter<this.myVertexProperties.size();intVertexCounter++) {
				VertexProperties thisVertexProperties = (VertexProperties)myVertexProperties.elementAt(intVertexCounter);
				if(intSourceVertices.intValue()==thisVertexProperties.getOriginalVertexID()) {
					thisVertexProperties.addMappedSourceVertexID(intNewVertices.intValue());
					
				}
				
				if(intDestVertices.intValue()==thisVertexProperties.getOriginalVertexID()) {
					thisVertexProperties.addMappedDestVertexID(intNewVertices.intValue());
					
				}
				
			}
			
			//Now deal with Edge Mapping
			
			for(int intEdgeVectorCounter=0;intEdgeVectorCounter<this.myEdgeProperties.size();intEdgeVectorCounter++) {
				EdgeProperties thisEdgeProperties = (EdgeProperties)this.myEdgeProperties.elementAt(intEdgeVectorCounter);
				if(intEdgeNumber==thisEdgeProperties.getOriginalEdgeID()) {
					thisEdgeProperties.setServerID(intNewVertices.intValue());
				}
				
			}
			
			
			/*this.intVerticestoVerticesLSMap[intEdgeNumber.intValue()][0]=intSourceVertices.intValue();
			this.intVerticestoVerticesLSMap[intEdgeNumber.intValue()][1]=intDestVertices.intValue();
			this.intVerticestoVerticesLSMap[intEdgeNumber.intValue()][2]=intNewVertices.intValue();*/
				
		}
		
		/*for (int intTCounter=0;intTCounter<this.intVerticestoVerticesLSMap.length;intTCounter++) {
			System.out.println("OldSource: "+this.intVerticestoVerticesLSMap[intTCounter][0]+"\t OldDest "+this.intVerticestoVerticesLSMap[intTCounter][1]+"\t New: "+this.intVerticestoVerticesLSMap[intTCounter][2]);
		}*/
		
	}
	
	
	public DirectedSparseGraph convertFromNetworkToServerGraph(DirectedSparseGraph network_graph) {
		DirectedSparseGraph server_graph;

		Map<DirectedEdge,Vertex> link_server_map = new HashMap<DirectedEdge,Vertex>();
		Map<Vertex,Set<DirectedEdge>> router_turns_map = new HashMap<Vertex,Set<DirectedEdge>>();
		server_graph = GraphUtils.createServerGraph(network_graph, link_server_map, router_turns_map);
		//(new GraphMLFile()).save(server_graph, filename + "_server.graphml");
		//System.out.println("network_graph_edges: "+network_graph.getEdges());
		//System.out.println("network_graph_vertices: "+network_graph.getVertices());
		//System.out.println("Link Server Map: "+link_server_map.toString());
		

		/*System.out.println(network_graph.getVertices().toString());
		System.out.println(network_graph.getEdges().toString());
		System.out.println();System.out.println();System.out.println();*/
		
		this.linkServerTranslation(link_server_map.toString());
		
		//System.out.println("router_turns: "+router_turns_map.toString());
		TurnProhibition tp = new TurnProhibition(network_graph);
		tp.runTurnProhibition();
		tp.removeProhibitedTurns(server_graph, link_server_map, router_turns_map);
		//(new GraphMLFile()).save(server_graph, filename + "_server_cyclefree.graphml");

		return server_graph;
	}
	
	public Vector run() {
		
		// create a network graph manually...
		//network_graph = createDemoNetwork();
		//(new GraphMLFile()).save(network_graph, filename + "_network.graphml");
		// ... or by loading an existing graph, e.g. created and exported using BRITE
		//network_graph = (DirectedSparseGraph) (new GraphMLFile()).load(filename + "_network.graphml");

		// convert this graph to a server graph
		//System.err.println("Start");
		server_graph = convertFromNetworkToServerGraph(network_graph);
		
		/*Now figure out what has ended up where*/	
		
		// create a new network analyser instance for the server graph
		na = new NetworkAnalyser(server_graph);
				
		// create a service curve for each server
		Set<Vertex> servers = GraphUtils.getServerSet(server_graph);
		
		
		//Need to be able to map servers back to original edges - Each connection is defined using Rate and latency
		//this is all held in the intVerticestoVerticesLSMap
		//The Server maps back through the LSMAP to an original Edge, which maps back to an original source. That should then hold the rate+latency
		//0.00001

		//System.out.println("VectorConnCap: "+this.vectorConnectionCapacities);
		//System.out.println("VectorConnLat: "+this.vectorConnectionLatencies);
		
		for (Vertex server : servers) {
			//System.out.println("server: "+server.toString());
			
			String serverNumber=(server.toString()).substring(1,(server.toString().length()));
			
			int intServerNumber=(new Integer(serverNumber)).intValue();
			
			int intOriginalEdge=-1;
			
			//System.out.println("server2: "+intServerNumber);
			for(int intCounter=0;intCounter<this.myEdgeProperties.size();intCounter++) {
				EdgeProperties thisEdgeProperties=(EdgeProperties)this.myEdgeProperties.elementAt(intCounter);
				if(thisEdgeProperties.getServerID()==intServerNumber) {
					intOriginalEdge=intCounter;
				}
			}
			
			double doubleConnectionCapacity = ((Double)((PyList)myArrayPhysicalConnections.get(intOriginalEdge)).get(2)).doubleValue();
			double doubleConnectionLatency = ((Double)((PyList)myArrayPhysicalConnections.get(intOriginalEdge)).get(3)).doubleValue();
			
			//System.out.println("ConCap: "+doubleConnectionCapacity);
			//System.out.println("ConLat: "+doubleConnectionLatency);
			
			na.setServiceCurve(server, Curve.createRateLatency(doubleConnectionLatency, doubleConnectionCapacity));
			na.setMaxServiceCurve(server, Curve.createRateLatency(doubleConnectionLatency, doubleConnectionCapacity));
		}

		// create a flow between every source-sink-pair if a path exists
		DijkstraShortestPath shortest_paths = new DijkstraShortestPath(server_graph);

		
		Set<Vertex> sources = GraphUtils.getSourceSet(server_graph);
		Set<Vertex> sinks = GraphUtils.getSinkSet(server_graph);
		
		//Assign servers, sources and sinks here...
		
		//System.out.println("Servers: "+servers.toString());
		//System.out.println("Sources: "+sources.toString());
		//System.out.println("Sinks: "+sinks.toString());
		
		//System.out.println("Vertices: "+server_graph.getVertices().toString());
		
		//System.out.println("Edges: "+server_graph.getEdges().toString());
		
		//Source and sink set can be related back to origin using the contents of intVerticestoVerticesLSMap
		/*Ignore the mapping of NC V terms to position within teh array. It can be done using the intVerticestoVerticesLSMap and the order that the data comes in
		e.g. the list order is something like: (* indicate singleton sources which for the example network are the servers - obviously it works the other way in dest)
		OldSource: 0	 OldDest 8	 New: 21
		OldSource: 0	 OldDest 11	 New: 58
		OldSource: 0	 OldDest 13	 New: 70
		OldSource: 0	 OldDest 17	 New: 57
		*OldSource: 1	 OldDest 9	 New: 69
		OldSource: 2	 OldDest 5	 New: 56
		OldSource: 2	 OldDest 6	 New: 67
		OldSource: 2	 OldDest 11	 New: 55
		OldSource: 2	 OldDest 16	 New: 68
		*OldSource: 3	 OldDest 5	 New: 31
		*OldSource: 4	 OldDest 5	 New: 42
		OldSource: 5	 OldDest 2	 New: 22
		OldSource: 5	 OldDest 3	 New: 43
		OldSource: 5	 OldDest 4	 New: 32
		OldSource: 5	 OldDest 9	 New: 39
		OldSource: 5	 OldDest 16	 New: 23
		OldSource: 5	 OldDest 19	 New: 41
		OldSource: 5	 OldDest 20	 New: 33
		OldSource: 6	 OldDest 2	 New: 66
		OldSource: 6	 OldDest 14	 New: 51
		*OldSource: 7	 OldDest 11	 New: 59
		*OldSource: 8	 OldDest 0	 New: 53
		OldSource: 9	 OldDest 1	 New: 45
		OldSource: 9	 OldDest 5	 New: 54
		OldSource: 9	 OldDest 13	 New: 60
		*OldSource: 10	 OldDest 11	 New: 52
		OldSource: 11	 OldDest 0	 New: 46
		OldSource: 11	 OldDest 2	 New: 30
		OldSource: 11	 OldDest 7	 New: 34
		OldSource: 11	 OldDest 10	 New: 27
		OldSource: 11	 OldDest 14	 New: 26
		OldSource: 11	 OldDest 18	 New: 35
		*OldSource: 12	 OldDest 14	 New: 37
		OldSource: 13	 OldDest 0	 New: 29
		OldSource: 13	 OldDest 9	 New: 28
		OldSource: 13	 OldDest 15	 New: 36
		OldSource: 13	 OldDest 16	 New: 65
		OldSource: 14	 OldDest 6	 New: 47
		OldSource: 14	 OldDest 11	 New: 61
		OldSource: 14	 OldDest 12	 New: 64
		OldSource: 14	 OldDest 16	 New: 48
		*OldSource: 15	 OldDest 13	 New: 50
		OldSource: 16	 OldDest 2	 New: 63
		OldSource: 16	 OldDest 5	 New: 62
		OldSource: 16	 OldDest 13	 New: 49
		OldSource: 16	 OldDest 14	 New: 25
		*OldSource: 17	 OldDest 0	 New: 38
		*OldSource: 18	 OldDest 11	 New: 44
		*OldSource: 19	 OldDest 5	 New: 24
		*OldSource: 20	 OldDest 5	 New: 40
		
		V'0 http://rembrandt15.uva.netherlight.nl#Rembrandt15
		V'1 http://rembrandt4.uva.netherlight.nl#Rembrandt4
		V'2 http://rembrandt20.uva.netherlight.nl#Rembrandt20
		V'3 http://rembrandt2.uva.netherlight.nl#Rembrandt2
		V'4 http://rembrandt0.uva.netherlight.nl#Rembrandt0
		V'5 http://rembrandt12.uva.netherlight.nl#Rembrandt12
		V'6 http://rembrandt19.uva.netherlight.nl#Rembrandt19
		V'7 http://rembrandt8.uva.netherlight.nl#Rembrandt8
		V'8 http://rembrandt11.uva.netherlight.nl#Rembrandt11
		V'9 http://rembrandt13.uva.netherlight.nl#Rembrandt13
		V'10 http://rembrandt7.uva.netherlight.nl#Rembrandt7
		V'11 http://rembrandt16.uva.netherlight.nl#Rembrandt16
		V'12 http://rembrandt6.uva.netherlight.nl#Rembrandt6
		V'13 http://rembrandt14.uva.netherlight.nl#Rembrandt14
		V'14 http://rembrandt17.uva.netherlight.nl#Rembrandt17
		V'15 http://rembrandt5.uva.netherlight.nl#Rembrandt5
		V'16 http://rembrandt18.uva.netherlight.nl#Rembrandt18
		V'17 http://rembrandt10.uva.netherlight.nl#Rembrandt10
		V'18 http://rembrandt9.uva.netherlight.nl#Rembrandt9
		V'19 http://rembrandt1.uva.netherlight.nl#Rembrandt1
		V'20 http://rembrandt3.uva.netherlight.nl#Rembrandt3
		
		This list gives the order the vertices are created in so the mapping is 1:1. This can be translated to the position within the connection array using
		pos0	V0
		pos1	V16
		pos2	V20
		pos3	V15
		pos4	V19
		pos5	V14
		pos6	V17
		pos7	V12
		pos8	V18
		pos9	V13
		pos10	V9
		pos11	V4
		pos12	V10
		pos13	V1
		pos14	V6
		pos15	V5
		pos16	V8
		pos17	V2
		pos18	V11
		pos19	V7
		pos20	V3
		
		But it doesn't need to be. As soon as the servergraph is converted you just need the transformed node to the original position which should be able
		to hook up sources with sinks. Any thing with multiple conns should be a server. I don't know whether this is a generalisble case. Additionally I'm not
		100% about bringing in other bandwidths. Granted before the transform you know where they all are which probably helps!
		
		*/
		
				
		//System.out.println("Server Info\t"+this.myArrayServerInformation.size());
		//System.out.println(this.myArrayServerInformation);
		
		//System.out.println("Server Connections\t"+this.myArrayServerConnections.size());
		//System.out.println(this.myArrayServerConnections);
		
		//System.out.println("Client Requirements\t"+this.myArrayClientRequirements.size());
		//System.out.println(this.myArrayClientRequirements);
		
		for (int intServerConnectionCounter=0;intServerConnectionCounter<this.myArrayServerConnections.size();intServerConnectionCounter++) {
			
			/*Setup lookup for connections table - cross referenced to vertices number*/
			ConnectionsInformation myConnectionInformation=new ConnectionsInformation();
			
			String stringDestinationClient = (String)((PyList)myArrayServerConnections.get(intServerConnectionCounter)).get(0);
			String stringSourceServer = (String)((PyList)myArrayServerConnections.get(intServerConnectionCounter)).get(1);
			
			myConnectionInformation.setStringSource(stringSourceServer);
			myConnectionInformation.setStringDest(stringDestinationClient);
			
			//System.out.println("Dest: "+stringDestinationClient);
			//System.out.println("Src: "+stringSourceServer);
			
			//Find the server infomation
			double doubleWaitTime=0;
			double doubleContentLength=0;
			double doubleTotalLength=0;
			double doubleDataRate=0;
			double doubleMaxPacketSize=0;
			
			for(int intServerInformationCounter=0;intServerInformationCounter<this.myArrayServerInformation.size();intServerInformationCounter++) {
				String stringServerResource=(String)((PyList)myArrayServerInformation.get(intServerInformationCounter)).get(0);
				if(stringSourceServer.contains(stringServerResource)) {
					doubleTotalLength=((Double)((PyList)myArrayServerInformation.get(intServerInformationCounter)).get(1)).doubleValue();
					doubleContentLength=((Double)((PyList)myArrayServerInformation.get(intServerInformationCounter)).get(2)).doubleValue();
					doubleWaitTime=((Double)((PyList)myArrayServerInformation.get(intServerInformationCounter)).get(3)).doubleValue();
					doubleDataRate=this.calculateDataRate(doubleWaitTime,doubleTotalLength,doubleContentLength);
					doubleMaxPacketSize=this.calculateMaxSize(doubleWaitTime,doubleTotalLength,doubleContentLength);
				}
			}
			
			myConnectionInformation.setTotalLength(doubleTotalLength);
			myConnectionInformation.setContentLength(doubleContentLength);
			myConnectionInformation.setWaitTime(doubleWaitTime);
			
			myConnectionInformation.setMaxDataRate(doubleDataRate);
			myConnectionInformation.setMaxPacketSize(doubleMaxPacketSize);
			
			///////////////////////////////////////////////////////////////////////////////////////////////
			//Re-orientate data to map a server to an original vertex "V0" then onto a mapped server
			//Check code from here to set server map up correctly.
			
			
			int intMappedSourceVertex=-1;
			int intMappedDestVertex=-1;
			
			/*for(int intCounter=0;intCounter<this.myVertexProperties.size();intCounter++) {
				System.out.println(((VertexProperties)this.myVertexProperties.elementAt(intCounter)).getStringIdentifier()+"\t"+((VertexProperties)this.myVertexProperties.elementAt(intCounter)).getMappedDestVertexID().toString()+"\t"+((VertexProperties)this.myVertexProperties.elementAt(intCounter)).getMappedSourceVertexID().toString()+"\t"+((VertexProperties)this.myVertexProperties.elementAt(intCounter)).getOriginalVertexID());
			}*/			

			//Sort out duplicate null entries in the vertexPropertiesList
			/*for(int intCounter=0;intCounter<this.myVertexProperties.size();intCounter++) {
				VertexProperties thisVertexProperties = (VertexProperties)this.myVertexProperties.elementAt(intCounter);
				if(((ArrayList)thisVertexProperties.getMappedSourceVertexID()).size()==0 && ((ArrayList)thisVertexProperties.getMappedDestVertexID()).size()==0) {
					//System.err.println("Removing "+this.myVertexProperties.size());
					this.myVertexProperties.remove(intCounter);
					intCounter--;
				}
			}*/
			
			for(int intCounter=0;intCounter<this.myVertexProperties.size();intCounter++) {
				VertexProperties thisVertexProperties = (VertexProperties)this.myVertexProperties.elementAt(intCounter);
				if(stringSourceServer.contains(thisVertexProperties.getStringIdentifier())) {
					//System.out.println("s\t"+stringSourceServer+"\t"+thisVertexProperties.getStringIdentifier());
					//System.out.println(thisVertexProperties.getMappedSourceVertexID().toString());
					//System.out.println(thisVertexProperties.getMappedDestVertexID().toString());
					intMappedSourceVertex=((Integer)((ArrayList)thisVertexProperties.getMappedSourceVertexID()).get(0)).intValue();
					
				}
				
				if(stringDestinationClient.contains(thisVertexProperties.getStringIdentifier())) {
					//System.out.println("d\t"+stringDestinationClient+"\t"+thisVertexProperties.getStringIdentifier());
					//System.out.println(thisVertexProperties.getMappedSourceVertexID().toString());
					//System.out.println(thisVertexProperties.getMappedDestVertexID().toString());
					intMappedDestVertex=((Integer)((ArrayList)thisVertexProperties.getMappedDestVertexID()).get(0)).intValue();
				}
				
			}
			
			//int intMappedSourceVertex=this.findServer(intOriginalSourceVertices);
			//int intMappedDestVertex=this.findClient(intOriginalDestinationVertices);
			
			/*for(int intTempCounter=0;intTempCounter<intVerticestoVerticesLSMap.length;intTempCounter++) {
				System.out.println(intVerticestoVerticesLSMap[intTempCounter][0]+"\t"+intVerticestoVerticesLSMap[intTempCounter][1]+"\t"+intVerticestoVerticesLSMap[intTempCounter][2]+"\t");
			}*/
			//System.out.println("Src2: "+intMappedSourceVertex+"\t Dest2: "+intMappedDestVertex);
			
			//System.out.println("Sources: "+sources);
			//System.out.println("Sinks: "+sinks);
			//System.out.println("servers: "+servers);
			
			
			//System.out.println("Src2: "+intMappedSourceVertex+"\t Dest2: "+intMappedDestVertex);
			int intServerPosition=findPositionInVector(intMappedSourceVertex, (String)sources.toString());
			int intClientPosition=findPositionInVector(intMappedDestVertex, (String)sinks.toString());
			
			//System.out.println("ServerPos is: "+intServerPosition+" ClientPos is: "+intClientPosition);
			
			myConnectionInformation.setMappedVertSource(intMappedSourceVertex);
			myConnectionInformation.setMappedVertDest(intMappedDestVertex);
			
			int intSourceIterator=0;
			int intSinkIterator=0;
			
			for(Vertex source : sources) {
				for (Vertex sink : sinks) {
					boolean boolConnect=false;
					if((intSourceIterator==intServerPosition)&&(intSinkIterator==intClientPosition))
					{
						boolConnect=true;
					}
					if(boolConnect) {
						List<DirectedEdge> path = shortest_paths.getPath(source, sink);
						if (path.size() > 0) {
							Curve flow_prototype = Curve.createTokenBucket(doubleMaxPacketSize, doubleDataRate);
							na.addFlow(new Flow(source, sink, flow_prototype.copy(), path));
						}
					}
					intSinkIterator++;
				}
				intSourceIterator++;
				intSinkIterator=0;
			}
			
			//Extract the connection requirements
			for(int intCounter=0;intCounter<this.myArrayClientRequirements.size();intCounter++) {
				String stringConnection = ((String)((PyList)myArrayClientRequirements.get(intCounter)).get(0));
				if(stringConnection.contains(myConnectionInformation.getStringDest())) {
					double doubleDelayRequirement= ((Double)((PyList)myArrayClientRequirements.get(intCounter)).get(1)).doubleValue();
					myConnectionInformation.setDelayRequirement(doubleDelayRequirement);				
				}
			}
			
			this.myConnectionsInformation.add(myConnectionInformation);
			
		}
		
		for (int flow_counter=0;flow_counter<na.getFlows().size();flow_counter++) {
			//System.out.println("Flow "+flow_counter+" : " + na.getFlows().get(flow_counter));
			CalculusData results=this.performAnalysis(na,flow_counter);
			int intResultsMappedDest=results.getMappedVertDest();
			int intResultsMappedSource=results.getMappedVertSource();
			
			double doubleResultsMaxDataRate=results.getMaxDataRate();
			double doubleResultsMaxDataSize=results.getMaxDataSize();
			boolean boolResultsSet=false;
			for(int intCounter=0;intCounter<this.myConnectionsInformation.size();intCounter++) {
				
				ConnectionsInformation thisConnection=(ConnectionsInformation)this.myConnectionsInformation.elementAt(intCounter);
				
				
				//System.out.println("Sorted data: ");
				//System.out.println(thisConnection.getMappedVertDest()+"\t"+intResultsMappedDest+"\n"+thisConnection.getMappedVertSource()+"\t"+intResultsMappedSource+"\n"+thisConnection.getMaxDataRate()+"\t"+doubleResultsMaxDataRate+"\n"+thisConnection.getMaxPacketSize()+"\t"+doubleResultsMaxDataSize);
				
				if(!thisConnection.hasResults()) {
					if((thisConnection.getMappedVertDest()==intResultsMappedDest)&&(thisConnection.getMappedVertSource()==intResultsMappedSource)&&(thisConnection.getMaxDataRate()==doubleResultsMaxDataRate)&&(thisConnection.getMaxPacketSize()==doubleResultsMaxDataSize)) {
						thisConnection.setResults(results);
						boolResultsSet=true;
					}
				}
			}
			if(!boolResultsSet) {
				//System.err.println("Unable to add Results");
			}
			
		}
		
		return this.myConnectionsInformation;
			
	}
	
	public CalculusData performAnalysis(NetworkAnalyser na,int intFlowInterest) {
		// select the flow of interest
		
		CalculusData results=new CalculusData();
		
		Flow flow_of_interest = na.getFlows().get(intFlowInterest);
		ArrayList myFlowData=this.processFlowOfInterest(flow_of_interest.toString());
		
		int[] intFlowSourceDest=new int[2];
		double[] doubleFlowParameters=new double[2];
		
		intFlowSourceDest=(int[])myFlowData.get(0);
		doubleFlowParameters=(double[])myFlowData.get(1);
		
		results.setMaxDataSize(doubleFlowParameters[0]);
		results.setMaxDataRate(doubleFlowParameters[1]);
		results.setMappedVertSource(intFlowSourceDest[0]);
		results.setMappedVertDest(intFlowSourceDest[1]);
		
		int intSourceVertex=-1;
		int intDestVertex=-1;
		
		String tempSource=new String();
		
		String tempDest=new String();
		//System.out.println("W ");
		//Find original ids from the VertexProperties
		for(int intCounter=0;intCounter<this.myVertexProperties.size();intCounter++) {
			VertexProperties thisVertexProperties = (VertexProperties)this.myVertexProperties.elementAt(intCounter);
			//System.out.println(intFlowInterest+" Val: "+((ArrayList)thisVertexProperties.getMappedSourceVertexID()).toString());
			int intVertPropMappedSource=-1;
			int intVertPropMappedDest=-1;
			
			if(thisVertexProperties.getMappedSourceVertexID().size()>0) {
				intVertPropMappedSource=((Integer)((ArrayList)thisVertexProperties.getMappedSourceVertexID()).get(0)).intValue();
			}
			
			if(thisVertexProperties.getMappedDestVertexID().size()>0) {
				intVertPropMappedDest=((Integer)((ArrayList)thisVertexProperties.getMappedDestVertexID()).get(0)).intValue();
			}
			
			if(intVertPropMappedSource==results.getMappedVertSource()) {
				intSourceVertex=thisVertexProperties.getPositionInOriginalVetexList();
				tempSource=thisVertexProperties.getStringIdentifier();
			}
			
			if(intVertPropMappedDest==results.getMappedVertDest()) {
				intDestVertex=thisVertexProperties.getPositionInOriginalVetexList();
				tempDest=thisVertexProperties.getStringIdentifier();
			}
			
		}
		
		//System.out.println("SourceT: "+tempSource);
		//System.out.println("DestinationT: "+tempDest);
		
		results.setStringSource(tempSource);
		results.setStringDestination(tempDest);	
		
		//System.out.println("Flow of interest : " + flow_of_interest.toString());
		results.setFlowofInterest(flow_of_interest.toString());
		
		// analyse the network
		Curve beta;
		Map<Vertex,Double> server_bound_map;

		na.setUseFifoService(false);
		na.setUseGamma(false);
		na.setUseExtraGamma(false);

		//System.out.println("--- Charny Bound ---");
		//System.out.println("cur. utilization: "	+ na.getHighestServerUtilization());
		results.setCharnyBoundCurrentUtilisation(na.getHighestServerUtilization());
		
		//System.out.println("max. utilization: " + na.getMaxUtilizationForCharnyBound());
		results.setCharnyBoundMaxUtilisation(na.getMaxUtilizationForCharnyBound());
		
		//System.out.println("delay bound     : " + na.getCharnyDelayBound());
		results.setCharnyBoundDelay(na.getCharnyDelayBound());
		
		server_bound_map = na.getCharnyBacklogBounds();
		if (!server_bound_map.isEmpty()) {
			//System.out.println("backlog bound   : "	+ Collections.max(server_bound_map.values()));
			results.setCharnyBoundBacklogBounds(Collections.max(server_bound_map.values()));
			
			//System.out.println("     per server : " + server_bound_map.values().toString());
			results.setCharnyBoundBacklogPerServer(server_bound_map.values().toString());
			
		} else {
			//System.out.println("backlog bound   : " + Double.POSITIVE_INFINITY);
			results.setCharnyBoundBacklogBounds(Double.POSITIVE_INFINITY);
		}
		//System.out.println();

		//System.out.println("--- Fair Queueing ---");
		beta = na.performFairQueueingAnalysis(flow_of_interest);
		//System.out.println("delay bound     : "	+ Curve.getDelayBound(flow_of_interest.ac, beta));
		results.setFairQueueingDelayBound(Curve.getDelayBound(flow_of_interest.ac, beta));
		
		//System.out.println("backlog bound   : "	+ Curve.getBacklogBound(flow_of_interest.ac, beta));
		results.setFairQueueingBacklogBound(Curve.getBacklogBound(flow_of_interest.ac, beta));
		
		//System.out.println();

		//System.out.println("--- Total Flow Analysis ---");
		server_bound_map.clear();
		double delay = na.performTotalFlowAnalysis(flow_of_interest,
				server_bound_map);
		//System.out.println("delay bound     : " + delay);
		results.setTotalFlowAnalysisDelayBound(delay);		
		
		//System.out.println("backlog bound   : "	+ Collections.max(server_bound_map.values()));
		results.setTotalFlowAnalysisBacklogBound(Collections.max(server_bound_map.values()));
		
		//System.out.println("     per server : " + server_bound_map.values().toString());
		results.setTotalFlowAnalysisPerServer(server_bound_map.values().toString());
		
		//System.out.println();

		//System.out.println("--- Separated Flow Analysis ---");
		beta = na.performSeparatedFlowAnalysis(flow_of_interest);
		//System.out.println("delay bound     : "	+ Curve.getDelayBound(flow_of_interest.ac, beta));
		results.setSeparatedFlowAnalysisDelayBound(Curve.getDelayBound(flow_of_interest.ac, beta));
		
		//System.out.println("backlog bound   : "	+ Curve.getBacklogBound(flow_of_interest.ac, beta));
		results.setSeparatedFlowAnalysisBacklogBound(Curve.getBacklogBound(flow_of_interest.ac, beta));
		
		//System.out.println();

		//System.out.println("--- PMOO Analysis ---");
		beta = na.performPMOOAnalysis(flow_of_interest);
		//System.out.println("PMOO service curve: " + beta);
		results.setPMOOAnalysisServiceCurve(beta);
		
		//System.out.println("delay bound     : "	+ Curve.getDelayBound(flow_of_interest.ac, beta));
		results.setPMOOAnalysisDelayBound(Curve.getDelayBound(flow_of_interest.ac, beta));
		
		//System.out.println("backlog bound   : "	+ Curve.getBacklogBound(flow_of_interest.ac, beta));
		results.setPMOOAnalysisBacklogBound(Curve.getBacklogBound(flow_of_interest.ac, beta));
		
		//System.out.println();
		
		return results;
	}
	
	private ArrayList processFlowOfInterest(String stringFlowOfInterest) {
		String[] stringFlowSourceDest=new String[2];
		String[] stringFlowParameters=new String[2];
		int[] intFlowSourceDest=new int[2];
		double[] doubleFlowParameters=new double[2];
		
		String delimsLS = "[, =(){}]+";
		
		String[] tokensLS = stringFlowOfInterest.split(delimsLS);
		
		//System.out.println("Flow Split: ");
		
		/*for(int intCounter=0;intCounter<tokensLS.length;intCounter++) {
			System.out.println(tokensLS[intCounter]);
		}*/
		
		stringFlowSourceDest[0]=tokensLS[2].substring(1,(tokensLS[2].length()));
		stringFlowSourceDest[1]=tokensLS[3].substring(1,(tokensLS[3].length()));
		
		stringFlowParameters[0]=tokensLS[9].substring(0,(tokensLS[9].length()));
		stringFlowParameters[1]=tokensLS[10].substring(0,(tokensLS[10].length()));
		
		intFlowSourceDest[0]=(new Integer(stringFlowSourceDest[0])).intValue();
		intFlowSourceDest[1]=(new Integer(stringFlowSourceDest[1])).intValue();
		
		doubleFlowParameters[0]=(new Double(stringFlowParameters[0])).doubleValue();
		doubleFlowParameters[1]=(new Double(stringFlowParameters[1])).doubleValue();
		
		@SuppressWarnings("rawtypes")
		ArrayList myParameters = new ArrayList();
		
		myParameters.add(intFlowSourceDest);
		myParameters.add(doubleFlowParameters);
		
		return myParameters;
	}
	
	private int findPositionInVector(int intNumber, String vectorToHuntIn) {
		//Vector is expected to be server or client list so it'll be pre-fixed with a V
		int intPosition=-1;
		
		String stringToSearch="V"+intNumber;
		
		String phraseEdges = vectorToHuntIn;
		phraseEdges=phraseEdges.substring(1,(phraseEdges.length()-1));
		String delimsEdges = "[, ()]+";
		String[] tokensEdges = phraseEdges.split(delimsEdges);
		
		for(int intCounter=0;intCounter<tokensEdges.length;intCounter++) {
			
			//System.out.println("tok: "+tokensEdges[intCounter]+ " ToSearch: "+stringToSearch);
			if(tokensEdges[intCounter].contains(stringToSearch)) {
				intPosition=intCounter;
			}
		}
		
		if(intPosition==-1) {
			//System.out.println("Can't Set "+intNumber);
		}
		
		return intPosition;
	}
	
	private double calculateDataRate(double doubleWait, double doublePacketLength, double doubleContentLength) {
		double doubleDataRate=0;
		//Data sent is doubleContentLength every doubleWait Seconds but this is actually doublePacketLength Bytes
		double doubleBytesPerSecond=doublePacketLength/doubleWait;
		double doubleBitsPerSecond=doubleBytesPerSecond*8;
		
		doubleDataRate=doubleBitsPerSecond;
		//doubleDataRate=522890;
		//System.out.println(doubleDataRate);
		
		return doubleDataRate;
	}
	
	private double calculateMaxSize(double doubleWait, double doublePacketLength, double doubleContentLength) {
		double doubleMaxBitSize=0;
		//Max Bit size, i.e. packets at once assumed to be packetlength
		doubleMaxBitSize=doublePacketLength*8;
		//doubleMaxBitSize=10752;
		//System.out.println(doubleMaxBitSize);
		
		return doubleMaxBitSize;
	}
	
	public Vector getConnectionsInformation() {
		return this.myConnectionsInformation;
	}
	
	public void cleanUpNetwork() {
		this.network_graph.removeAllEdges();
		this.network_graph.removeAllVertices();
		
		this.server_graph.removeAllEdges();
		this.server_graph.removeAllVertices();
		
		System.out.println("Edges: "+this.network_graph.getEdges().toString());
		System.out.println("Vertices: "+this.network_graph.getVertices().toString());
		System.out.println("Edges: "+this.server_graph.getEdges().toString());
		System.out.println("Vertices: "+this.server_graph.getVertices().toString());
	}
}
