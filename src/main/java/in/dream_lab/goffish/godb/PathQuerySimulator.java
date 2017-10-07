package in.dream_lab.goffish.godb;




import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.Iterator;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.Analyzer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;







public class PathQuerySimulator{ 
        
        
        
        public static final Log LOG = LogFactory.getLog(PathQuerySimulator.class);
        
        String Arguments=null;
        //Required for lucene 
        static File vertexIndexDir;
        static Directory vertexDirectory;
        static Analyzer analyzer;
        static IndexReader indexReader;
        static IndexSearcher indexSearcher;
        static BooleanQuery query;
        static ScoreDoc[] hits;
        static boolean initDone = false ;
        static boolean queryMade = false;


        static Hueristics hueristics = new Hueristics(); 
        
        static ArrayList<Step> path;
        static int noOfSteps;
        static Double[] queryCostHolder;
        static double networkCoeff=195.41;
        static double joinCoeff=1.503;
        static double indexCoeff;
        /**
         * Representative class to keep tab of next vertex to be processed, different for path query
         */
        public class VertexMessageSteps{
                Long queryId;
                Long vertexId;
                String message;
                Integer stepsTraversed;
                Long previousSubgraphId;
                Integer previousPartitionId;
                Long startVertexId;
                Integer startStep;
                VertexMessageSteps(Long _queryId,Long _vertexId,String _message,Integer _stepsTraversed,Long _startVertexId,Integer _startStep,Long _previousSubgraphId, Integer _previousPartitionId){
                        this.queryId=_queryId;
                        this.vertexId = _vertexId;
                        this.message = _message;
                        this.stepsTraversed = _stepsTraversed;
                        this.previousSubgraphId = _previousSubgraphId;
                        this.startVertexId = _startVertexId;
                        this.startStep=_startStep;
                        this.previousPartitionId = _previousPartitionId;
                }
        }
        
        //this is used for storing output messages, output messages are partial messages that are sent to parent subgraph for recursive aggregation
        public class OutputMessageSteps{
                LongWritable targetSubgraphId;
                Text message;
                public OutputMessageSteps(Text _message, LongWritable _SubgraphId) {
                        // TODO Auto-generated constructor stub
                        this.message = _message;
                        this.targetSubgraphId = _SubgraphId;
                }
        }
        
        

                
        // Recursive Output COllection data structures  
        // TODO: Populate the hash appropriately
        long time;
        class Pair{
                Long endVertex;
        Long prevSubgraphId;
        Integer prevPartitionId;    
        public Pair(Long _endVertex,Long _prevSubgraphId, Integer _prevPartitionId) {
                this.endVertex=_endVertex;
            this.prevSubgraphId = _prevSubgraphId;
            this.prevPartitionId = _prevPartitionId;
        }
    }
                
                class RecursivePathMaintained{
                        Long startVertex;
                        Integer startStep;
                        String path;
                        int direction=0;
                        
                        public int hashCode(){
                                return (int)(startStep+direction+startVertex + path.hashCode());
                        }
                        
                        public boolean equals(Object obj){
                                RecursivePathMaintained other=(RecursivePathMaintained)obj;
                                return (this.direction==other.direction && this.startStep.intValue()==other.startStep.intValue() && this.startVertex.longValue()==other.startVertex.longValue() && this.path.equals(other.path));
                        }
                        
                        
                        public RecursivePathMaintained(Long _startVertex,Integer _startStep, String _path, int _direction){
                                this.startVertex=_startVertex;
                                this.startStep=_startStep;
                                this.path=_path;
                                this.direction = _direction;
                        }
                }
                
                
        
                /**
                 * This is custom key used for storing partial paths.(Could be merged with OutputPathKey as only interpretation of keys are different)
                 * 
                 */
                 class RecursivePathKey {
                 long queryID;
                 int step;
                 boolean direction;
                 long endVertex;
                
                 public int hashCode() {
                     return (int) (queryID + step + endVertex);          
                 }
                
                 public boolean equals(Object obj) {
                        RecursivePathKey other=(RecursivePathKey)obj;
                    return (this.queryID==other.queryID && this.step==other.step && this.direction==other.direction && this.endVertex==other.endVertex);
                 }
                 
                public RecursivePathKey(long _queryID,int _step,boolean _direction,long _endVertex){
                        this.queryID=_queryID;
                        this.step=_step;
                        this.direction=_direction;
                        this.endVertex=_endVertex;                      
                }
            }
                 
                /**
                 * This is custom key used for storing information required to perform recursive merging of result. 
                 * 
                 *
                 */
                class OutputPathKey{
                        long queryID;
                        int step;
                        boolean direction;
                        long startVertex;
                        
                        public int hashCode() {
                                return (int) (queryID + step + startVertex);
                        }
                        
                        public boolean equals(Object obj){
                                OutputPathKey other=(OutputPathKey)obj;
                                return (this.queryID==other.queryID && this.step==other.step && this.direction==other.direction && this.startVertex==other.startVertex);
                        }
                        
