# -*- coding: utf-8 -*-
"""RDF module -- Input from and Output to RDF using rdflib"""

# builtin modules
import logging
import xml.sax
import jarray
import java.util.ArrayList as ArrayList

# semi-standard modules
try:
    import rdflib
    if map(int, rdflib.__version__.split(".")) >= [3, 0, 0]:
        from rdflib import Literal
        from rdflib import ConjunctiveGraph as Graph
    elif map(int, rdflib.__version__.split(".")) > [2, 1, 0]:
        from rdflib.Graph import Graph
        from rdflib.Literal import Literal
except ImportError:
    raise ImportError("Module rdflib is not available. It can be downloaded from http://rdflib.net/\n")
# local modules
import pynt.layers
import pynt.elements
import pynt.xmlns
import pynt.input
import pynt.rangeset
import threading
import pynt.technologies
import sys
import os

class UnkownURI(Exception):
    """Exception raised when an URI is not mentioned in the graph"""
    pass

# Global lock for all fetcher objects. Apparently globals don't work between modules/files.
fetcherlock = threading.Lock()
logger = logging.getLogger("pynt.input")
logger.debug("Created global lock object %s" % fetcherlock)

# RDF Fetcher objects. Apparently globals don't work between modules/files.
rdffetcherobjects = {}

# This function put here, since it is related to the RDFFetcher class
# TODO: deprecte GetCreate* and replace with override of __metaclass__(type), __call__(cls, *args, **kwargs) and 
# __new__(klass, self,url, identifier=None, nsuri=None, *args, **kwargs) in class RDFFetcher
def GetCreateRDFFetcher(fetcherclass, url, identifier=None, nsuri=None):
    """This function can be used to acquire fetcher objects. When called through this function,
    fetchers are created and globally registered. To be able to identify fetcher objects,
    the source url is mandatory. This function adds thread safety to this module.
    If a fetcher for a certain namespace already exists, that fetcher object is returned rather
    than a new fetcher object. If using this function to create objects, it is advised not to 
    create any other RDF Fetcher objects manually."""
    
    # TODO: should this be moved to pynt/input/__init__? (That lock is per-fetcher. Likely that needs to be global.)
    logger = logging.getLogger("pynt.input")
    
    global rdffetcherobjects
    global fetcherlock
    
    if url == "":
        raise ConsistencyException("No url provided for GetCreateRDFFetcher")
    
    if url not in rdffetcherobjects:
        fetcherlock.acquire()
        if url in rdffetcherobjects: # The object was created in the past few cycles
            fetcherlock.release()
            return rdffetcherobjects(url)
        
        # Create a new threadsafe object
        fetcher = fetcherclass(url, identifier, nsuri)
        fetcher.setSourceURL(url)
        logger.debug("Created fetcher object for url %s" % url)
        rdffetcherobjects[url] = fetcher
        
        fetcherlock.release()
        return fetcher
    
    else:
        return rdffetcherobjects(url)
    

# If workonline is False, known remote prefixes are changes to local file paths
workonline = False

def setWorkOnline(online):
    global workonline
    logger = logging.getLogger("pynt.input")
    workonline = bool(online)
    if workonline:
        logger.info("Working online")
    else:
        logger.info("Working offline")
        

remoteprefixes = {
    "http://www.science.uva.nl/research/sne/schema/": os.path.realpath(os.path.join(os.path.dirname(__file__), '../../schema/rdf/')),
    "http://ndl.uva.netherlight.nl/config/": os.path.realpath(os.path.join(os.path.dirname(__file__), '../../network-examples/config/')),
    "http://ndl.uva.netherlight.nl/lmontest/": os.path.realpath(os.path.join(os.path.dirname(__file__), '../../network-examples/lmontest/')),
}

def RemoteUrlToLocalUrl(url):
    """Change online URL's to Local URL's, if possible. This avoids (unreliable) Internet access.
    This is a temporary fix because the schema files on www.science.uva.nl are hardly reachable."""
    global workonline
    if workonline:
        return url
    # Try to find the url in the local schema directory, because we do not trust
    # science.uva.nl right now.
    logger = logging.getLogger("pynt.input")
    if not ( (type(url) == str) or isinstance(url, rdflib.URIRef) ):
        logger.warning("Unknown URL type %s" % type(url))
        return url
    global remoteprefixes
    for prefix in remoteprefixes:
        if url.startswith(prefix):
            localfile = os.path.join(remoteprefixes[prefix],url[len(prefix):])
            if os.path.isfile(localfile):
                logger.log(25, "Replace remote URL %s with local source %s" % (url, localfile))
                return localfile
            else:
                logger.warning("Can not use local source %s: file not found" % localfile)
    if url.startswith('http://') or url.startswith('https://'):
        logger.warning("Local source for remote URL %s not found" % url)
    return url


