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







public class PathQuerySimulatorFractionalSubgraphLevelResidualTermination{ 
        
        
        
        public static final Log LOG = LogFactory.getLog(PathQuerySimulatorFractionalSubgraphLevelResidualTermination.class);
        
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


        static Hueristics[] hueristics = new Hueristics[4]; 
        
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
               Long  reSGID=Long.parseLong(subgraphRemap.get(data[2]));
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
        	System.out.println("Verifying MetaGraph:");
        	for(int count:V) {
        		System.out.println("VCount:"+count);
        	}
        	
        	for(int i=0;i<W.length;i++) {
        		for(int j=0;j<W[i].length;j++) {
        			System.out.print(W[i][j]+" ");
        		}
        	}
        	
        	System.out.println("\nVerification Done");
        	
        	//assuming number of partitions same as number of subgraphs
        	//Taking upper bounds on the size of this matrix
        	//matrix index: subgraphid,superstep,step
        	float[][][] n = new float[4][path.size()][path.size()];
        	
        	//residuals from earlier superstep
        	float[][][] l = new float[4][path.size()][path.size()];
        	
        	//Compute element count
        	float[][][] s = new float[4][path.size()][path.size()];
        	for(int sp=1;sp<path.size();sp++) 
        	//iterate through supersteps
        	{
        		//for each superstep,iterate through sugraph
        		for(int sgid=0;sgid<n.length;sgid++){
        			//for each superstep, iterate through steps
        			for(int step=0;step<n[sgid][sp].length;step++) {
	        			float curL=L(sgid,sp-1,step,n,l,s,V,W);
	        			float curS=S(sgid,sp,step,n,l,s,V,W);
	        			float curN=curL + curS;
	        			n[sgid][sp][step]=curN;
	        			
	        			System.out.println("Sim:" +sgid+","+sp+","+step+"," + curN + "," + curL + "," + curS);
        			}
        		}
     		}
    }        
        
        
        
    static float S(int _sgid,int _sp,int _step,float[][][] _n,float[][][] _l,float[][][] _s,int[] V,float[][] W) {
    	
    	float res;
    	
    	if(_sp==1 &&_step==0) {
    		Step s=path.get(_step);
    		String property = s.property;
    		Object value=s.value;
    		System.out.println("In:"+(V[_sgid]*hueristics[_sgid].probabilityOfVertex(property, value.toString())));
    		res=(float) (V[_sgid]*hueristics[_sgid].probabilityOfVertex(property, value.toString()));
    	}
    	else if(_step%2==0) {
    		int h=(_step+2)/2;//for vertex step h is this
    		int index=2*h-2;
    		Step s=path.get(index);
    		String property = s.property;
    		Object value=s.value;
    		System.out.println("HVLocal:" + (2*h-2)+" prop:"+property+" val:"+s.value + " "+hueristics[_sgid].probabilityOfVertex(property, value.toString()) +" " + N(_sgid,_sp,_step-1,_n) );
    		res=(float) (N(_sgid,_sp,_step-1,_n)* hueristics[_sgid].probabilityOfVertex(property, value.toString()));
    	}
    	else {
    		int h=(_step+1)/2;//for edge step h is this
    		int vindex=2*h-2;//for vertex
    		Step s=path.get(vindex);
    		String property = s.property;
    		Object value=s.value;
    		System.out.println("HELocal:" + (2*h-2)+" prop:"+property+" val:"+s.value + " "+hueristics[_sgid].avgDeg(property,value.toString() , true, true) +" " + N(_sgid,_sp,_step-1,_n) );
    		res = (float) (N(_sgid,_sp,_step-1,_n) * hueristics[_sgid].avgDeg(property,value.toString() , true, true)); //hueristics.probabilityOfEdge(property, value)
    	}
    	_s[_sgid][_sp][_step]=res;
    	return res;
    }
    
static float L(int _sgid,int _sp,int _step,float[][][] _n,float[][][] _l,float[][][] _s,int[] V,float[][] W) {
	float res;
	
	if(_sp==0 || _step%2==0) {
		res=0;
	}
	else {
		float sum=0;
		for(int j=0;j<_n.length;j++) {
			sum+=R(_sgid,_sp,_step,_n,_l,_s,V,W)*W[j][_sgid];
			System.out.println("LResSG:"+j+","+_sgid+":"+R(_sgid,_sp,_step,_n,_l,_s,V,W)*W[j][_sgid]);
		}
		if(sum>=1) {
			res=sum;
		}
		else {
			res=0;
		}
	}
    _l[_sgid][_sp][_step]=res;
    System.out.println("LRes:"+ _l[_sgid][_sp][_step]);
    return res;	
   }

static float R(int _sgid,int _sp,int _step,float[][][] _n,float[][][] _l,float[][][] _s,int[] V,float[][] W) {
	
	float res;
    if(_step%2==0) {
    	res=0;
    }
    else
    {
    	int h=(_step+1)/2;
    	int vindex=2*h-2;
    	Step s=path.get(vindex);
		String property = s.property;
		String value=s.value.toString();
		System.out.println("HRemote:"+ (2*h-2)+" prop:"+property+" val:"+s.value + " "+hueristics[_sgid].avgRemoteDeg(property, value, true, true) +" " + N(_sgid,_sp,_step-1,_n));
    	res=(float) (N(_sgid,_sp,_step-1,_n)*hueristics[_sgid].avgRemoteDeg(property, value, true, true));
//    	System.out.println("RemoteRES:"+res);
    }
	
	return res;
}

static float N(int _sgid,int _sp,int _step,float[][][] _n) {
	
	if(_step==-1)
		return 0;
	
	return _n[_sgid][_sp][_step];
}
        
        
static void LoadHeuristics(){
	HashMap<String,String> subgraphRemap=new HashMap<>();
	
	subgraphRemap.put("19", "0");
	subgraphRemap.put("4294967307", "1");
	subgraphRemap.put("8589934609", "2");
	subgraphRemap.put("12884901903", "3");
	
	
	for(Map.Entry<String, String> entry:subgraphRemap.entrySet()) {
		
		  try{/*Directly reading the gathered heuristics*/
		    FileInputStream fis = new FileInputStream("/home/abhilash/abhilash/heuristicsCitPatent/hue_"+entry.getKey()+".ser"); 
		    ObjectInputStream ois = new ObjectInputStream(fis);
		    hueristics[Integer.parseInt(entry.getValue())] = (Hueristics)ois.readObject();
		    ois.close();
		  		}catch(Exception e){e.printStackTrace();}
	}
	
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
   e.printStackTrace();
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

