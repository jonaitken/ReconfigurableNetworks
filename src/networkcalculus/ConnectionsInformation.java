package networkcalculus;

public class ConnectionsInformation {

	private String stringSource;
	private String stringDest;
	
	private int intMappedVertSource;
	private int intMappedVertDest;
	
	private double doubleWaitTime=0;
	private double doubleContentLength=0;
	private double doubleTotalLength=0;
	private double doubleDataRate=0;
	private double doubleMaxPacketSize=0;
	
	private double doubleDelayRequirement;
	
	private boolean boolHasResults=false;
	
	private boolean boolHasRequirement=false;
	
	private CalculusData dataResults;
	
	public ConnectionsInformation() {
		
	}
	
	public boolean hasResults() {
		return this.boolHasResults;
	}
	
	public boolean hasRequirement() {
		return this.boolHasRequirement;
	}
	
	public void setResults(CalculusData a) {
		this.dataResults=a;
		this.boolHasResults=true;
	}
	
	public CalculusData getResults() {
		return this.dataResults;
	}
	
	public void setDelayRequirement(double a) {
		this.doubleDelayRequirement=a;
		this.boolHasRequirement=true;
	}
	
	public double getDelayRequirement() {
		return this.doubleDelayRequirement;
	}
	
	public void setWaitTime(double a) {
		this.doubleWaitTime=a;
	}
	
	public double getWaitTime() {
		return this.doubleWaitTime;
	}
	
	public void setContentLength(double a) {
		this.doubleContentLength=a;
	}
	
	public double getContentLength() {
		return this.doubleContentLength;
	}
	
	public void setTotalLength(double a) {
		this.doubleTotalLength=a;
	}
	
	public double getTotalLength() {
		return this.doubleTotalLength;
	}
	
	public void setMaxDataRate(double a) {
		this.doubleDataRate=a;
	}
	
	public double getMaxDataRate() {
		return this.doubleDataRate;
	}
	
	public void setMaxPacketSize(double a) {
		this.doubleMaxPacketSize=a;
	}
	
	public double getMaxPacketSize() {
		return this.doubleMaxPacketSize;
	}
	
	public void setStringSource(String a) {
		this.stringSource=new String(a);
	}
	
	public String getStringSource() {
		return this.stringSource;
	}
	
	public void setStringDest(String a) {
		this.stringDest=new String(a);
	}
	
	public String getStringDest() {
		return this.stringDest;
	}
	
	public void setMappedVertSource(int a) {
		this.intMappedVertSource=a;
	}
	
	public int getMappedVertSource() {
		return this.intMappedVertSource;
	}
	
	public void setMappedVertDest(int a) {
		this.intMappedVertDest=a;
	}
	
	public int getMappedVertDest() {
		return this.intMappedVertDest;
	}
	
	public double getCharnyHeadroom() {
		return (this.doubleDelayRequirement-this.dataResults.getCharnyBoundDelay());
	}
	
	public double getFairQueueingHeadroom() {
		return (this.doubleDelayRequirement-this.dataResults.getFairQueueingDelayBound());
	}
	
	public double getPMOOAnalysisHeadroom() {
		return (this.doubleDelayRequirement-this.dataResults.getPMOOAnalysisDelayBound());
	}
	
	public double getSeparatedFlowHeadroom() {
		return (this.doubleDelayRequirement-this.dataResults.getSeparatedFlowAnalysisDelayBound());
	}
	
	public double getTotalFlowHeadroom() {
		return (this.doubleDelayRequirement-this.dataResults.getTotalFlowAnalysisDelayBound());
	}
	
	public boolean testCharnyDelayBound() {
		boolean boolPass=true;
		if(this.dataResults.getCharnyBoundDelay()>this.doubleDelayRequirement) {
    		boolPass=false;
    	}
		return boolPass;
	}
	
	public boolean testFairQueueingDelayBound() {
		boolean boolPass=true;
		if(this.dataResults.getFairQueueingDelayBound()>this.doubleDelayRequirement) {
    		boolPass=false;
    	}
		return boolPass;
	}
	
	public boolean testPMOOAnalysisDelayBound() {
		boolean boolPass=true;
		if(this.dataResults.getPMOOAnalysisDelayBound()>this.doubleDelayRequirement) {
    		boolPass=false;
    	}
		return boolPass;		
	}
	
	public boolean testSeparatedFlowAnalysisDelayBound() {
		boolean boolPass=true;
		if(this.dataResults.getSeparatedFlowAnalysisDelayBound()>this.doubleDelayRequirement) {
    		boolPass=false;
    	}
		return boolPass;
	}
	
	public boolean testTotalFlowAnalysisDelayBound() {
		boolean boolPass=true;
		if(this.dataResults.getTotalFlowAnalysisDelayBound()>this.doubleDelayRequirement) {
    		boolPass=false;
    	}
		return boolPass;
	}
	
	
}
