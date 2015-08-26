package jython.maincode;

import networkcalculus.NetworkCalculusManager;


public class Main {

    @SuppressWarnings("unchecked")
	public static void main(String[] args) {
    	
    	NetworkCalculusManager myManager= new NetworkCalculusManager();
    	//myManager.innerLoop();
    	
    	myManager.middleLoop();
    	
    }
    
}