class RDFFetcher(pynt.input.BaseRecursiveFetcher):
    """Input from RDF using librdf. Fetches the current RDF files, but does 
       not yet parse the information within.
       
       This class is merely a generic class that defines functions that can 
       be used by its subclasses. To fetch specific RDF objects, subclasses 
       have been defined."""

    graph        = None  # the graph with the NDL data.
    url          = ""    # filename or URL
    sourceparsed = False # Indicates if we already parsed the source for this fetcher
    
    #def __init__(self, source, threadsafe=False):
    #    """Override the init function to allow that we let the RDF file determine the URI 
    #    of the subject (this can only be done if there is only one subject of a certain kind)"""
    #    pynt.input.BaseRecursiveFetcher.__init__(self, source, threadsafe=threadsafe)
    
    def getSubject(self):
        if self.subject == None:
            self.fetch()
        if self.subject == None:
            raise pynt.input.ParsingException("fetch() did not create a new subject")
        return self.subject
    def setSource(self, source):
        self.url = source
    def getSource(self):
        return self.url
    def setSourceURL(self, url):
        self.url = url
    def setSourceFile(self, filename, hostname=None):
        self.url = filename
    
    def open(self):
        """This function should take into account if this source has
           already been parsed into a graph."""
        logger = logging.getLogger("pynt.input")
        if not self.url:
            raise RuntimeError("Call setSourceURL() or setSourceFile() before calling getSubject() or fetch() of a Fetcher instance")
        if self.sourceparsed:
            logger.debug("Source %s was already parsed into a graph, skipping." % self.url)
            return
        self.graph = Graph()
        try:
            logger.log(25,"Parsing RDF input %s using %s" % (self.url, self.__class__.__name__))
            url = RemoteUrlToLocalUrl(self.url)
            self.graph.parse(url)
            # We could also check against self.graph, but it is probably
            # better to use a flag to indicate that we already parsed
            # this source into the graph
            self.sourceparsed = True
            #print(self.graph)
        except OSError:
            raise OSError("File/URL %s doesn't exist, or can't be opened" % (self.url))
        except xml.sax._exceptions.SAXParseException, e:
            raise pynt.input.ParsingException("File/URL %s is not a valid XML file: %s" % (self.url, e))
        except rdflib.exceptions.ParserError, e:
            raise pynt.input.ParsingException("File/URL %s is not a valid RDF file: %s" % (self.url, e))
    
    def close(self):
        # Avoid possible bugs if we try to parse it again.
        self.sourceparsed = False
        self.graph = None
    
    # This function is not necessary, an abstract already exists in
    # input.BaseFetcher
    #def retrieve(self):
    #    pass
    
    def IdentifierFromRDF(self, rootobject=None):
        pass
    
    def GetRDFLibNamespace(self, prefix=None, uri=None):
        if uri:
            namespace = pynt.xmlns.GetCreateNamespace(uri=uri, prefix=prefix)
        else:
            namespace = pynt.xmlns.GetCreateWellKnownNamespace(prefix, uri=uri)
        return rdflib.Namespace(namespace.uri)
    
    def GetParentClasses(self, classuri):
        """Returns a list of parent classes to which a certain class belongs.
        The argument may be a uri, or uri list.
        The return value is a list of rdflib.URIRef.
        If the resource is not found, raise an UnkownURI exception.
        Warning: it does only take subclasses into account if those subclasses are defined in the current graph!"""
        rdfs = self.GetRDFLibNamespace(prefix="rdfs")
        if isinstance(classuri, list):
            unchecked = classuri[:]  # make a copy
        else:
            unchecked = [rdflib.URIRef(classuri)]
        # Find parent classes:
        #   uri         rdfs:subClassOf     superclass
        #   superclass  rdfs:subClassOf     supersuperclass
        # (possible nested)
        classes = [] # classes contains types, which we did check for parent classes.
        while len(unchecked):
            rdfclass = unchecked.pop()
            superclasses = list(self.graph.objects(rdfclass, rdfs["subClassOf"]))
            classes.append(rdfclass)  # we checked rdfclass
            unchecked.extend(superclasses)  # also check subclasses of subclasses
        return classes
    
    def GetParentProperties(self, propertyuri):
        """Returns a list of parent classes to which a certain class belongs.
        The argument may be a uri, or uri list.
        The return value is a list of rdflib.URIRef.
        If the resource is not found, raise an UnkownURI exception.
        Warning: it does only subproperties into account if those are defined in the current graph!"""
        rdfs = self.GetRDFLibNamespace(prefix="rdfs")
        if isinstance(propertyuri, list):
            unchecked = propertyuri[:]  # make a copy
        else:
            unchecked = [rdflib.URIRef(propertyuri)]
        # Find parent classes:
        #   uri         rdfs:subClassOf     superclass
        #   superclass  rdfs:subClassOf     supersuperclass
        # (possible nested)
        properties = [] # classes contains types, which we did check for parent classes.
        while len(unchecked):
            rdfclass = unchecked.pop()
            superclasses = list(self.graph.objects(rdfclass, rdfs["subPropertyOf"]))
            properties.append(rdfclass)  # we checked rdfclass
            unchecked.extend(superclasses)  # also check subclasses of subclasses
        return properties
    
    def GetTypes(self, resourceuri):
        """Returns a list of classes to which a certain resource belongs.
        The return value is a list of rdflib.URIRef.
        If the resource is not found, raise an UnkownURI exception.
        uri MUST be a rdflib.URIRef, not a string.
        Warning: it does only subclasses into account if defined in the current graph!"""
        # Step 1. Find directly defined classes:
        #   uri         rdf:type            class
        rdf  = self.GetRDFLibNamespace(prefix="rdf")
        assert(isinstance(resourceuri, rdflib.URIRef))
        types = list(self.graph.objects(resourceuri, rdf["type"]))
        if len(types) == 0:
            # uri has no types or does not exist
            properties = list(self.graph.predicate_objects(resourceuri))
            if len(properties) == 0:
                raise UnkownURI("Could not find subject %s in RDF source %s" % (resourceuri, self.url))
        # Step 2. Find parent classes:
        return self.GetParentClasses(types)
    
    def getLayerURIInList(self, urilist):
        """Given a list of uris, pick the one that is of type layer:Layer. If multiple 
        uris in the list have that property, simply return the first one. Returns None 
        if no uri is a layer."""
        typeuri  = self.GetRDFLibNamespace(prefix="rdf")["type"]
        layeruri = self.GetRDFLibNamespace(prefix="layer")["Layer"]
        for uri in urilist:
            if (uri, typeuri, layeruri) in self.graph:
                return uri
        return None
    
    def getLayerFromList(self, urilist):
        """Given a list of uris, try to find one of them which is a Layer.
        This not only searches for uri type Layer, but also for uri in 
        the list of already defined RDFObjects."""
        # get classes of a network element. Hopefully one is a layer (e.g. rdf:type MylayerNetworkElement)
        layeruri = self.getLayerURIInList(urilist)
        if layeruri:
            (namespace, identifier) = pynt.xmlns.splitURI(layeruri)
            layer = pynt.layers.GetCreateLayer(identifier, namespace)
            return layer
        for typeuri in urilist:
            (namespace, identifier) = pynt.xmlns.splitURI(typeuri)
            try:
                layer = pynt.xmlns.GetRDFObject(identifier, namespace)
                if isinstance(layer, pynt.layers.Layer):
                    return layer
            except pynt.xmlns.UndefinedNamespaceException:
                pass
        # This is way too verbose for a mere helper function. The caller 
        # function should log the warning.
        #logger = logging.getLogger("pynt.input")
        #logger.warning("Could not find any layer in the list %s. Perhaps you need to import a schema first." % urilist)
        return None
    
    def getLayer(self, neuri):
        """Given the uri of a NetworkElement, try to find the layer of this network element.
        First, a list of types (classes and superclasses) is retrieved, and they are checked 
        to see if one of them is of type layer:Layer. If that fails, we check if one of the types 
        is registered as a xmlns.Layer object. returns a Layer object (not just the URI)"""
        # get classes of a network element. Hopefully one is a layer (e.g. rdf:type MylayerNetworkElement)
        types = self.GetTypes(neuri)
        layer = self.getLayerFromList(types)
        return layer
    
    def IsType(self, uri, rdftype):
        """Check if a subject with the given uri is present in self.graph.
        If not, raise an UnkownURI exception. If found, verify if it is of the 
        correct class (rdftype)."""
        types = self.GetTypes(uri)
        return rdftype in types
    
    def predicateinlist(self, predicate, propertylist=None, ignorepredicates=None, namespacelist=None):
        """Helper function for SetRDFProperties(). Returns True if a predicate is in propertylist
        or in namespacelist, and not in ignorepredicates."""
        if propertylist == None and ignorepredicates == None and namespacelist == None:
            # If neither of these are set, we should return true, because otherwise the property
            # will not be added if there is no search list set.
            return True
        if ignorepredicates and (predicate in ignorepredicates):
            return False
        if propertylist and (predicate in propertylist):
            return True
        if namespacelist:
            (namespace, identifier) = pynt.xmlns.splitURI(predicate)
            if namespace.uri in namespacelist:
                return True
        return False
    
    def SetRDFProperties(self, subject, propertylist=None, ignorepredicates=None, namespacelist=None):
        """Given the subject, get all predicates, manually set non technology specific properties
        and add unknown (technology specific) properties to the subject.
        If propertylist or namespacelist is set, restrict search to these predicates."""
        logger = logging.getLogger("pynt.input")
        subjecturi = rdflib.URIRef(subject.getURIdentifier())
        tuples = list(self.graph.predicate_objects(subjecturi))
        logger.debug("Found %d RDF predicates for object %s" % (len(tuples), subjecturi))
        rdf  = self.GetRDFLibNamespace(prefix="rdf")
        rdfs = self.GetRDFLibNamespace(prefix="rdfs")
        ndl  = self.GetRDFLibNamespace(prefix="ndl")
        dc   = self.GetRDFLibNamespace(prefix="dc")
        
        for (predicate, value) in tuples:
            #print "%s --> %s --> %s" % (subjecturi, predicate, value)
            if not self.predicateinlist(predicate, propertylist=propertylist, ignorepredicates=ignorepredicates, namespacelist=namespacelist):
                # NOTE: this generates a lot of messages.
                #logger.debug("Ignore RDF predicate %s for object %s" % (predicate, subject.getURIdentifier()))
                continue
            logger.debug("Setting RDF predicate %s for object %s" % (predicate, subject.getURIdentifier()))
            
            # NOTE:   using str(predicate) doesn't seem to work when comparing
            #         to namespaces like this so I (Niels) removed them all 
            #         from the conditions.
            # NOTE 2: also, when comparing a string to a -list- of strings,
            #         it looks like you DO need the str() wrapped around the
            #         string
            if predicate == rdf["type"]:
                if str(value) == ndl["ConfiguredInterface"]:
                    subject.removable = True
                # else:
                #     logger.debug("Interface %s is of type %s" % (self.getURIdentifier(), repr(value)))
            elif predicate == ndl["switchedTo"]:
                (namespace, identifier) = pynt.xmlns.splitURI(value)
                peerinterface = subject.getCreateSwitchedInterface(identifier=identifier, namespace=namespace)
                subject.addSwitchedInterface(peerinterface)
                logger.debug("Added switched interface from %s to %s" % (subjecturi, value))
            elif predicate == ndl["packetSwitchedTo"]:
                (namespace, identifier) = pynt.xmlns.splitURI(value)
                peerinterface = subject.getCreateSwitchedInterface(identifier=identifier, namespace=namespace)
                subject.addPacketSwitchedInterface(peerinterface)
                logger.debug("Added packetswitched interface from %s to %s" % (subjecturi, value))
            elif predicate == ndl["circuitSwitchedTo"]:
                (namespace, identifier) = pynt.xmlns.splitURI(value)
                peerinterface = subject.getCreateSwitchedInterface(identifier=identifier, namespace=namespace)
                subject.addCircuitSwitchedInterface(peerinterface)
                logger.debug("Added circuit interface from %s to %s" % (subjecturi, value))
            elif predicate == ndl["linkTo"]:
                (namespace, identifier) = pynt.xmlns.splitURI(value)
                logger.info("Skipping property %s for %s, this is handled elsewhere." % (str(predicate), identifier)) # this we have to implement later
            elif predicate == ndl["connectedTo"]:
                (namespace, identifier) = pynt.xmlns.splitURI(value)
                logger.info("Skipping property %s for %s, this is handled elsewhere." % (str(predicate), identifier)) # this we have to implement later
            elif predicate == ndl["capacity"]:
                subject.setCapacity(value)
            # See NOTE 2 above!
            elif str(predicate) in ["http://www.science.uva.nl/research/sne/ndl#name",
                                    "http://www.w3.org/2000/01/rdf-schema#label", ]:
                subject.setName(value)
            elif str(predicate) in ["http://www.science.uva.nl/research/sne/ndl#description",
                                    "http://www.w3.org/2000/01/rdf-schema#comment",
                                    "http://purl.org/dc/elements/1.1/description", # FIXME: this should be fixed in the RDF
                                    "http://purl.org/dc/elements/1.1/#description", ]:
                subject.setDescription(value)
            else: # here we add any unknown (technology specific) predicates
                # Check if the property type is defined for the subject by checking
                # if it is part of the layer
                try: 
                    subjectlayer = subject.getLayer()
                except AttributeError:
                    logger.warning("Subject %s is not layer aware, skipping predicate %s" % (subject.getURIdentifier(), predicate))
                    continue
                if subjectlayer == None:
                    logger.warning("No layer defined for subject %s" % subject.getURIdentifier());
                    continue
                (namespace, identifier) = pynt.xmlns.splitURI(predicate)

                # First check if this is an adaptation function
                adaptation = pynt.layers.GetAdaptationFunction(identifier, namespace)
                # Ignore adapatations, we process those later because all interfaces of a device have to exist
                # first, we need the layer information
                if adaptation != None: 
                    continue
                
                # Find the property in the layer properties
                if subjectlayer.hasProperty(identifier):
                    prop = subjectlayer.getProperty(identifier)
                else: 
                    prop = None

                #propvalue = subjectlayer.getPropertyValue(identifier)
                if prop == None:
                    logger.warning("Ignoring property %s which is not defined on layer %s" % (predicate, subjectlayer.getName()))
                    #raise pynt.ConsistencyException("Where is the stack going?")
                    continue
                
                # Add the property to the subject, in a dict where we map property type to the property value
                # The addproperty method of the subject is supposed to do something sensible while setting
                # the property, such as double-checking if the property is valid for the layer of the object
                logger.debug("Setting property %s for %s to %s" % (predicate, subject.getURIdentifier(), value))
                try:
                    subject.addProperty(identifier, value)
                except AttributeError:
                    logger.warning("addProperty not implemented for subject %s" % type(subject))
                    continue
    
    def retrieveObject(self, objecturi, objectClass):
        """Just return the object itself, without setting anything else."""
        assert(issubclass(objectClass, pynt.xmlns.RDFObject))
        (namespace, identifier) = pynt.xmlns.splitURI(objecturi)
        logger = logging.getLogger("pynt.input")
        logger.debug("retrieving %s %s" % (objectClass.__name__, objecturi))
        return objectClass(identifier, namespace)
    
    def retrieveAndSetObject(self, objecturi, objectClass):
        theobject = self.retrieveObject(objecturi, objectClass)
        # self.SetRDFProperties(layer)
        self.SetRDFNameDescription(theobject)
        # This is done elsewhere
        #self.SetRDFSeeAlso(theobject)
        return theobject
    
    def SetRDFNameDescription(self, subject):
        """Given the subject, set the name and description, based on its RDF predicates."""
        subjecturi = rdflib.URIRef(subject.getURIdentifier())
        tuples = list(self.graph.predicate_objects(subjecturi))
        for (predicate, objectvalue) in tuples:
            try:
                # type of objectvalue is either rdflib.Literal.Literal or rdflib.URIRef.URIRef
                pynt.xmlns.RDFObject.setRDFProperty(subject, predicate, objectvalue)
            except pynt.xmlns.UndefinedNamespaceException:
                pass
    
    def GetRDFSeeAlso(self, subjecturi):
        """Given the subject, return a list of seeAlso URLs, based on its RDF predicates."""
        rdfs = self.GetRDFLibNamespace(prefix="rdfs")
        subjecturi = rdflib.URIRef(subjecturi) # make sure it is not a string
        seealsos = list(self.graph.objects(subjecturi, rdfs["seeAlso"]))
        seealsos.extend(list(self.graph.objects(subjecturi, rdfs["isDefinedBy"])))
        return seealsos
    
    def SetRDFSeeAlso(self, subject):
        """Set the seeAlso URLs in the object based on its RDF predicates, but do not fetch it."""
        subjecturi = rdflib.URIRef(subject.getURIdentifier())
        seealsos = self.GetRDFSeeAlso(subjecturi)
        for seealso in seealsos:
            self.attachSource(str(seealso), subject)
    
    def QueueRDFSeeAlso(self, subjecturi, fetcherclass=None, prepend=False):
        """Given the subject, find the seeAlso URLs and add them to the queue for fetching."""
        seealsos = self.GetRDFSeeAlso(subjecturi)
        if fetcherclass == None:
            fetcherclass = self.__class__
        for seealso in seealsos:
            self.queueSource(str(seealso), fetcherclass, prepend)
    
    # TODO: I am pretty sure this function is not used in any place
    #def SetRdfProperty(self, subject, predicateuri, setmethod=None, getmethod=None, setvariable=None, verifyAttributes=False):
    #    """Given an RDFObject, check if there is exactly one property with the given predicate, 
    #    and set that to the subject with the given setter function. If no property is found, 
    #    simply return. If multiple properties are found, raise a parsing exception."""
    #    assert(setmethod or setvariable) # either one must be set
    #    subjecturi = rdflib.URIRef(subject.getURIdentifier())
    #    assert(isinstance(predicateuri, rdflib.URIRef))
    #    objects = list(self.graph.objects(subjecturi, predicateuri))
    #    if len(objects) > 1:
    #        raise pynt.input.CommandFailed("Found %d %s properties of %s. Only one allowed." % (len(objects), predicateuri, subjecturi))
    #    if len(objects) == 0:
    #        return
    #    value = objects[0]
    #    if verifyAttributes:
    #        assert(getmethod or setvariable)  # either one must be set if verifyAttributes is true
    #        if getmethod:
    #            oldvalue = getmethod()
    #        else:
    #            try:
    #                oldvalue = getattr(subject, setvariable)
    #            except AttributeError:
    #                oldvalue = None
    #        if oldvalue not in [None, value]:
    #            raise pynt.ConsistencyException("New %s value of %s is %s, but was already set to %s" % (predicateuri, subjecturi, value, oldvalue))
    #    # TODO: check type!!
    #    if setmethod:
    #        setmethod(value)
    #    else:
    #        setattr(subject, setvariable, value)
    
    def GetRdfProperty(self, subject, predicateuri):
        """Given an RDFObject, check if there is exactly one property with the given predicate, 
        and return the value. If no property is found, return None.
        If multiple properties are found, raise a parsing exception."""
        logger = logging.getLogger("pynt.input")
        assert(isinstance(predicateuri, rdflib.URIRef))
        if isinstance(subject, pynt.xmlns.RDFObject):
            subjecturi = rdflib.URIRef(subject.getURIdentifier())
        elif isinstance(subject, rdflib.URIRef):
            subjecturi = subject
        else:
            subjecturi = rdflib.URIRef(subject)
        objects = list(self.graph.objects(subjecturi, predicateuri))
        if len(objects) > 1:
            logger.warning("Found %d %s properties of %s. Only one allowed." % (len(objects), predicateuri, subjecturi))
        # print "%s %s = %s" % (subjecturi, predicateuri, objects)
        if len(objects) == 0:
            return None
        return objects[0]
    
    def GetRdfLiteral(self, subject, predicateuri, type=None, required=True):
        """Given an RDFObject, check if there is exactly one property with the given predicate, 
        and return the value as native python object (string, int, float, etc.) or None 
        if no instances were found."""
        logger = logging.getLogger("pynt.input")
        value = self.GetRdfProperty(subject, predicateuri)
        # we're returning objects[0] as a python object.
        if value == None:
            if required:
                logger.error("Property %s of %s not found" % (predicateuri, subject))
            else:
                logger.debug("Property %s of %s not found" % (predicateuri, subject))
            return None
        elif isinstance(value, Literal):
            if type != None:
                try:
                    return type(value)
                except TypeError:
                    logger.warning("Can't covert property %s of %s to %s" % (repr(value), subject, type.__name__))
                    return None
            else:
                try:
                    value = value.toPython()
                except AttributeError:  # toPython() was introduced in rdflib 2.3.2
                    logger.warning("Please specify type (int, float, etc.) of RDF Literal %s when calling GetRdfLiteral()" % (predicateuri))
            return value
        else:
            # logger.warning("Found value %s of %s is a %s, not a Literal. Returning None" % (repr(value), subject, __builtins__.type(value)))
            return type(value)
    
    # TODO: this function is only referenced from RDFDeviceFetcher
    #       which is to be removed
    def getUniqueSubjectURI(self, subjectType, subjectURI=None):
        """Find a subject of the type subjectType.
        If the subjectURI is set, verify if it is present in the 
        retrieved RDF file and is of rdf:type subjectType. 
        If subjectURI is not set, find the unique subject of subjectType in the RDF file."""
        logger = logging.getLogger("pynt.input")
        assert(isinstance(subjectType, rdflib.URIRef))
        if subjectURI:
            # we're looking for a specific subject
            classes = self.GetTypes(rdflib.URIRef(subjectURI))
            if subjectType not in classes:
                raise UnkownURI("Subject %s is not of the correct type %s, but of type(s) %s" % (subjectURI, subjectType, classes))
            logger.debug("Found %s with correct type %s" % (subjectURI, subjectType))
        else:
            # we're looking for a unique subject of type subjectType.
            rdf = self.GetRDFLibNamespace(prefix="rdf")
            subjects = list(self.graph.subjects(rdf["type"], subjectType))
            if len(subjects) == 0:
                raise UnkownURI("No subject of type %s found in RDF file %s" % (subjectType, self.url))
            elif len(subjects) > 1:
                raise UnkownURI("Multiple subject of type %s found in RDF file %s. You need to specify one." % (subjectType, self.url))
            else:
                # set the uri and identifier
                logger.debug("Found unique subject %s with type %s" % (subjects[0], subjectType))
                subjectURI = subjects[0]
        return rdflib.URIRef(subjectURI)
    