                        public OutputPathKey(long _queryID,int _step,boolean _direction,long _startVertex){
                                this.queryID=_queryID;
                                this.step=_step;
                                this.direction=_direction;
                                this.startVertex=_startVertex;
                                
                        }
                        
                }
        
        
        
                
        /**
         * Initialize the class variables
         * This method is called in first superstep, it parses the query passed.
         * It also reads the Graph statistics(Called as Heuristics) from disk
         */
        private static void init(String arguments){
                
                
          
        
                
             path = new ArrayList<Step>();
                Step.Type previousStepType = Step.Type.EDGE;
                for(String _string : arguments.split(Pattern.quote("//"))[0].split(Pattern.quote("@")) ){
                        if(_string.contains("?")){
                                if(previousStepType == Step.Type.EDGE)
                                        path.add(new Step(Step.Type.VERTEX,null, null, null));
                                previousStepType = Step.Type.EDGE;
                                String[] _contents = _string.split(Pattern.quote("?")); 
                                String p = null ;
                                Object v = null ;
                                Step.Direction d = (_contents[0].equals("out") ) ? Step.Direction.OUT : Step.Direction.IN;
                                if ( _contents.length > 1 )     {
                                        p = _contents[1].split(Pattern.quote(":"))[0];
                                        String typeAndValue = _contents[1].split(Pattern.quote(":"))[1];
                                        String type = typeAndValue.substring(0, typeAndValue.indexOf("["));
                                        if(type.equals("float")) {
                                                v = Float.valueOf(typeAndValue.substring(typeAndValue.indexOf("[") + 1, typeAndValue.indexOf("]")) );
                                        }
                                        else if(type.equals("double")) { 
                                                v = Double.valueOf(typeAndValue.substring(typeAndValue.indexOf("[") + 1, typeAndValue.indexOf("]")) );

                                        }
                                        else if(type.equals("int")) { 
                                                v = Integer.valueOf(typeAndValue.substring(typeAndValue.indexOf("[") + 1, typeAndValue.indexOf("]")) );

                                        }
                                        else { 
                                                v = String.valueOf(typeAndValue.substring(typeAndValue.indexOf("[") + 1, typeAndValue.indexOf("]")) );

                                        }
                                }
                                path.add(new Step(Step.Type.EDGE, d, p, v));
                        }
                        else{
                                previousStepType = Step.Type.VERTEX;
                                String p = _string.split(Pattern.quote(":"))[0];
                                String typeAndValue = _string.split(Pattern.quote(":"))[1];
                                Object v = null;
                                String type = typeAndValue.substring(0, typeAndValue.indexOf("["));
                                if(type.equals("float")) {
                                        v = Float.valueOf(typeAndValue.substring(typeAndValue.indexOf("[") + 1, typeAndValue.indexOf("]")) );
                                }
                                else if(type.equals("double")) { 
                                        v = Double.valueOf(typeAndValue.substring(typeAndValue.indexOf("[") + 1, typeAndValue.indexOf("]")) );

                                }
                                else if(type.equals("int")) { 
                                        v = Integer.valueOf(typeAndValue.substring(typeAndValue.indexOf("[") + 1, typeAndValue.indexOf("]")) );

                                }
                                else { 
                                        v = String.valueOf(typeAndValue.substring(typeAndValue.indexOf("[") + 1, typeAndValue.indexOf("]")) );

                                }

                                path.add(new Step(Step.Type.VERTEX,null, p, v));
                        }
                        

                }
                if(previousStepType == Step.Type.EDGE){
                        path.add(new Step(Step.Type.VERTEX,null, null, null));
                }
                noOfSteps = path.size();
                queryCostHolder = new Double[noOfSteps];
                for (int i = 0; i < queryCostHolder.length; i++) {
                        queryCostHolder[i] = new Double(0);
                        
                }
//                forwardLocalVertexList = new LinkedList<VertexMessageSteps>();
//                revLocalVertexList = new LinkedList<VertexMessageSteps>();
//              inVerticesMap = new HashMap<Long, HashMap<String,LinkedList<Long>>>();
//              remoteSubgraphMap = new HashMap<Long, Long>();
//              hueristics=HueristicsLoad.getInstance();//loading this at a different place

                
        }

        
        
        
        
        

    
        
        


        

        
        
        
        
        
        
