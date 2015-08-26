#from jython.interfaces import MyParserType
from pynt.input.rdf import *
from pynt.output.rdf import *

print 'Initialising'
filelocation="lighthouse.rdf"
print(filelocation)
y=RDFSchemaFetcher(filelocation)
y.buildSourceQueue()
y.retrieve()

cons=y.getAllConnections()
print("Out")