class RDFSchemaFetcher(RDFFetcher):
    """General purpose RDF fetcher, that retrieves all network elements from 
       a given RDF file. This class implements two phases of the recursive
       network fetcher process:

       (1) Finding seealso and isdefinedby sources for all RDF classes in
           the dependency list below
       (3) Parsing all RDF objects in a source, following the dependency
           list as displayed below

       The dependency list for fetching RDF/NDL objects is as follows:

       * Ontologies/layers/adaptationproperties/labeltype/property
       * Locations
       * Networks
       * Devices
       * Interfaces
       * Switchmatrices
       * linkTo
       * connectedTo

       Rather than building a hierarchy where for example for every specific
       device, its interfaces and switchmatrices are fetched, this fetcher 
       will rather look up any object that is a specific RDF class. The 
       object-specific retrieve function will then make sure they are 
       connected to their parents or children where necessary.

       It returns no specific subject."""

    techaware = False        # if technology aware, the interfaces are subclasses

    #########################################################################
    #########################################################################
    #
    # Phase (1) of the three-phase recursive fetching process
    #
    # TODO: see the description below. Should we also support references
    # for the mentioned RDF object classes?
    def buildSourceQueue(self):
        """This function implements step (1) of the recursive fetching 
           process, where all seeAlso's and isDefinedBy's are located
           and added to the fetch queue in the correct order.

           The dependency order is as follows:
           * Layer references
           * Location references
           * Network domain references
           * Device references

           NOTE / TODO: right now this process does not implement looking
           up interface, switchmatrix and linkto/connectedto references,
           since those are less likely to be used in RDF."""

        logger = logging.getLogger("pynt.input")
        rdf    = self.GetRDFLibNamespace(prefix="rdf")
        rdfs   = self.GetRDFLibNamespace(prefix="rdfs")
        ndl    = self.GetRDFLibNamespace(prefix="ndl") 

        self.open() # We need the graph right now

        # Find all location descriptions (ndl:Location)
        logger.debug("Finding location references in source %s" % self.url)
        locations = list(self.graph.subjects(rdf["type"], ndl["Location"]))
        for locationuri in locations:
            self.QueueRDFSeeAlso(locationuri)

        # Find all network descriptions (ndl:NetworkDomain)
        logger.debug("Finding network references in source %s" % self.url)
        networks = list(self.graph.subjects(rdf["type"], ndl["NetworkDomain"]))
        for networkuri in networks:
            self.QueueRDFSeeAlso(networkuri)

        # Find all device descriptions (ndl:Device)
        logger.debug("Finding device references in source %s" % self.url)
        devices = list(self.graph.subjects(rdf["type"], ndl["Device"]))
        for deviceuri in devices:
            self.QueueRDFSeeAlso(deviceuri)

        # Find all layer descriptions (rdf:Description)
        # TODO: this should be changed in the RDF. Layer isDefinedBy 
        #       references are not accompanied by an rdf:type statement 
        #       telling they reference to layer definitions. So basically 
        #       we could be adding the source to any kind of RDF file.
        logger.debug("Prepending layer and remaining references in source %s" % self.url)
        layers = list(self.graph.subject_objects(rdfs["isDefinedBy"]))
        for (subjuri, objuri) in layers:
            # NOTE: we prepend them, because layers have to be fetched first
            self.queueSource(objuri, self.__class__, prepend=True)

        # FIXME: due to the above, that isDefinedBy and seeAlso references
        #        are almost always defined without an rdf:type, we can't be
        #        sure what they point to. It turned out that in the wdm 
        #        schema there are seeAlso's that do NOT refer to actual URLs
        #        but to URIs. :(
        #layers = list(self.graph.subject_objects(rdfs["seeAlso"]))
        #for (subjuri, objuri) in layers:
        #    # NOTE: we prepend them, because layers have to be fetched first
        #    self.queueSource(objuri, self.__class__, prepend=True)

    # End of phase (1) of the three-phase recursive fetching process
    #
    #########################################################################
    #########################################################################


    #########################################################################
    #########################################################################
    #
    # Phase (3) of the three-phase recursive fetching process
 
    #########################################################################
    #
    # Functions relevant to retrieving layers and everything related
    def retrieveNamespace(self, namespaceuri):
        """Create an namespace based on a URI."""
        logger = logging.getLogger("pynt.input")
        logger.debug("retrieving namespace %s" % namespaceuri)
        rdfs      = self.GetRDFLibNamespace(prefix="rdfs")
        vs        = self.GetRDFLibNamespace(prefix="vs")
        prefix    = self.GetRdfLiteral(namespaceuri, rdfs["label"], type=str, required=False)
        schemaurl = self.GetRdfLiteral(namespaceuri, rdfs["isDefinedBy"], type=str, required=False)
        humanurl  = self.GetRdfLiteral(namespaceuri, vs["userdocs"], type=str, required=False)
        # TODO: should GetCreate be removed here?
        namespace = pynt.xmlns.GetCreateNamespace(str(namespaceuri),prefix=prefix,schemaurl=schemaurl,humanurl=humanurl)

        # This is already done elsewhere
        #if schemaurl and (schemaurl != self.url):
        #    self.fetchSource(schemaurl)
        #return namespace

    def retrieveLayer(self, layeruri):
        """Create an layer based on a URI. Note that this works fine (and MUST work) if the Layer object already exists!"""
        logger = logging.getLogger("pynt.input")
        logger.debug("Retrieving layer %s" % layeruri)
        layer = self.retrieveAndSetObject(layeruri, pynt.layers.Layer)
        return layer

    def retrieveAdaptation(self, adaptationuri):
        logger = logging.getLogger("pynt.input")
        logger.debug("Retrieving adaptation %s" % adaptationuri)
        assert(isinstance(adaptationuri, rdflib.URIRef))
        rdfs = self.GetRDFLibNamespace(prefix="rdfs")
        layerns = self.GetRDFLibNamespace(prefix="layer")
        try:
            clientlayer = None
            layeruri = list(self.graph.objects(adaptationuri, rdfs["range"]))
            clientlayer = self.getLayerFromList(layeruri)
        except pynt.xmlns.UndefinedNamespaceException:
            pass
        if clientlayer == None:
            logger.warning("Ignoring adaptation %s, as the domain does not seem to be a valid client layer" % (adaptationuri))
            return
        try:
            serverlayer = None
            layeruri = list(self.graph.objects(adaptationuri, rdfs["domain"]))
            serverlayer = self.getLayerFromList(layeruri)
        except pynt.xmlns.UndefinedNamespaceException:
            pass
        if serverlayer == None:
            logger.warning("Ignoring adaptation %s, as the domain does not seem to be a valid server layer" % (adaptationuri))
            return
        clientlayercount = self.GetRdfLiteral(adaptationuri, layerns["clientCount"], type=int, required=False)
        serverlayercount = self.GetRdfLiteral(adaptationuri, layerns["serverCount"], type=int, required=False)
        (namespace, identifier) = pynt.xmlns.splitURI(adaptationuri)
        # TODO: should GetCreate be removed here?
        adaptation = pynt.layers.GetCreateAdaptationFunction(identifier, namespace, clientlayer, serverlayer, clientlayercount, serverlayercount)
        # TODO: this does not seem to set the name and description, while it should do that!
        # self.SetRDFProperties(adaptation)
        self.SetRDFNameDescription(adaptation)
        return adaptation

    def retrieveLabelType(self, labeltypeuri):
        """Retrieve a RDF resource of type layer:LabelType, and store it as a LabelSet"""
        logger = logging.getLogger("pynt.input");
        assert(isinstance(labeltypeuri, rdflib.URIRef))
        
        rdfs  = self.GetRDFLibNamespace(prefix="rdfs");
        xsd   = self.GetRDFLibNamespace(prefix="xsd");
        
        parentclasses = self.GetParentClasses(labeltypeuri)
        if xsd["integer"] in parentclasses:
            datatype = int
            interval = 1
        elif xsd["float"] in parentclasses:
            datatype = float
            logger.warning("Using interval 0.1 for float interval. Float interval are not specified in the schema yet.")
            interval = 0.1  # TODO: define in schema and import.
        elif xsd["string"] in parentclasses:
            datatype = str
            interval = 0
        elif rdfs["Literal"] in parentclasses:
            datatype = str
            interval = 0
        else:
            logger.error("Could not find data type of label type %s in %s. Expected integer, float or string" % (labeltypeuri, parentclasses))
            return None
        
        if datatype == str:
            logger.warning("Ignore LabelType %s. Labels in string or Literal format are not supported yet." % labeltypeuri)
            return None
        
        mininclusive = self.GetRdfLiteral(labeltypeuri, xsd["minInclusive"], type=datatype, required=False)
        maxinclusive = self.GetRdfLiteral(labeltypeuri, xsd["maxInclusive"], type=datatype, required=False)
        
        rangeset = pynt.rangeset.RangeSet(None, itemtype=datatype, interval=interval)
        # TODO: should GetCreate be removed here?
        (namespace, identifier) = pynt.xmlns.splitURI(labeltypeuri)
        labelset = pynt.layers.GetCreateLabelSet(identifier, namespace, rangeset=rangeset)
        # FIXME: This doesn't work, because retrieveAndSetObject can't handle more than two arguments.
        #labelset = self.retrieveAndSetObject(labeltypeuri, pynt.layers.LabelSet, rangeset=rangeset)
        if (mininclusive != None) and (maxinclusive != None):
            labelset.rangeset.add(mininclusive, maxinclusive)
            logger.debug("Set range of labelset %s to %s." % (labeltypeuri, rangeset))
        else:
            logger.warning("Could not find a valid (mininclusive-maxinclusive = %s-%s) range for labelset %s" % (mininclusive, maxinclusive, labeltypeuri))
        
        # TODO: remove, should be done by retrieveAndSetObject
        #self.SetRDFNameDescription(labelset)
        return labelset

    def retrieveProperty(self, propertyuri):
        logger = logging.getLogger("pynt.input");
        assert(isinstance(propertyuri, rdflib.URIRef))
        
        ndl     = self.GetRDFLibNamespace(prefix="ndl");
        rdfs    = self.GetRDFLibNamespace(prefix="rdfs");
        layerns = self.GetRDFLibNamespace(prefix="layer");
        capability = self.GetRDFLibNamespace(prefix="capability");
        xsd     = self.GetRDFLibNamespace(prefix="xsd");
        owl     = self.GetRDFLibNamespace(prefix="owl");
        
        # First check if the domain is a connection point.
        domainlist = self.GetParentClasses(list(self.graph.objects(propertyuri, rdfs["domain"])))
        #if ndl["ConnectionPoint"] not in domainlist:
        #    logger.warning("Ignore property %s. The domain is not a ConnectionPoint" % propertyuri)
        #    return None
        layer = self.getLayerFromList(domainlist)
        if layer == None:
            logger.warning("Ignore property %s. The associated layer is not found in it's rdf:domain" % propertyuri)
            return None
        types = self.GetTypes(propertyuri)
        if layerns["AdaptationProperty"] in types:
            # Commented this out, maybe this is a bit too verbose, because it's not like adaptation
            # functions are entirely ignored, this function just doesn't handle them.
            #logger.debug("Ignore Adaptation Propery %s" % propertyuri)
            return None
        
        parentproperties = self.GetParentProperties(propertyuri)
        labelprops = set([layerns["label"], layerns["ingressLabel"], layerns["egressLabel"], capability["internalLabel"]])
        if len(labelprops.intersection(set(parentproperties))) > 0: # is one of the labelproperties in parentproperties?
            prop = self.retrieveLabelProperty(propertyuri)
        else:
            prop = self.retrieveLayerProperty(propertyuri, layer)
        if prop:
            self.SetRDFNameDescription(prop)
            layer.addProperty(prop)
        return prop
    
    def retrieveLayerProperty(self, propertyuri, layer=None):
        """PropertyURI is a regular property of a certain layer."""
        logger = logging.getLogger("pynt.input");
        assert(isinstance(propertyuri, rdflib.URIRef))
        
        rdfs    = self.GetRDFLibNamespace(prefix="rdfs");
        layerns = self.GetRDFLibNamespace(prefix="layer");
        xsd     = self.GetRDFLibNamespace(prefix="xsd");
        owl     = self.GetRDFLibNamespace(prefix="owl");

        # Check if the property was already set for the layer. If so,
        # we are not too strict about certain things. In this way, we
        # can define certain layer properties in other places
        if layer and layer.hasProperty(propertyuri): hasprop = True
        else: hasprop = False
        
        # compatible is a switch that marks a particular label as a potential source
        # of incompatibilities. Two regular properties with disjunct values are ignored.
        # Two interfces with disjunct properties whose compatible switch is set, are 
        # unable to transport data between each other.
        parentproperties = self.GetParentProperties(propertyuri)
        incompatible = layerns["property"] in parentproperties
        
        # check if a property is required, and if one or more may be specified.
        # WARNING: This is not the proper way to use owl:minCardinality.
        # We assume owl:minCardinality is a property of a Property, but the 
        # correct way to list is to use an owl:Restriction with owl:onProperty and owl:minCardinality.
        # TODO: Support proper form.
        mincardinality = self.GetRdfLiteral(propertyuri, owl["minCardinality"], type=int, required=False)
        compulsory = not mincardinality  # mincardinality is not 0 or None
        maxcardinality = self.GetRdfLiteral(propertyuri, owl["maxCardinality"], type=int, required=False)
        
        rangelist = self.GetParentClasses(list(self.graph.objects(propertyuri, rdfs["range"])))
        if len(rangelist) > 1:
            logger.error("Regular layer property %s has multiple ranges: %s. Only using the first." % (propertyuri, rangelist))
        range = None
        if xsd["integer"] in rangelist:
            range = int
        elif xsd["float"] in rangelist:
            range = float
        elif xsd["string"] in rangelist:
            range = str
        elif xsd["boolean"] in rangelist:
            range = bool
        elif rdfs["Literal"] in rangelist:
            range = str
        elif hasprop:
            range = self.retrieveResourceClass(rangelist[0])
            if range == None:
                logger.error("Could not find any information on resource class %s. Set range of %s to any Resource." % (rangelist[0], propertyuri))
        
        # FIXME: This doesn't work, because retrieveAndSetObject can't handle more than two arguments.
        #return self.retrieveAndSetObject(propertyuri, pynt.layers.Property)

        # TODO: should GetCreate be removed here?
        (namespace, identifier) = pynt.xmlns.splitURI(propertyuri);
        prop = pynt.layers.GetCreateProperty(identifier, namespace, range, incompatible=incompatible, compulsory=compulsory)

        # Try finding optimal minimum and maximum range values
        mininclusive = self.GetRdfLiteral(propertyuri, xsd["minInclusive"], required=False)
        maxinclusive = self.GetRdfLiteral(propertyuri, xsd["maxInclusive"], required=False)
        if mininclusive != None or maxinclusive != None:
            prop.setOptimalRange(mininclusive, maxinclusive)
            self.logger.debug("Set optimal range of %s to %s and %s" % (propertyuri, mininclusive, maxinclusive))

        return prop
 
    
    def retrieveResourceClass(self, resourceclassuri):
        assert(isinstance(resourceclassuri, rdflib.URIRef))
        
        rdf     = self.GetRDFLibNamespace(prefix="rdf");
        
        # Create a ResourceClass object with resourceuri
        # TODO: should GetCreate be removed here?
        #(namespace, identifier) = pynt.xmlns.splitURI(resourceclassuri);
        #resourceclass = pynt.layers.GetCreateResourceClass(identifier, namespace)
        resourceclass = self.retrieveAndSetObject(resourceclassuri, pynt.layers.ResourceClass)
        
        # read all resources of type resourceuri
        resourceuris = list(self.graph.subjects(rdf["type"], resourceclassuri))
        # create a Resource object of those
        for resourceuri in resourceuris:
            resource = self.retrieveResource(resourceuri)
            if resource:
                # add list of found resources to the ResourceClass
                resourceclass.addKnownResource(resource)
        
        # TODO: remove, should be done by retrieveAndSetObject
        # self.SetRDFNameDescription(resourceclass)
        return resourceclass
    
    def retrieveResource(self, resourceuri):
        logger = logging.getLogger("pynt.input");
        assert(isinstance(resourceuri, rdflib.URIRef))
        
        # TODO: should GetCreate be removed here?
        #(namespace, identifier) = pynt.xmlns.splitURI(resourceuri);
        #resource = pynt.layers.GetCreateResource(identifier, namespace)
        resource = self.retrieveAndSetObject(resourceuri, pynt.layers.Resource)
        # TODO: remove, should be done by retrieveAndSetObject
        #self.SetRDFNameDescription(resource)
        return resource
    
    def retrieveLabelProperty(self, propertyuri):
        logger = logging.getLogger("pynt.input");
        assert(isinstance(propertyuri, rdflib.URIRef))
        
        rdfs    = self.GetRDFLibNamespace(prefix="rdfs");
        layerns = self.GetRDFLibNamespace(prefix="layer");
        capabilityns = self.GetRDFLibNamespace(prefix="capability");
        xsd     = self.GetRDFLibNamespace(prefix="xsd");
        owl     = self.GetRDFLibNamespace(prefix="owl");
        
        domainlist = list(self.graph.objects(propertyuri, rdfs["domain"]))
        layer = self.getLayerFromList(domainlist)
        if layer == None:
            logger.warning("Ignore label property %s. The associated layer is not found in it's rdf:domain" % propertyuri)
            return None
        
        # check if a property is required, and if one or more may be specified.
        # WARNING: This is not the proper way to use owl:minCardinality.
        # We assume owl:minCardinality is a property of a Property, but the 
        # correct way to list is to use an owl:Restriction with owl:onProperty and owl:minCardinality.
        # TODO: Support proper form.
        mincardinality = self.GetRdfLiteral(propertyuri, owl["minCardinality"], type=int, required=False)
        compulsory = bool(mincardinality)  # mincardinality is not 0 or None (but e.g. 1)
        maxcardinality = self.GetRdfLiteral(propertyuri, owl["maxCardinality"], type=int, required=False)
        
        rangelist = list(self.graph.objects(propertyuri, rdfs["range"]))
        if len(rangelist) > 1:
            logger.error("Label property %s has multiple ranges: %s. Only using the first." % (propertyuri, rangelist))
        
        try:
            # We assume that Labelset are always created before (label) properties.
            # This is not true if the labelset is defined in another schema, read later then this schema.
            (namespace, identifier) = pynt.xmlns.splitURI(rangelist[0])
            labelset = pynt.xmlns.GetRDFObject(identifier=identifier, namespace=namespace, klass=pynt.layers.LabelSet)
        except pynt.xmlns.UndefinedNamespaceException:
            logger.warning("Could not find any information on LayerSet %s. Ignore label property %s." % (rangelist[0], propertyuri))
            return None
        
        (namespace, identifier) = pynt.xmlns.splitURI(propertyuri);
        # TODO: should GetCreate be removed here?
        prop = pynt.layers.GetCreateProperty(identifier, namespace, labelset, incompatible=True, compulsory=compulsory)
        
        logger.debug("Associating property %s to layer %s." % (prop, layer))
        parentproperties = self.GetParentProperties(propertyuri)
        if layerns["ingressLabel"] in parentproperties:
            layer.setIngressLabelProperty(prop)
        elif layerns["egressLabel"] in parentproperties:
            layer.setEgressLabelProperty(prop)
        elif capabilityns["internalLabel"] in parentproperties:
            layer.setInternalLabelProperty(prop)
        elif layerns["label"] in parentproperties:  # MUST be latest: the ingresslabel, etc. are also regular label
            layer.setLabelProperty(prop)
        else:
            logger.warning("Property %s does not seem to be a label property" % (propertyuri))
        
        return prop
    # End of layer related functions
    #
    #########################################################################


    #########################################################################
    #
    # Functions relevant for retrieving locations
    def retrieveLoction(self, locationuri):
        """Create a location object based on a URI."""
        logger = logging.getLogger("pynt.input")
        logger.debug("Retrieving location %s" % locationuri)
        return self.retrieveAndSetObject(locationuri, pynt.elements.Location)
    # End of location related functions
    #
    #########################################################################
    

    #########################################################################
    #
    # Functions relevant for retrieving networks
    def retrieveNetwork(self, networkuri):
        """Create an domain object based on a URI."""
        logger = logging.getLogger("pynt.input")
        logger.debug("Retrieving network domain %s" % networkuri)
        ndl = self.GetRDFLibNamespace(prefix="ndl")

        domain = self.retrieveAndSetObject(networkuri, pynt.elements.Domain)

        # Check if this domain has any devices defined in this source
        # and add these devices.
        devices = list(self.graph.objects(networkuri, ndl["hasDevice"]))
        for deviceuri in devices:
            device = self.retrieveObject(deviceuri, pynt.elements.Device)
            domain.addDevice(device)
        

    # End of network related functions
    #
    #########################################################################

    
    #########################################################################
    # 
    # Functions relevant for retrieving devices
    def retrieveDevice(self, deviceuri):
        """Create an domain object based on a URI."""
        logger = logging.getLogger("pynt.input")
        logger.debug("Retrieving device %s" % deviceuri)

        ndl        = self.GetRDFLibNamespace(prefix="ndl")
        capability = self.GetRDFLibNamespace(prefix="capability")

        device = None
        device = self.retrieveAndSetObject(deviceuri, pynt.elements.Device)
        if device != None:
           self.SetRDFProperties(device, ignorepredicates=[ndl["hasInterface"], capability["hasSwitchMatrix"]])

        # Retrieve all interfaces that are defined here.
        interfaces = list(self.graph.objects(deviceuri, ndl["hasInterface"]))
        for interfaceuri in interfaces: 
            interface = self.retrieveObject(interfaceuri, pynt.elements.Interface)
            if interface:
                interface.setDevice(device)
        return device

    # End of device related functions
    #
    #########################################################################


    #########################################################################
    #
    # Functions relevant for retrieving interfaces
    def getInterfaceClass(self, layer):
        if self.techaware:
            logger = logging.getLogger("pynt.input")
            try:
                klass = None
                klass = pynt.technologies.GetInterfaceClassByLayer(layer)
            except AttributeError:
                pass
            if klass == None:
                logger.warning("No interface class found for layer %s" % (layer))
                klass = pynt.elements.Interface
            return klass
        else:
            return pynt.elements.Interface

    def getCreateInterface(self, interfaceuri):
        logger = logging.getLogger("pynt.input")
        try:
            layer = self.getLayer(interfaceuri)
            klass = self.getInterfaceClass(layer)
        except UnkownURI:
            logger.warning("Interface %s is not defined in %s. Skipping interface." % (interfaceuri, self.url))
            return None
        (namespace, identifier) = pynt.xmlns.splitURI(interfaceuri)
        try:
            # TODO: should GetCreate be removed here?
            interface = pynt.elements.GetCreateConnectionPoint(identifier, namespace, klass)
        except pynt.xmlns.DuplicateNamespaceException:
            try:
                interface = pynt.xmlns.GetRDFObject(identifier, namespace)
            except pynt.xmlns.UndefinedNamespaceException:
                interface = None
            logger.warning("Ignoring interface %s, as the URI already exists, but is a %s, instead of an %s object" % (interfaceuri, type(interface).__name__, klass.__name__))
            return None
        # If interface is not set, we are not interested in knowing that the 
        # layers do not match, because this function can also be called
        # for linkTo and connectedTo. In those sources it is not required
        # to specify a layer
        if interface.getLayer() not in [None, layer] and layer != None:
            logger.warning("Interface %s has already layer %s, but I found layer %s. Ignoring new value." % (interfaceuri, interface.getLayer(), layer))
        elif layer != None:
            interface.setLayer(layer)
        # We only want to log this error if the interface was new and didn't have a layer set yet
        elif interface.getLayer() == None and layer == None:
            logger.warning("Creating interface %s, despite that no layer information was found." % (interfaceuri))
        return interface
 
    def retrieveInterface(self, interfaceuri):
        logger = logging.getLogger("pynt.input")
        logger.debug("Processing interface %s" % interfaceuri)
        assert(isinstance(interfaceuri, rdflib.URIRef))
        
        interface = self.getCreateInterface(interfaceuri)
        if interface != None:
            # This used to call 'setInterfaceProperties', but that just calls SetRDFProperties,
            # so why not do it directly?
            #self.setInterfaceProperties(interface)
            self.SetRDFProperties(interface)

        return interface
    # End of interface related functions
    #
    #########################################################################


    #########################################################################
    #
    # Adaptation related functions
    def retrieveAdaptations(self, interface):
        logger = logging.getLogger("pynt.input")
        interfaceuri = rdflib.URIRef(interface.getURIdentifier())
        tuples = list(self.graph.predicate_objects(interfaceuri))
        logger.debug("Checking %d RDF predicates for adaptations for interface %s" % (len(tuples), interfaceuri))
        for (predicate, value) in tuples:
            logger.debug("Looking for pred %s, val %s" % (predicate, value))
            # here we add any unknown (technology specific) predicates
            # Check if the property type is defined for the subject by checking
            # if it is part of the layer
            try: 
                layer = interface.getLayer()
            except AttributeError:
                logger.warning("Interface %s is not layer aware, skipping predicate %s" % (interface.getURIdentifier(), predicate))
                continue
            if layer == None:
                logger.warning("No layer defined for interface %s" % interface.getURIdentifier());
                continue
            (namespace, identifier) = pynt.xmlns.splitURI(predicate)
            
            # First check if this is an adaptation function
            adaptation = pynt.layers.GetAdaptationFunction(identifier, namespace)
            if adaptation == None: # This is not an adaptation, let's ignore it
                continue
            
            logger.debug("Found adaptation %s -> %s -> %s" % (interface.getURIdentifier(), identifier, value))
            # Make sure subject is an interface
            if not isinstance(interface, pynt.elements.ConnectionPoint):
                logger.warning("Subject %s is not an interface, adaptations should only be configured for interfaces." % interface.getName())
                continue
            # Find the target interface
            (tnamespace, tidentifier) = pynt.xmlns.splitURI(value)
            try: 
                target = pynt.xmlns.GetRDFObject(identifier=tidentifier, namespace=tnamespace, klass=pynt.elements.Interface)
            except pynt.xmlns.UndefinedNamespaceException: 
                logger.warning("Target interface %s for adaptation %s not found." % (value, adaptation))
                continue
            
            # Add the target to the subject as a client interface and v.v. (taken care of by addClientInterface())
            try: 
                interface.addClientInterface(target, adaptation)
            # except AssertionError:
            #     logger.warning("Assertion error in addClientInterface for %s occurred." % interface.getName())
            # except Exception, (strerror): 
            except pynt.ConsistencyException, (strerror):
                logger.warning("Error adding adaptation: %s" % strerror)
    # End of adaptation related functions
    #
    #########################################################################


    #########################################################################
    # 
    # SwitchMatrix related functions
    def retrieveSwitchMatrix(self, switchmatrixuri):
        logger = logging.getLogger("pynt.input")
        logger.debug("Processing switchmatrix %s" % switchmatrixuri)
        assert(isinstance(switchmatrixuri, rdflib.URIRef))
        
        capability = self.GetRDFLibNamespace(prefix="capability")
        
        # TODO: should we remove GetCreate here?
        #(namespace, identifier) = pynt.xmlns.splitURI(switchmatrixuri)
        #switchmatrix = pynt.elements.GetCreateSwitchMatrix(identifier, namespace)
        switchmatrix = self.retrieveAndSetObject(switchmatrixuri, pynt.elements.SwitchMatrix)

        # TODO: set layer, hasSwitchingCapability, hasSwappingCapability
        switchuris = list(self.graph.objects(switchmatrixuri, capability["hasSwitchingCapability"]))
        if len(switchuris) > 1:
            logger.error("Switch matrix %s has two or more hasSwitchingCapability predicates. Only using first one." % switchmatrixuri)
        if len(switchuris) > 0:
            switchmatrix.setSwitchingCapability(True)
            layer = self.getLayerFromList(switchuris)
            if layer:
                switchmatrix.setLayer(layer)
        swapuris   = list(self.graph.objects(switchmatrixuri, capability["hasSwappingCapability"]))
        if len(swapuris) > 1:
            logger.error("Switch matrix %s has two or more hasSwappingCapability predicates. Only using first one." % switchmatrixuri)
        if len(swapuris) > 0:
            switchmatrix.setSwappingCapability(True)
            layer = self.getLayerFromList(swapuris)
            switchmatrix.setLayer(layer)
        if switchmatrix.getLayer() == None:
            logger.error("Switch matrix %s has no switching nor swapping capability; don't know it's layer" % switchmatrixuri)
        return switchmatrix
    # End of switchmatrix related functions
    #
    #########################################################################


    #########################################################################
    #
    # linkTo related functions
    def retrieveLinkTo(self, sourceifuri, destifuri):
        logger = logging.getLogger("pynt.input")
        logger.debug("Processing link %s -> %s" % (sourceifuri, destifuri))
        # Find the source interface, the destination interface and connect 
        # source to dest.
        sourceif = self.retrieveObject(sourceifuri, pynt.elements.Interface)
        destif   = self.retrieveObject(destifuri,   pynt.elements.Interface)

        try:
            sourceif.addLinkedInterface(destif)
        except pynt.ConsistencyException, (strerror):
            logger.warning("%s" % strerror)
    # End of linkto related functions
    #
    #########################################################################


    #########################################################################
    #
    # connectedTo related functions
    def retrieveConnectedTo(self, sourceifuri, destifuri):
        logger = logging.getLogger("pynt.input")
        logger.debug("Processing connection %s -> %s" % (sourceifuri, destifuri))
        # Find the source interface, the destination interface and connect 
        # source to dest.
        sourceif = self.retrieveObject(sourceifuri, pynt.elements.Interface)
        destif   = self.retrieveObject(destifuri,   pynt.elements.Interface)

        try:
            sourceif.addConnectedInterface(destif)
        except pynt.ConsistencyException, (strerror):
            logger.warning("%s" % strerror)
    # End of linkto related functions
    #
    #########################################################################


    #########################################################################
    #
    # Phase (3) of the three-phase recursive fetching process
    def retrieve(self):
        """This implements step (3) of the recursive fetching process."""

        logger     = logging.getLogger("pynt.input")
        ndl        = self.GetRDFLibNamespace(prefix="ndl")
        rdf        = self.GetRDFLibNamespace(prefix="rdf")
        owl        = self.GetRDFLibNamespace(prefix="owl")
        layerns    = self.GetRDFLibNamespace(prefix="layer")
        capability = self.GetRDFLibNamespace(prefix="capability")

        # Find all namespaces
        subjects = list(self.graph.subjects(rdf["type"], owl["Ontology"]))
        for subject in subjects:
            self.retrieveNamespace(subject)
            
        # Find all Layers
        subjects = list(self.graph.subjects(rdf["type"], layerns["Layer"]))
        for subject in subjects:
            self.retrieveLayer(subject)

        # Find all Layer Interfaces (i.e. subclasses of both Interface and a specific Layer)
        # subjects = list(self.graph.subjects(rdf["type"], layerns["InterfaceClass"]))
        # for subject in subjects:
        #     self.retrieveLayerInterface(subject)

        # Find all adaptations
        subjects = list(self.graph.subjects(rdf["type"], layerns["AdaptationProperty"]))
        for subject in subjects:
            self.retrieveAdaptation(subject)

        # Find all label types
        subjects = list(self.graph.subjects(rdf["type"], layerns["LabelType"]));
        for subject in subjects:
            self.retrieveLabelType(subject);

        # Find all properties, including label properties
        subjects = list(self.graph.subjects(rdf["type"], rdf["Property"]));
        for subject in subjects:
            self.retrieveProperty(subject)

        # Retrieve all locations
        logger.debug("Retrieving locations from source %s" % self.url)
        locations = list(self.graph.subjects(rdf["type"], ndl["Location"]))
        for locationuri in locations:
            x=self.retrieveLoction(locationuri)
            #print(x)

        # Retrieve all networks
        logger.debug("Retrieving networks from source %s" % self.url)
        domains = list(self.graph.subjects(rdf["type"], ndl["NetworkDomain"]))
        for domainuri in domains:
            self.retrieveNetwork(domainuri)

        # Retrieve all devices
        logger.debug("Retrieving devices from source %s" % self.url)
        devices = list(self.graph.subjects(rdf["type"], ndl["Device"]))
        #devices - JMA
        
        for deviceuri in devices:
            x1=self.retrieveDevice(deviceuri)
            #print(x1)
            #print(x1.getIdentifier())
            #print(x1.getLogicalInterfaces())
            
        # Retrieve all interfaces
        logger.debug("Retrieving interfaces from source %s" % self.url)
        interfaces = list(self.graph.subjects(rdf["type"], ndl["Interface"]))
        for interfaceuri in interfaces:
            self.retrieveInterface(interfaceuri)

        # Retrieve all adaptations
        # This has to be done -after- retrieving interfaces, because
        # you'd have the possibility that for the client- or serveradaptation
        # the layer isn't set yet.
        logger.debug("Retrieving adaptations from source %s" % self.url)
        interfaces = list(self.graph.subjects(rdf["type"], ndl["Interface"]))
        for interfaceuri in interfaces:
            # We only want the interface, so we do not use self.retrieveInterface().
            # self.retrieveObject or retrieveAndSetObject should not be used,
            # because we are not sure what interface class we will need
            interface = self.getCreateInterface(interfaceuri)
            # FIXME: is this the right way? I think adaptations only should be 
            # set for logical interfaces, right?
            self.retrieveAdaptations(interface)

        # Retrieve all switchmatrices and their interfaces
        logger.debug("Retrieving switchmatrices from source %s" % self.url)
        switchmatrices = list(self.graph.subject_objects(capability["hasSwitchMatrix"]))
        for (deviceuri, switchmatrixuri) in switchmatrices:
            switchmatrix = self.retrieveSwitchMatrix(switchmatrixuri)
            device = self.retrieveObject(deviceuri, pynt.elements.Device)
            device.addSwitchMatrix(switchmatrix)
            # If there are any interfaces in the switchmatrix, we will be 
            # finding them in this source as well
            matrixinterfaces = list(self.graph.objects(switchmatrixuri, ndl["hasInterface"]))
            for interfaceuri in matrixinterfaces:
                # We only want the interface, so we do not use self.retrieveInterface().
                # self.retrieveObject or retrieveAndSetObject should not be used,
                # because we are not sure what interface class we will need
                interface = self.getCreateInterface(interfaceuri)
                if interface:
                    try:
                        switchmatrix.addInterface(interface)
                    except pynt.ConsistencyException, (strerror):
                        logger.error(strerror)


        # Retrieve all linkTo's
        linktos = list(self.graph.subject_objects(ndl["linkTo"]))
        for (sourceifuri, destifuri) in linktos:
            self.retrieveLinkTo(sourceifuri, destifuri)

        # Retrieve all connectedTo's
        connectedtos = list(self.graph.subject_objects(ndl["connectedTo"]))
        for (sourceifuri, destifuri) in connectedtos:
            self.retrieveConnectedTo(sourceifuri, destifuri)
            #print("Source: "+sourceifuri)
            #print("Destination: "+destifuri)

        # Retrieve all capacity
        capacityList = list(self.graph.subject_objects(ndl["capacity"]))

        # Retrieve all latency
        latencyList = list(self.graph.subject_objects(ndl["latency"]))
        
        # Retrieve all videoServerWaitInterval
        videoServerWaitIntervalList = list(self.graph.subject_objects(ndl["videoServerWaitInterval"]))
        #print("videoServerWaitInterval Test")
        #print(videoServerWaitIntervalList)

        # Retrieve all videoServerPacketLength
        videoServerPacketLengthList = list(self.graph.subject_objects(ndl["videoServerPacketLength"]))
        #print("videoServerPacketLength Test")
        #print(videoServerPacketLengthList)

        # Retrieve all videoServerContentLength
        videoServerContentLengthList = list(self.graph.subject_objects(ndl["videoServerContentLength"]))
        #print("videoServerContentLength Test")
        #print(videoServerContentLengthList)

        # Retrieve all VideoServersConnections 
        hasVideoServerConnectionList = list(self.graph.subject_objects(ndl["hasVideoServer"]))
        #print("videoServerConnectionList Test")
        #print(hasVideoServerConnectionList)
        
        # Retrieve all VideoServers 
        isVideoServerList = list(self.graph.subject_objects(ndl["isVideoServer"]))
        #print("isVideoServerList Test")
        #print(isVideoServerList)

        # Retrieve all TVideoServers 
        #hasTVideoServerList = list(self.graph.subject_objects(ndl["hasTVideoServer"]))
        #print("hasTVideoServerList Test")
        #print(hasTVideoServerList)

        # Retrieve all numHasTVideoServer
        numHasTVideoServerList = list(self.graph.subject_objects(ndl["hasNLatency"]))

        maxServerElements=0
        for numHasTVideoServerListItem in numHasTVideoServerList:
            numberListItems=int(numHasTVideoServerListItem[1])
            #print(numberListItems)
            if(numberListItems>maxServerElements):
                maxServerElements=numberListItems

        for elementsCounter in range(maxServerElements):
            maxLatencyListTemp = list(self.graph.subject_objects(ndl["maxLatency"+str(elementsCounter)]))
            if(elementsCounter==0):
                maxLatencyList=maxLatencyListTemp
            else:
                maxLatencyList+=maxLatencyListTemp
                
        ###################################################################
        #End of Anything "Official"

        ######################
        #Sort out connections so that they can be transferred

        lengthCon=len(connectedtos)
        print("Number Connections: ")
        print(lengthCon)

        numdevices=len(devices)

        self.connectionsPresent = [[0 for col in range(numdevices)] for row in range(numdevices)]

        setval=0

        for (sourceifuri, destifuri) in connectedtos:
            src=0
            dest=0
            srcnum=0
            destnum=0
            srcfound=False
            destfound=False
            for deviceuri in devices:
                if((not srcfound) or (not destfound)):
                    if (str(deviceuri) in str(sourceifuri)):
                        srcfound=True
                        srcnum+=1
                        src=devices.index(deviceuri)
                        
                    if (str(deviceuri) in str(destifuri)):
                        destfound=True
                        destnum+=1
                        dest=devices.index(deviceuri)
                        
                    if(destfound and srcfound):
                        setval+=1
                        self.connectionsPresent[src][dest]+=1

            if((not srcfound) or (not destfound)):
                if(not srcfound):
                    print("Source not found")
                    print(sourceifuri)
                if(not destfound):
                    print("Dest not found")
                    print(destifuri)
            
        self.numdevices=numdevices
        for deviceuri in devices:
            print(deviceuri)
                        
        print(self.connectionsPresent)
        #print(setval)

        ##########################
        #Setup to move capacities
       
        lengthCap=len(capacityList)
        #print("Number capacities: ")
        #print(lengthCap)

        self.connectionsCapacity=[0 for col in range(lengthCap)]

        #print("Length: ")
        #print(len(self.connectionsCapacity))

        setpoint=0
        for capacityuri in capacityList:
            #print("Capacity Breakdown: ")
            #print(capacityuri)
            self.connectionsCapacity[setpoint]=float(capacityuri[1])
            #print(self.connectionsCapacity[setpoint])
            setpoint+=1
        print("Repeat")
        #############################

        ##########################
        #Setup to move latency
       
        lengthLatency=len(latencyList)
        print("Number Latency: ")
        print(lengthLatency)

        self.connectionsLatency=[0 for col in range(lengthLatency)]

        setpoint=0
        for latencyuri in latencyList:
            #print("Latency Breakdown: ")
            #print(latencyuri)
            self.connectionsLatency[setpoint]=float(latencyuri[1])
            #print(self.connectionsLatency[setpoint])
            setpoint+=1
        print("Repeat")
        #############################

        ##########################
        #Setup to move server/application connection info
       
        lengthWait=len(videoServerWaitIntervalList )
        lengthServPackLength=len(videoServerPacketLengthList)
        lengthVidSerContentLength=len(videoServerContentLengthList)
        lengthhasVidServConn=len(hasVideoServerConnectionList)
        lengthServers=len(isVideoServerList)
        
        print("Servers: "+str(lengthServers)+" Devices: "+str(numdevices))
        self.serverInfo=[[0 for col in range(3)] for row in range(numdevices)]

        for rowCounter in range(numdevices):
            for colCounter in range(3):
                self.serverInfo[rowCounter][colCounter]=0.0

        
        self.serverConnectionsInfo=[[0 for col in range(numdevices)] for row in range(numdevices)]

        #print("Servers: "+str(lengthServers))
        #print("Wait: "+str(lengthWait))
        #print("PLen"+str(lengthServPackLength))
        #print("CLen"+str(lengthVidSerContentLength))
        #print("HasConn "+str(lengthhasVidServConn))

        for counter in range(lengthServers):
            #print("VidServList")
            servertofind=isVideoServerList[counter][0]
            #print(servertofind)

            indexpos=devices.index(servertofind)
            #print("Pos: "+str(indexpos))

            #print(len(self.serverInfo))

            self.serverInfo[indexpos][0]=float(videoServerWaitIntervalList[counter][1])
            self.serverInfo[indexpos][1]=float(videoServerPacketLengthList[counter][1])
            self.serverInfo[indexpos][2]=float(videoServerContentLengthList[counter][1])

        print(self.serverInfo)

        for counter in range(lengthhasVidServConn):
            #print("Server Connections: ")
            #print(hasVideoServerConnectionList[counter])
            #print(hasVideoServerConnectionList[counter][0])
            #print(hasVideoServerConnectionList[counter][1])
            

            clientID=devices.index(hasVideoServerConnectionList[counter][0])
            serverID=devices.index(hasVideoServerConnectionList[counter][1])
            #print("clientID: "+str(clientID))
            #print("serverID: "+str(serverID))

            self.serverConnectionsInfo[serverID][clientID]+=1

        
        #############################
        # Handle maxLatency Transfer
        #print("MaxLatency: ")
        #print(maxLatencyList)

        lengthMaxLatency=len(maxLatencyList)
        print("maxLatencyList")
        for counter in range(lengthMaxLatency):
            print(maxLatencyList[counter])
              


        
    # End of phase (3) of the three-phase recursive fetching process
    #
    #########################################################################
    #########################################################################

    def getAllConnections(self):
        return self.connectionsPresent

    def getNumDevices(self):
        return self.numdevices

    def getDeviceConnections(self, deviceNumber):
        x=ArrayList()
        for item in self.connectionsPresent[deviceNumber]:
            x.add(item)
        return x

    def getConnectionsCapacity(self):
        x=ArrayList()
        for item in self.connectionsCapacity:
            x.add(item)
        return x

    def getConnectionsLatency(self):
        x=ArrayList()
        for item in self.connectionsLatency:
            x.add(item)
        return x

    def getVideoServerInformation(self, deviceNumber):
        x=ArrayList()
        for item in self.serverInfo[deviceNumber]:
            x.add(item)
        return x

    def getVideoClientInformation(self, deviceNumber):
        x=ArrayList()
        for item in self.serverConnectionsInfo[deviceNumber]:
            x.add(item)
        return x