        /**
         * 
         */
 
        
        public static void computeNWFixed() {
          

          
          // RUNTIME FUNCTIONALITITES 
          {
                 
                         
                          // COMPUTE HUERISTIC BASED QUERY COST
                          {
                                  // TODO: implementation for calc cost from middle of query ( for each position calc cost forward and backward cost and add them)
                                  
                                  for (int pos = 0;pos < path.size() ; pos+=2 ){
                                          Double joinCost = new Double(0);
                                          //forward cost
                                          {       
                                                  Double totalCost = new Double(0);
                                                  Double prevScanCost = hueristics.numVertices;
                                                  Double resultSetNumber = hueristics.numVertices;
                                                  ListIterator<Step> It = path.listIterator(pos);
                                                  //Iterator<Step> It = path.iterator();
                                                  Step currentStep = It.next();
                                                  
                                                  while(It.hasNext()){
                                                          //cost calc
                                                          // TODO: make cost not count in probability when no predicate on edge/vertex
                                                          {
                                                                  Double probability = null;
                                                                  
                                                                  if ( currentStep.property == null )
                                                                          probability = new Double(1);
                                                                  else {
                                                                          if ( hueristics.vertexPredicateMap.get(currentStep.property).containsKey(currentStep.value.toString()) ){
                                                                                  probability = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).probability;
                                                                                  //System.out.println("Vertex Probability:" + probability);
                                                                          }       
                                                                          else {
                                                                                  totalCost = new Double(-1);
                                                                                  break;
                                                                          }
                                                                  }
                                                                  resultSetNumber *= probability;
                                                                  Double avgDeg = new Double(0);
                                                                  Double avgRemoteDeg = new Double(0);
                                                                  Step nextStep = It.next();
                                                                  if(nextStep.direction == Step.Direction.OUT){
                                                                          if ( currentStep.property == null) {
                                                                                  avgDeg = hueristics.numEdges/hueristics.numVertices;
                                                                                  avgRemoteDeg = hueristics.numRemoteVertices/(hueristics.numVertices+hueristics.numRemoteVertices) * avgDeg;
                                                                                  //System.out.println("AVGDEG:" +avgDeg + "REMOTEAVGDEG:" + avgRemoteDeg);
                                                                          }       
                                                                          else { 
                                                                                  avgDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgOutDegree; 
                                                                                  avgRemoteDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgRemoteOutDegree;
//                                                                                  System.out.println("AVGDEG:" +avgDeg + "REMOTEAVGDEG:" + avgRemoteDeg);
                                                                          }       
                                                                  }else if(nextStep.direction == Step.Direction.IN){
                                                                          if ( currentStep.property == null) {
                                                                                  avgDeg = hueristics.numEdges/hueristics.numVertices;
                                                                                  avgRemoteDeg = hueristics.numRemoteVertices/(hueristics.numVertices+hueristics.numRemoteVertices) * avgDeg;
                                                                                  //System.out.println("AVGDEG:" +avgDeg + "REMOTEAVGDEG:" + avgRemoteDeg);
                                                                          }       
                                                                          else { 
                                                                                  avgDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgInDegree;
                                                                                  avgRemoteDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgRemoteInDegree;
                                                                                  //System.out.println("AVGDEG:" +avgDeg + "REMOTEAVGDEG:" + avgRemoteDeg);
                                                                          }               
                                                                  }
                                                                  resultSetNumber *= (avgDeg+avgRemoteDeg); 
                                                                  Double eScanCost = prevScanCost * probability * avgDeg;
                                                                  Double networkCost = new Double(0);
                                                                  Double vScanCost = new Double(0);
                                                                  if(nextStep.property == null){
                                                                          vScanCost = eScanCost;
                                                                          networkCost = networkCoeff * prevScanCost * probability * avgRemoteDeg;
                                                                          System.out
                                                                              .println("Network:"+ prevScanCost + "," + probability + "," + avgRemoteDeg);
                                                                  }
                                                                  else {
                                                                          //output(partition.getId(), subgraph.getId(),nextStep.property);
                                                                          //output(partition.getId(), subgraph.getId(),nextStep.value.toString());
                                                                          //output(partition.getId(), subgraph.getId(),String.valueOf(hueristics.edgePredicateMap.size()));
                                                                          //output(partition.getId(), subgraph.getId(),String.valueOf(pos));
                                                                          //output(partition.getId(), subgraph.getId(),hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability.toString());
                                                                          //System.out.println(nextStep.property+":"+nextStep.value);
                                                                          if ( hueristics.edgePredicateMap.get(nextStep.property).containsKey(nextStep.value.toString()) ) {
                                                                                  vScanCost = eScanCost * hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                  networkCost = networkCoeff * prevScanCost * probability * avgRemoteDeg * hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                  resultSetNumber *= hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                  //System.out.println("Edge:" + hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability);
                                                                          }
                                                                          else {
                                                                                  totalCost = new Double(-1);
                                                                                  break;
                                                                          }
                                                                  }
                                                                  totalCost += (eScanCost+vScanCost+networkCost);
                                                                  prevScanCost = vScanCost;
                                                                  currentStep = It.next();
                                                          }       
                                                                                          
                                                  }
                                                  joinCost += resultSetNumber;
                                                  if(pos==0 || pos == (path.size()-1)){
                                                    joinCost=0.0;
                                                  }
                                                  queryCostHolder[pos] = totalCost;
                                                  
//                                                System.out.println(pos+":"+"for:"+String.valueOf(totalCost));
                                          }
                                          //reverse cost
                                          {
                                                  Double totalCost = new Double(0);
                                                  Double prevScanCost = hueristics.numVertices;
                                                  Double resultSetNumber = hueristics.numVertices;

                                                  ListIterator<Step> revIt = path.listIterator(pos+1);
                                                  Step currentStep = revIt.previous();
                                                  while(revIt.hasPrevious()){
                                                          // TODO: make cost not count in probability when no predicate on edge/vertex
                                                          {
                                                                  Double probability = null;
                                                                  if ( currentStep.property == null )
                                                                          probability = new Double(1);
                                                                  else {
                                                                          if ( hueristics.vertexPredicateMap.get(currentStep.property).containsKey(currentStep.value.toString()) )
                                                                                  probability = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).probability;
                                                                          else {
                                                                                  totalCost = new Double(-1);
                                                                                  break;
                                                                          }
                                                                  }
                                                                  resultSetNumber *= probability;
                                                                  Double avgDeg = new Double(0);
                                                                  Double avgRemoteDeg = new Double(0);
                                                                  Step nextStep = revIt.previous();
                                                                  if(nextStep.direction == Step.Direction.OUT){
                                                                          if ( currentStep.property == null) {
                                                                                  avgDeg = hueristics.numEdges/hueristics.numVertices;
                                                                                  avgRemoteDeg = hueristics.numRemoteVertices/(hueristics.numVertices+hueristics.numRemoteVertices) * avgDeg;
                                                                          }
                                                                          else {
                                                                                  avgDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgInDegree; 
                                                                                  avgRemoteDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgRemoteInDegree;
                                                                          }       
                                                                  }else if(nextStep.direction == Step.Direction.IN){
                                                                          if ( currentStep.property == null) {
                                                                                  avgDeg = hueristics.numEdges/hueristics.numVertices;
                                                                                  avgRemoteDeg = hueristics.numRemoteVertices/(hueristics.numVertices+hueristics.numRemoteVertices) * avgDeg;
                                                                          }
                                                                          else { 
                                                                                  avgDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgOutDegree;
                                                                                  avgRemoteDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgRemoteOutDegree;
                                                                          }       
                                                                  }
                                                                  resultSetNumber *= (avgDeg+avgRemoteDeg);
                                                                  Double eScanCost = prevScanCost * probability * avgDeg;
                                                                  Double vScanCost = new Double(0);
                                                                  Double networkCost = new Double(0);
                                                                  if(nextStep.property == null){
                                                                          vScanCost = eScanCost;
                                                                          networkCost = networkCoeff * prevScanCost * probability * avgRemoteDeg;
                                                                  }
                                                                  else {
                                                                          if ( hueristics.edgePredicateMap.get(nextStep.property).containsKey(nextStep.value.toString()) ) {
                                                                                  vScanCost = eScanCost * hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                  networkCost = networkCoeff * prevScanCost * probability * avgRemoteDeg * hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                  resultSetNumber *= hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                          }
                                                                          else {
                                                                                  totalCost = new Double(-1);
                                                                                  break;
                                                                          }
                                                                  }
                                                                  totalCost += (eScanCost+vScanCost +networkCost);
                                                                  prevScanCost = vScanCost;
                                                                  currentStep = revIt.previous();
                                                          }
                                                  }
                                                  joinCost *= joinCoeff*resultSetNumber;
                                                  if ( queryCostHolder[pos] != -1 && totalCost != -1) {
                                                          queryCostHolder[pos] += totalCost;
                                                          if (pos!=0 && pos!= path.size()-1)
                                                                  queryCostHolder[pos] += joinCost;
                                                  }
                                                  else
                                                          queryCostHolder[pos] = new Double(-1);
                                                  
                                          }
                                          /* add that extra cost of initial scan*/
                                          //TODO: Add 1 when indexed
                                          initDone=true;//done to make it simulate like query is using indexes
                                          if ( queryCostHolder[pos] != -1 )
                                          {
                                                  if(!initDone)
                                                          queryCostHolder[pos] += hueristics.numVertices;
                                                  else
                                                          queryCostHolder[pos] +=hueristics.numVertices*indexCoeff;
                                                          
                                          }
//                                        System.out.println(pos+":Total:"+String.valueOf(queryCostHolder[pos]));
                                  }
                                   
                          }
                          
                          
                          
                          
                          // LOAD START VERTICES
                          {
                                 
                                  int startPos=0;
                                  Double minCost = queryCostHolder[startPos];
                                  boolean queryPossible = true;
                                  String AllCostsStrings="";
                                  for (int i = 0; i < queryCostHolder.length ; i+=2) {//changed from i++ to i+=2
                                          if(AllCostsStrings.equals("")){
                                            AllCostsStrings=queryCostHolder[i].toString();
                                          }
                                          else{
                                            AllCostsStrings+="," + queryCostHolder[i];
                                          }
                                          
                                          if ( queryCostHolder[i]!=0 && queryCostHolder[i]!=-1 && queryCostHolder[i] < minCost ){
                                                  minCost=queryCostHolder[i];
                                                  startPos = i;
                                          }
                                          if( queryCostHolder[i]==-1 )
                                                  queryPossible = false;
                                  }
                                  
                                  String currentProperty = null;
                                  Object currentValue = null;
//                                startPos=0;//used for debugging
                                  currentProperty = path.get(startPos).property; 
                                  currentValue = path.get(startPos).value;
                                  
                                  // TODO: check if the property is indexed** uncomment this if using indexes
                                  long QueryId=getQueryId();
                                 
                                          
                                System.out.println("Starting Position:" + startPos +"  Query min Cost:" + minCost + "   Path Size:" + path.size());
                                System.out.println("AllCosts:" + AllCostsStrings);
//                                System.out.println("*******Querying done********:"+hits.length);
                                  
                                         
                                  
          
                                  // TODO : else iteratively check for satisfying vertices
//                                if ( queryPossible == true )
//                                for(IVertex<MapWritable, MapWritable, LongWritable, LongWritable> vertex: getSubgraph().getLocalVertices()) {
//                                        if ( vertex.isRemote() ) continue;
//                                        
//                                        if ( compareValuesUtil(vertex.getValue().get(new Text(currentProperty)).toString(), currentValue.toString()) ) {
//                                                String _message = "V:"+String.valueOf(vertex.getVertexId().get());
//                                                System.out.println("Vertex id:" + vertex.getVertexId().get() + "Property:"+currentProperty +" Value:" + vertex.getValue().get(new Text(currentProperty)).toString());
//                                                if ( startPos == 0)
//                                                        forwardLocalVertexList.add( new VertexMessageSteps(QueryId,vertex.getVertexId().get(),_message, startPos, vertex.getVertexId().get(),startPos, getSubgraph().getSubgraphId().get(), 0) );
//                                                else
//                                                if( startPos == (path.size()-1))
//                                                        revLocalVertexList.add( new VertexMessageSteps(QueryId,vertex.getVertexId().get(),_message, startPos , vertex.getVertexId().get(),startPos, getSubgraph().getSubgraphId().get(), 0) );
//                                                else{
//                                                        forwardLocalVertexList.add( new VertexMessageSteps(QueryId,vertex.getVertexId().get(),_message, startPos, vertex.getVertexId().get(),startPos, getSubgraph().getSubgraphId().get(), 0) );
//                                                        revLocalVertexList.add( new VertexMessageSteps(QueryId,vertex.getVertexId().get(),_message, startPos , vertex.getVertexId().get(),startPos, getSubgraph().getSubgraphId().get(), 0) );
//                                                }
//                                                //output(partition.getId(), subgraph.getId(), subgraphProperties.getValue(currentProperty).toString());
//                                        }
//                                }
                                  
                                  
                                  
                          }
                          
                  
                  
                  

          }
          
          
         
  }
        
        
        
        //In this network coefficient is removed from network cost to get the constant of network coefficient
        public static void computeCoeff() {
          

          
          // RUNTIME FUNCTIONALITITES 
          {
                 
                         
                          // COMPUTE HUERISTIC BASED QUERY COST
                          {
                                  // TODO: implementation for calc cost from middle of query ( for each position calc cost forward and backward cost and add them)
                                  
                                  for (int pos = 0;pos < path.size() ; pos+=2 ){
                                          Double joinCost = new Double(0);
                                          System.out.println("---");
                                          System.out.println("POSITION:" + pos);
                                          //forward cost
                                          {       
                                                  Double totalCost = new Double(0);
                                                  Double prevScanCost = hueristics.numVertices;
                                                  Double resultSetNumber = hueristics.numVertices;
                                                  ListIterator<Step> It = path.listIterator(pos);
                                                  //Iterator<Step> It = path.iterator();
                                                  Step currentStep = It.next();
                                                  
                                                  while(It.hasNext()){
                                                          //cost calc
                                                          // TODO: make cost not count in probability when no predicate on edge/vertex
                                                          {
                                                                  Double probability = null;
                                                                  
                                                                  if ( currentStep.property == null )
                                                                          probability = new Double(1);
                                                                  else {
                                                                          if ( hueristics.vertexPredicateMap.get(currentStep.property).containsKey(currentStep.value.toString()) ){
                                                                                  probability = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).probability;
                                                                                  //System.out.println("Vertex Probability:" + probability);
                                                                          }       
                                                                          else {
                                                                                  totalCost = new Double(-1);
                                                                                  break;
                                                                          }
                                                                  }
                                                                  resultSetNumber *= probability;
                                                                  Double avgDeg = new Double(0);
                                                                  Double avgRemoteDeg = new Double(0);
                                                                  Step nextStep = It.next();
                                                                  if(nextStep.direction == Step.Direction.OUT){
                                                                          if ( currentStep.property == null) {
                                                                                  avgDeg = hueristics.numEdges/hueristics.numVertices;
                                                                                  avgRemoteDeg = hueristics.numRemoteVertices/(hueristics.numVertices+hueristics.numRemoteVertices) * avgDeg;
                                                                                  //System.out.println("AVGDEG:" +avgDeg + "REMOTEAVGDEG:" + avgRemoteDeg);
                                                                          }       
                                                                          else { 
                                                                                  avgDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgOutDegree; 
                                                                                  avgRemoteDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgRemoteOutDegree;
//                                                                                  System.out.println("AVGDEG:" +avgDeg + "REMOTEAVGDEG:" + avgRemoteDeg);
                                                                          }       
                                                                  }else if(nextStep.direction == Step.Direction.IN){
                                                                          if ( currentStep.property == null) {
                                                                                  avgDeg = hueristics.numEdges/hueristics.numVertices;
                                                                                  avgRemoteDeg = hueristics.numRemoteVertices/(hueristics.numVertices+hueristics.numRemoteVertices) * avgDeg;
                                                                                  //System.out.println("AVGDEG:" +avgDeg + "REMOTEAVGDEG:" + avgRemoteDeg);
                                                                          }       
                                                                          else { 
                                                                                  avgDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgInDegree;
                                                                                  avgRemoteDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgRemoteInDegree;
                                                                                  //System.out.println("AVGDEG:" +avgDeg + "REMOTEAVGDEG:" + avgRemoteDeg);
                                                                          }               
                                                                  }
                                                                  resultSetNumber *= (avgDeg+avgRemoteDeg); 
                                                                  Double eScanCost = prevScanCost * probability * avgDeg;
                                                                  Double networkCost = new Double(0);
                                                                  Double vScanCost = new Double(0);
                                                                  if(nextStep.property == null){
                                                                          vScanCost = eScanCost;
                                                                          networkCost = prevScanCost * probability * avgRemoteDeg;
//                                                                          System.out.println("networkCost:" + networkCost + ",avgRemoteDeg:" + avgRemoteDeg);
                                                                  }
                                                                  else {
                                                                          //output(partition.getId(), subgraph.getId(),nextStep.property);
                                                                          //output(partition.getId(), subgraph.getId(),nextStep.value.toString());
                                                                          //output(partition.getId(), subgraph.getId(),String.valueOf(hueristics.edgePredicateMap.size()));
                                                                          //output(partition.getId(), subgraph.getId(),String.valueOf(pos));
                                                                          //output(partition.getId(), subgraph.getId(),hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability.toString());
                                                                          //System.out.println(nextStep.property+":"+nextStep.value);
                                                                          if ( hueristics.edgePredicateMap.get(nextStep.property).containsKey(nextStep.value.toString()) ) {
                                                                                  vScanCost = eScanCost * hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                  networkCost =  prevScanCost * probability * avgRemoteDeg * hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                  resultSetNumber *= hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                  //System.out.println("Edge:" + hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability);
                                                                          }
                                                                          else {
                                                                                  totalCost = new Double(-1);
                                                                                  break;
                                                                          }
                                                                  }
                                                                  
                                                                  System.out.println("ForCoeff:" + eScanCost.toString() +"," + vScanCost.toString() + "," + networkCost.toString());
                                                                  totalCost += (eScanCost+vScanCost+networkCost);
                                                                  prevScanCost = vScanCost;
                                                                  currentStep = It.next();
                                                          }       
                                                                                          
                                                  }
                                                  joinCost += resultSetNumber;
                                                  if(pos==0 || pos == (path.size()-1)){
                                                    joinCost=0.0;
                                                  }
                                                  System.out.println("ForJoinCost:" + joinCost);
                                                  
                                                  queryCostHolder[pos] = totalCost;
                                                  
//                                                System.out.println(pos+":"+"for:"+String.valueOf(totalCost));
                                          }
                                          //reverse cost
                                          {
                                                  Double totalCost = new Double(0);
                                                  Double prevScanCost = hueristics.numVertices;
                                                  Double resultSetNumber = hueristics.numVertices;

                                                  ListIterator<Step> revIt = path.listIterator(pos+1);
                                                  Step currentStep = revIt.previous();
                                                  while(revIt.hasPrevious()){
                                                          // TODO: make cost not count in probability when no predicate on edge/vertex
                                                          {
                                                                  Double probability = null;
                                                                  if ( currentStep.property == null )
                                                                          probability = new Double(1);
                                                                  else {
                                                                          if ( hueristics.vertexPredicateMap.get(currentStep.property).containsKey(currentStep.value.toString()) )
                                                                                  probability = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).probability;
                                                                          else {
                                                                                  totalCost = new Double(-1);
                                                                                  break;
                                                                          }
                                                                  }
                                                                  resultSetNumber *= probability;
                                                                  Double avgDeg = new Double(0);
                                                                  Double avgRemoteDeg = new Double(0);
                                                                  Step nextStep = revIt.previous();
                                                                  if(nextStep.direction == Step.Direction.OUT){
                                                                          if ( currentStep.property == null) {
                                                                                  avgDeg = hueristics.numEdges/hueristics.numVertices;
                                                                                  avgRemoteDeg = hueristics.numRemoteVertices/(hueristics.numVertices+hueristics.numRemoteVertices) * avgDeg;
                                                                          }
                                                                          else {
                                                                                  avgDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgInDegree; 
                                                                                  avgRemoteDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgRemoteInDegree;
                                                                          }       
                                                                  }else if(nextStep.direction == Step.Direction.IN){
                                                                          if ( currentStep.property == null) {
                                                                                  avgDeg = hueristics.numEdges/hueristics.numVertices;
                                                                                  avgRemoteDeg = hueristics.numRemoteVertices/(hueristics.numVertices+hueristics.numRemoteVertices) * avgDeg;
                                                                          }
                                                                          else { 
                                                                                  avgDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgOutDegree;
                                                                                  avgRemoteDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgRemoteOutDegree;
                                                                          }       
                                                                  }
                                                                  resultSetNumber *= (avgDeg+avgRemoteDeg);
                                                                  Double eScanCost = prevScanCost * probability * avgDeg;
                                                                  Double vScanCost = new Double(0);
                                                                  Double networkCost = new Double(0);
                                                                  if(nextStep.property == null){
                                                                          vScanCost = eScanCost;
                                                                          networkCost = prevScanCost * probability * avgRemoteDeg;
                                                                  }
                                                                  else {
                                                                          if ( hueristics.edgePredicateMap.get(nextStep.property).containsKey(nextStep.value.toString()) ) {
                                                                                  vScanCost = eScanCost * hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                  networkCost =  prevScanCost * probability * avgRemoteDeg * hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                  resultSetNumber *= hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                          }
                                                                          else {
                                                                                  totalCost = new Double(-1);
                                                                                  break;
                                                                          }
                                                                  }
                                                                  System.out.println("RevCoeff:" + eScanCost.toString() +"," + vScanCost.toString() + "," + networkCost.toString());
                                                                  totalCost += (eScanCost+vScanCost +networkCost);
                                                                  prevScanCost = vScanCost;
                                                                  currentStep = revIt.previous();
                                                          }
                                                  }
                                                  joinCost *= resultSetNumber;
                                                  System.out.println("RevJoinCost:" + joinCost);
                                                  if ( queryCostHolder[pos] != -1 && totalCost != -1) {
                                                          queryCostHolder[pos] += totalCost;
                                                          if (pos!=0 && pos!= path.size()-1)
                                                                  queryCostHolder[pos] += joinCost;
                                                  }
                                                  else
                                                          queryCostHolder[pos] = new Double(-1);
                                                  
                                          }
                                          /* add that extra cost of initial scan*/
                                          //TODO: Add 1 when indexed
                                          initDone=true;//done to make it simulate like query is using indexes
                                          if ( queryCostHolder[pos] != -1 )
                                          {
                                                  if(!initDone)
                                                          queryCostHolder[pos] += hueristics.numVertices;
                                                  else
                                                          queryCostHolder[pos] +=1;
                                                          
                                                  System.out.println("Index:" + hueristics.numVertices);
                                          }
//                                        System.out.println(pos+":Total:"+String.valueOf(queryCostHolder[pos]));
                                  }
                                   
                          }
                         
                                    

          }
          
          
         
  }

        

        
        

        
        
        /**
         * SUPERSTEP 0:parse the query and initialize lucene
         * SUPERSTEP 1 and 2: populating InEdges
         * SUPERSTEP 3: Find execution plan and start execution
         * ....
         * REDUCE: write results
         */
        
        
       
        public static void compute() {
                

                
                // RUNTIME FUNCTIONALITITES 
                {
                       
                               
                                // COMPUTE HUERISTIC BASED QUERY COST
                                {
                                        // TODO: implementation for calc cost from middle of query ( for each position calc cost forward and backward cost and add them)
                                        
                                        for (int pos = 0;pos < path.size() ; pos+=2 ){
                                                Double joinCost = new Double(0);
                                                String eqStr="";
                                                //forward cost
                                                {      
                                                        Double totalCost = new Double(0);
                                                        Double prevScanCost = hueristics.numVertices;
                                                        Double resultSetNumber = hueristics.numVertices;
                                                        eqStr+=hueristics.numVertices.toString();
                                                        ListIterator<Step> It = path.listIterator(pos);
                                                        //Iterator<Step> It = path.iterator();
                                                        Step currentStep = It.next();
                                                        
                                                        while(It.hasNext()){
                                                                //cost calc
                                                                // TODO: make cost not count in probability when no predicate on edge/vertex
                                                                {
                                                                        Double probability = null;
                                                                        
                                                                        if ( currentStep.property == null )
                                                                                probability = new Double(1);
                                                                        else {
                                                                                if ( hueristics.vertexPredicateMap.get(currentStep.property).containsKey(currentStep.value.toString()) ){
                                                                                        probability = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).probability;
                                                                                        //System.out.println("Vertex Probability:" + probability);
                                                                                        
                                                                                }       
                                                                                else {
                                                                                        totalCost = new Double(-1);
                                                                                        break;
                                                                                }
                                                                        }
                                                                        eqStr+=probability.toString();
                                                                        resultSetNumber *= probability;
                                                                        Double avgDeg = new Double(0);
                                                                        Double avgRemoteDeg = new Double(0);
                                                                        Step nextStep = It.next();
                                                                        if(nextStep.direction == Step.Direction.OUT){
                                                                                if ( currentStep.property == null) {
                                                                                        avgDeg = hueristics.numEdges/hueristics.numVertices;
                                                                                        avgRemoteDeg = hueristics.numRemoteVertices/(hueristics.numVertices+hueristics.numRemoteVertices) * avgDeg;
                                                                                        //System.out.println("AVGDEG:" +avgDeg + "REMOTEAVGDEG:" + avgRemoteDeg);
                                                                                }       
                                                                                else { 
                                                                                        avgDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgOutDegree; 
                                                                                        avgRemoteDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgRemoteOutDegree;
                                                                                        //System.out.println("AVGDEG:" +avgDeg + "REMOTEAVGDEG:" + avgRemoteDeg);
                                                                                }       
                                                                        }else if(nextStep.direction == Step.Direction.IN){
                                                                                if ( currentStep.property == null) {
                                                                                        avgDeg = hueristics.numEdges/hueristics.numVertices;
                                                                                        avgRemoteDeg = hueristics.numRemoteVertices/(hueristics.numVertices+hueristics.numRemoteVertices) * avgDeg;
                                                                                        //System.out.println("AVGDEG:" +avgDeg + "REMOTEAVGDEG:" + avgRemoteDeg);
                                                                                }       
                                                                                else { 
                                                                                        avgDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgInDegree;
                                                                                        avgRemoteDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgRemoteInDegree;
                                                                                        //System.out.println("AVGDEG:" +avgDeg + "REMOTEAVGDEG:" + avgRemoteDeg);
                                                                                }               
                                                                        }
                                                                        eqStr += " " + avgDeg.toString() + " " + avgRemoteDeg.toString();
                                                                        resultSetNumber *= (avgDeg+avgRemoteDeg); 
                                                                        Double eScanCost = prevScanCost * probability * avgDeg;
                                                                        Double networkCost = new Double(0);
                                                                        Double vScanCost = new Double(0);
                                                                        if(nextStep.property == null)
                                                                                vScanCost = eScanCost;
                                                                        else {
                                                                                //output(partition.getId(), subgraph.getId(),nextStep.property);
                                                                                //output(partition.getId(), subgraph.getId(),nextStep.value.toString());
                                                                                //output(partition.getId(), subgraph.getId(),String.valueOf(hueristics.edgePredicateMap.size()));
                                                                                //output(partition.getId(), subgraph.getId(),String.valueOf(pos));
                                                                                //output(partition.getId(), subgraph.getId(),hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability.toString());
                                                                                //System.out.println(nextStep.property+":"+nextStep.value);
                                                                                if ( hueristics.edgePredicateMap.get(nextStep.property).containsKey(nextStep.value.toString()) ) {
                                                                                        vScanCost = eScanCost * hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                        eqStr+=" " + hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                        networkCost = networkCoeff * prevScanCost * probability * avgRemoteDeg * hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                        resultSetNumber *= hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                        //System.out.println("Edge:" + hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability);
                                                                                }
                                                                                else {
                                                                                        totalCost = new Double(-1);
                                                                                        break;
                                                                                }
                                                                        }
                                                                        totalCost += (eScanCost+vScanCost+networkCost);
                                                                        prevScanCost = vScanCost;
                                                                        currentStep = It.next();
                                                                }       
                                                                                                
                                                        }
                                                        joinCost += resultSetNumber;
                                                        queryCostHolder[pos] = totalCost;
                                                        
//                                                      System.out.println(pos+":"+"for:"+String.valueOf(totalCost));
                                                }
                                                eqStr+="$";
                                                //reverse cost
                                                {
                                                        Double totalCost = new Double(0);
                                                        Double prevScanCost = hueristics.numVertices;
                                                        Double resultSetNumber = hueristics.numVertices;

                                                        ListIterator<Step> revIt = path.listIterator(pos+1);
                                                        Step currentStep = revIt.previous();
                                                        while(revIt.hasPrevious()){
                                                                // TODO: make cost not count in probability when no predicate on edge/vertex
                                                                {
                                                                        Double probability = null;
                                                                        if ( currentStep.property == null )
                                                                                probability = new Double(1);
                                                                        else {
                                                                                if ( hueristics.vertexPredicateMap.get(currentStep.property).containsKey(currentStep.value.toString()) )
                                                                                        probability = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).probability;
                                                                                else {
                                                                                        totalCost = new Double(-1);
                                                                                        break;
                                                                                }
                                                                        }
                                                                        eqStr=probability.toString();
                                                                        resultSetNumber *= probability;
                                                                        Double avgDeg = new Double(0);
                                                                        Double avgRemoteDeg = new Double(0);
                                                                        Step nextStep = revIt.previous();
                                                                        if(nextStep.direction == Step.Direction.OUT){
                                                                                if ( currentStep.property == null) {
                                                                                        avgDeg = hueristics.numEdges/hueristics.numVertices;
                                                                                        avgRemoteDeg = hueristics.numRemoteVertices/(hueristics.numVertices+hueristics.numRemoteVertices) * avgDeg;
                                                                                }
                                                                                else {
                                                                                        avgDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgInDegree; 
                                                                                        avgRemoteDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgRemoteInDegree;
                                                                                }       
                                                                        }else if(nextStep.direction == Step.Direction.IN){
                                                                                if ( currentStep.property == null) {
                                                                                        avgDeg = hueristics.numEdges/hueristics.numVertices;
                                                                                        avgRemoteDeg = hueristics.numRemoteVertices/(hueristics.numVertices+hueristics.numRemoteVertices) * avgDeg;
                                                                                }
                                                                                else { 
                                                                                        avgDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgOutDegree;
                                                                                        avgRemoteDeg = hueristics.vertexPredicateMap.get(currentStep.property).get(currentStep.value.toString()).avgRemoteOutDegree;
                                                                                }       
                                                                        }
                                                                        eqStr += " " + avgDeg.toString() + " " + avgRemoteDeg.toString();
                                                                        resultSetNumber *= (avgDeg+avgRemoteDeg);
                                                                        Double eScanCost = prevScanCost * probability * avgDeg;
                                                                        Double vScanCost = new Double(0);
                                                                        Double networkCost = new Double(0);
                                                                        if(nextStep.property == null)
                                                                                vScanCost = eScanCost;
                                                                        else {
                                                                                if ( hueristics.edgePredicateMap.get(nextStep.property).containsKey(nextStep.value.toString()) ) {
                                                                                        vScanCost = eScanCost * hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                        eqStr+=" " + hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                        networkCost = networkCoeff * prevScanCost * probability * avgRemoteDeg * hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                        resultSetNumber *= hueristics.edgePredicateMap.get(nextStep.property).get(nextStep.value.toString()).probability;
                                                                                }
                                                                                else {
                                                                                        totalCost = new Double(-1);
                                                                                        break;
                                                                                }
                                                                        }
                                                                        totalCost += (eScanCost+vScanCost+networkCost);
                                                                        prevScanCost = vScanCost;
                                                                        currentStep = revIt.previous();
                                                                }
                                                        }
                                                        joinCost *= joinCoeff*resultSetNumber;
                                                        if ( queryCostHolder[pos] != -1 && totalCost != -1) {
                                                                queryCostHolder[pos] += totalCost;
                                                                if (pos!=0 && pos!= path.size()-1)
                                                                        queryCostHolder[pos] += joinCost;
                                                        }
                                                        else
                                                                queryCostHolder[pos] = new Double(-1);
                                                        
                                                }
                                                System.out
                                                    .println("Equation Coeff:" + eqStr);
                                                /* add that extra cost of initial scan*/
                                                //TODO: Add 1 when indexed
                                                initDone=true;//done to make it simulate like query is using indexes
                                                if ( queryCostHolder[pos] != -1 )
                                                {
                                                        if(!initDone)
                                                                queryCostHolder[pos] += hueristics.numVertices;
                                                        else
                                                                queryCostHolder[pos] +=1;
                                                                
                                                }
//                                              System.out.println(pos+":Total:"+String.valueOf(queryCostHolder[pos]));
                                        }
                                         
                                }
                                
                                
                                
                                
                                // LOAD START VERTICES
                                {
                                       
                                        int startPos=0;
                                        Double minCost = queryCostHolder[startPos];
                                        boolean queryPossible = true;
                                        String AllCostsStrings="";
                                        for (int i = 0; i < queryCostHolder.length ; i+=2) {//changed from i++ to i+=2
                                                if(AllCostsStrings.equals("")){
                                                  AllCostsStrings=queryCostHolder[i].toString();
                                                }
                                                else{
                                                  AllCostsStrings+="," + queryCostHolder[i];
                                                }
                                                
                                                if ( queryCostHolder[i]!=0 && queryCostHolder[i]!=-1 && queryCostHolder[i] < minCost ){
                                                        minCost=queryCostHolder[i];
                                                        startPos = i;
                                                }
                                                if( queryCostHolder[i]==-1 )
                                                        queryPossible = false;
                                        }
                                        
                                        String currentProperty = null;
                                        Object currentValue = null;
//                                      startPos=0;//used for debugging
                                        currentProperty = path.get(startPos).property; 
                                        currentValue = path.get(startPos).value;
                                        
                                        // TODO: check if the property is indexed** uncomment this if using indexes
                                        long QueryId=getQueryId();
                                       
                                                
                                      System.out.println("Starting Position:" + startPos +"  Query min Cost:" + minCost + "   Path Size:" + path.size());
                                      System.out.println("AllCosts:" + AllCostsStrings);
//                                      System.out.println("*******Querying done********:"+hits.length);
                                        
                                               
                                        
                
                                        // TODO : else iteratively check for satisfying vertices
//                                      if ( queryPossible == true )
//                                      for(IVertex<MapWritable, MapWritable, LongWritable, LongWritable> vertex: getSubgraph().getLocalVertices()) {
//                                              if ( vertex.isRemote() ) continue;
//                                              
//                                              if ( compareValuesUtil(vertex.getValue().get(new Text(currentProperty)).toString(), currentValue.toString()) ) {
//                                                      String _message = "V:"+String.valueOf(vertex.getVertexId().get());
//                                                      System.out.println("Vertex id:" + vertex.getVertexId().get() + "Property:"+currentProperty +" Value:" + vertex.getValue().get(new Text(currentProperty)).toString());
//                                                      if ( startPos == 0)
//                                                              forwardLocalVertexList.add( new VertexMessageSteps(QueryId,vertex.getVertexId().get(),_message, startPos, vertex.getVertexId().get(),startPos, getSubgraph().getSubgraphId().get(), 0) );
//                                                      else
//                                                      if( startPos == (path.size()-1))
//                                                              revLocalVertexList.add( new VertexMessageSteps(QueryId,vertex.getVertexId().get(),_message, startPos , vertex.getVertexId().get(),startPos, getSubgraph().getSubgraphId().get(), 0) );
//                                                      else{
//                                                              forwardLocalVertexList.add( new VertexMessageSteps(QueryId,vertex.getVertexId().get(),_message, startPos, vertex.getVertexId().get(),startPos, getSubgraph().getSubgraphId().get(), 0) );
//                                                              revLocalVertexList.add( new VertexMessageSteps(QueryId,vertex.getVertexId().get(),_message, startPos , vertex.getVertexId().get(),startPos, getSubgraph().getSubgraphId().get(), 0) );
//                                                      }
//                                                      //output(partition.getId(), subgraph.getId(), subgraphProperties.getValue(currentProperty).toString());
//                                              }
//                                      }
                                        
                                        
                                        
                                }
                                
                        
                        
                        

                }
                
                
               
        }

        
        static void LoadMetagraph(String fileName,int[] V,float[][] W) throws IOException {
        	try {
        		
        	
        	HashMap<String,String> subgraphRemap=new HashMap<>();
        	
        	subgraphRemap.put("19", "0");
        	subgraphRemap.put("4294967307", "1");
        	subgraphRemap.put("8589934609", "2");
        	subgraphRemap.put("12884901903", "3");
        	FileReader fr = new FileReader(fileName);
            BufferedReader br = new BufferedReader(fr);

            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
               String[] data=sCurrentLine.split(":");
               Long  reSGID=Long.parseLong(data[2]);
               Long localVertexCount=Long.parseLong(data[3]);
               V[reSGID.intValue()]=localVertexCount.intValue();
               Long remoteEdgeCount=Long.parseLong(data[4]);
               String outSubgraph=data[5];
               String[] outData=outSubgraph.split(",");
               for(int i=1;i<outData.length;i+=2) {
            	   String sgid=outData[i];
            	   String outEdgeCount=outData[i+1];
            	   String outReMap=subgraphRemap.get(sgid);
            	   if(outReMap!=null) {
            		   Long outReSGID=Long.parseLong(outReMap);
            		   Long outEdges=Long.parseLong(outEdgeCount);
            		   float outFraction=(float)outEdges/remoteEdgeCount;
            		   W[reSGID.intValue()][outReSGID.intValue()]=outFraction;
            	   }
               }
            }
            
            br.close();
        	}
        	catch(Exception e) {
        		e.printStackTrace();
        	}
        }

        /**
         * Simulation of compute for concurrent queries 
         * @throws IOException 
         */
        
        public static void computeSimulator() throws IOException {
            //metagraph  information , number of vertices per subgraph and fraction of edges between subgraphs
        	int[] V =new int[4];
        	float[][] W = new float[4][4];
        	//TODO:populate metagraph information here
        	String fileName="/home/abhilash/abhilash/Cit4PMetaGraph.txt";
        	LoadMetagraph(fileName,V,W);
        	
        	
        	//assuming number of partitions same as number of subgraphs
        	//Taking upper bounds on the size of this matrix
        	//matrix index: subgraphid,superstep,step
        	//taken as integer because we need floor
        	int[][][] n = new int[4][path.size()][path.size()];
        	
        	//residuals from earlier superstep
        	int[][][] l = new int[4][path.size()][path.size()];
        	
        	//Compute element count
        	int[][][] s = new int[4][path.size()][path.size()];
        	
        	for(int sgid=0;sgid<n.length;sgid++)//iterate through subgraphs
        	{
        		//for each subgraph,iterate through supersteps
        		for(int sp=1;sp<n[sgid].length;sp++) {
        			//for each superstep, iterate through steps
        			for(int step=0;step<n[sgid][sp].length;step++) {
	        			int curL=L(sgid,sp-1,step,n,l,s,V,W);
	        			int curS=S(sgid,sp,step,n,l,s,V,W);
	        			int curN=curL + curS;
	        			n[sgid][sp][step]=curN;
	        			
	        			System.out.println("Sim:" +sgid+","+sp+","+step+"," + curN + "," + curL + "," + curS);
        			}
        		}
     		}
    }        
        
        
        
    static int S(int _sgid,int _sp,int _step,int[][][] _n,int[][][] _l,int[][][] _s,int[] V,float[][] W) {
    	
    	int res;
    	Step s=path.get(_step);
		String property = s.property;
		String value=s.value.toString();
    	if(_step==0) {
    		res=(int) (V[_sgid]*hueristics.probabilityOfVertex(property, value));
    	}
    	else if(_step%2==0) {
    		
    		res=(int) (N(_sgid,_sp,_step-1,_n)* hueristics.probabilityOfVertex(property, value));
    	}
    	else {   		
    		res = (int) (N(_sgid,_sp,_step-1,_n) * hueristics.avgDeg(property, value, true, true)); //hueristics.probabilityOfEdge(property, value)
    	}
    	_s[_sgid][_sp][_step]=res;
    	return res;
    }
    
