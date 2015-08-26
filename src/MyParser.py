from jython.interfaces import MyParserType
from pynt.input.rdf import *
from pynt.output.rdf import *
from shutil import *

class MyParser(MyParserType, object):
    ''' Parser '''

    def __init__(self):
        print 'Initialising'
        #self.filelocation="newfile.rdf"
        #self.filelocation="lighthouse7RandomSmall.rdf"
        #self.filelocation="lighthouse40.rdf"
        self.filelocation="Out.rdf"
        copyfile("Out.rdf","Results/Out.rdf")
        copyfile("src/evolutionAspects/parametersMoo.params","Results/parametersMoo.params")
        #self.filelocation="lighthouse4c.rdf"
        #self.filelocation="lighthouse4c.rdf"
        #print(self.filelocation)
        #self.setFileLocation("lighthouse.rdf")
        pass

    def parse(self):
    	print(self.filelocation)
        self.y=RDFSchemaFetcher(self.filelocation)
        self.y.buildSourceQueue()
        self.y.retrieve()
        print("Out")
        

    def setFileLocation(self, newFileLocation):
        self.fileLocation=newFileLocation

    def getNumDevices(self):
        return self.y.getNumDevices()

    def getDeviceConnections(self, deviceNumber):
        return self.y.getDeviceConnections(deviceNumber)

    def getConnectionsCapacity(self):
        return self.y.getConnectionsCapacity()

    def getConnectionsLatency(self):
        return self.y.getConnectionsLatency()

    def getVideoServerInformation(self):
        return self.y.getVideoServerInformation()

    def getVideoClientInformation(self, deviceNumber):
        return self.y.getVideoClientInformation(deviceNumber)

    def getDeviceNames(self):
        return self.y.getDeviceNames()

    def getVideoServerConnectionInformation(self):
        return self.y.getVideoServerConnectionInformation()

    def getVideoClientRequirements(self):
        return self.y.getVideoClientRequirements()

    def getPhysicalConnectionInformation(self):
        return self.y.getPhysicalConnectionInformation()

    def getTotalInterfaceList(self):
        return self.y.getTotalInterfaceList()

    def getLocations(self):
        return self.y.getLocations()
