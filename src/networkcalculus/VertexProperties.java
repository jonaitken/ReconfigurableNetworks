package networkcalculus;

import java.util.ArrayList;

public class VertexProperties {

	private String stringIdentifer;
	private int intOriginalVertexID;
	private ArrayList intMappedSourceVertexID=new ArrayList();
	private ArrayList intMappedDestVertexID=new ArrayList();
	
	private int intPositionInOriginalVertexList;
	
	
	public VertexProperties() {
				
	}
	
	public void setStringIdentifier(String id) {
		this.stringIdentifer=new String(id);
	}
	
	public String getStringIdentifier() {
		return this.stringIdentifer;
	}
	
	public void setOriginalVertexID(int id) {
		this.intOriginalVertexID=id;
	}
	
	public int getOriginalVertexID() {
		return this.intOriginalVertexID;
	}
	
	public void addMappedSourceVertexID(int id) {
		this.intMappedSourceVertexID.add(id);
	}
	
	public ArrayList getMappedSourceVertexID() {
		return this.intMappedSourceVertexID;
	}
	
	public void addMappedDestVertexID(int id) {
		this.intMappedDestVertexID.add(id);
	}
	
	public ArrayList getMappedDestVertexID() {
		return this.intMappedDestVertexID;
	}
	
	public void setPositionInOriginalVetexList(int a) {
		this.intPositionInOriginalVertexList=a;
	}
	
	public int getPositionInOriginalVetexList() {
		return this.intPositionInOriginalVertexList;
	}
	
}