static int L(int _sgid,int _sp,int _step,int[][][] _n,int[][][] _l,int[][][] _s,int[] V,float[][] W) {
	int res;
	
	if(_sp==1 || _step%2==0) {
		res=0;
	}
	else {
		int sum=0;
		for(int j=0;j<_n.length;j++) {
			sum+=R(_sgid,_sp,_step,_n,_l,_s,V,W)*W[j][_sgid];
		}
		res=sum;
	}
    _l[_sgid][_sp][_step]=res;	
    return res;	
   }

static int R(int _sgid,int _sp,int _step,int[][][] _n,int[][][] _l,int[][][] _s,int[] V,float[][] W) {
	
	int res;
    if(_step%2==0) {
    	res=0;
    }
    else
    {
    	int h=(_step+1)/2;
    	Step s=path.get(h);
		String property = s.property;
		String value=s.value.toString();
    	res=(int) (N(_sgid,_sp,h,_n)*hueristics.avgRemoteDeg(property, value, true, true));
    }
	
	return res;
}
static int N(int _sgid,int _sp,int _step,int[][][] _n) {
	
	return _n[_sgid][_sp][_step];
}
        
        
static void LoadHeuristics(){
  try{/*Directly reading the gathered heuristics*/
    FileInputStream fis = new FileInputStream("/home/abhilash/abhilash/heuristics/hue_FULL.ser"); 
    ObjectInputStream ois = new ObjectInputStream(fis);
    hueristics = (Hueristics)ois.readObject();
    ois.close();
}catch(Exception e){e.printStackTrace();}
}

static void clear(){
 path.clear();
 queryCostHolder=null;
 noOfSteps=0;
}
        
public static void main(String[] args){
	System.out.println("Simulating Path Query");
//  networkCoeff=Double.parseDouble(args[1]);
//  joinCoeff=Double.parseDouble(args[2]);
//  indexCoeff=Double.parseDouble(args[3]);
//  System.out.println("Network Coeff:"+ networkCoeff);
//  System.out.println("Join Coeff:" + joinCoeff);
//  System.out.println("Index Coeff:" + indexCoeff);
  LoadHeuristics();
  System.out.println("Completed Loading of heuristics");
//  String queryStr="patid:string[4837891]@out?@patid:string[3287759]//0//163";
  String Args="";
  String fileName=args[0];
  System.out.println("reading File for arguments:" + args[0]);
  try
  {
    FileReader fr = new FileReader(fileName);
    BufferedReader br = new BufferedReader(fr);

    String sCurrentLine;

  

    while ((sCurrentLine = br.readLine()) != null) {
            
            Args=sCurrentLine;
            System.out.println(Args);
            init(Args);
            computeSimulator();
            clear();  
    }
    br.close();
  }catch(Exception e){
    System.out.println("Error:"+e.getMessage().toString());
  }
    
  
  System.out.println("Completed");
  
}
       








        
        
        
        
        
        
        
    
        
        
        
        //Getting identifier for a query
        //TODO:change this when implementing concurrent queries
        private static long getQueryId() {
                
                return 1;
        }
        
        
        //(*********** UTILITY FUNCTIONS**********
        
        
        /**
         * Utility function to compare two values
         * 
         */
        private boolean compareValuesUtil(Object o,Object currentValue){
                if( o.getClass().getName() != currentValue.getClass().getName()){return false;}
                if (o instanceof Float){
                        return ((Float)o).equals(currentValue);
                }
                else if (o instanceof Double){
                        return ((Double)o).equals(currentValue);
                }
                else if (o instanceof Integer){
                        return ((Integer)o).equals(currentValue);
                }
                else{
                        return ((String)o).equals(currentValue);
                }
                
        }
        







       




        
        
    
    
}

