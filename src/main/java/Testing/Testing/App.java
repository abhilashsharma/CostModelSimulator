package Testing.Testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.hadoop.hbase.util.*;

import com.google.common.primitives.Longs;

import in.dream_lab.goffish.godb.Path;
import it.unimi.dsi.fastutil.longs.LongArrayList;



/**
 * Hello world!
 *
 */
public class App 
{
	
	/**
	 * Representative class to keep tab of next vertex to be processed, different for path query
	 */
	public static class VertexMessageSteps{
		Long queryId;
		Long vertexId;
//		String message;
		Path path;
		Integer stepsTraversed;
		Long previousSubgraphId;
		Integer previousPartitionId;
		Long startVertexId;
		Integer startStep;
//		VertexMessageSteps(Long _queryId,Long _vertexId,String _message,Integer _stepsTraversed,Long _startVertexId,Integer _startStep,Long _previousSubgraphId, Integer _previousPartitionId){
//			this.queryId=_queryId;
//			this.vertexId = _vertexId;
//			this.message = _message;
//			this.stepsTraversed = _stepsTraversed;
//			this.previousSubgraphId = _previousSubgraphId;
//			this.startVertexId = _startVertexId;
//			this.startStep=_startStep;
//			this.previousPartitionId = _previousPartitionId;
//		}
		
		VertexMessageSteps(Long _queryId,Long _vertexId,Path p,Integer _stepsTraversed,Long _startVertexId,Integer _startStep,Long _previousSubgraphId, Integer _previousPartitionId){
			this.queryId=_queryId;
			this.vertexId = _vertexId;
			this.path = p;
			this.stepsTraversed = _stepsTraversed;
			this.previousSubgraphId = _previousSubgraphId;
			this.startVertexId = _startVertexId;
			this.startStep=_startStep;
			this.previousPartitionId = _previousPartitionId;
		}
	}
    public static void main( String[] args )
    {
      /*String JSONString="[38289734, 'url:String:http://lists.apple.com/archives/Fed-talk/2007/Nov/msg00046.html$domain:String:http://lists.apple.com/$', []]";
      JsonParser GsonParser = new JsonParser();
      JsonArray JSONInput = GsonParser.parse(JSONString).getAsJsonArray();
        System.out.println( "Source Property:" + JSONInput.get(1).toString() );*/
    	
    //(10,[(8,130298428), (2,20232602), (9,4756160)])
    //	(8,[(6,35923544)])
    	//(10,[(0,61322797)])
    	//(8,[(5,1433902)])
//    String sCurrentLine="(10,[(0,61322797)])";
//    
////    System.out.println(sCurrentLine.indexOf(','));
//    String pData = sCurrentLine.substring(sCurrentLine.indexOf(',')+2, sCurrentLine.length()-2);
//    System.out.println("PData:"+pData);
//	 String[] data= pData.split(",\\s+");
//	 
//
//		
//	
//	 for(String tuple:data) {
//		 try {
//		 String[] rtuple=tuple.substring(1, tuple.length()-1).split(",");
//		 System.out.println(Long.parseLong(rtuple[1]) + "," + Long.parseLong(rtuple[0]));
//		 
//		 }catch(Exception e) {
//			 System.out.println("Exception in Line:"+sCurrentLine+"  Exception:" + e.getMessage());
//		 }
//	 }
//    	String test="293905#7";
//    	System.out.println(test.split("#")[1]);
    	
//    	String TestJsonArray="[11152400,\"vid:Long:11152400$Language:String:Java$Industry:String:ConsumerServices$Contributors:Integer:55$IsPublic:Boolean:1$\",[[9706751],[26752642],[44856961],[83240784],[12820864],[132267648],[100954530],[162625813],[4718530]]]";
//    	 JsonParser GsonParser = new JsonParser();
//    	 JsonArray JSONInput = GsonParser.parse(TestJsonArray).getAsJsonArray();
//		System.out.println("SRC:" + Long.valueOf(JSONInput.get(0).toString()));
//		JsonArray edgeList = (JsonArray) JSONInput.get(2);
//		for (Object edgeInfo : edgeList) {
//		      JsonArray edgeValues = ((JsonArray) edgeInfo).getAsJsonArray();
//		     System.out.println("EDGEJSON:"+edgeValues.toString());
//		          System.out.println(Long.valueOf(edgeValues.get(0).toString()));
//		}
//    	Long[] test= new Long[5];
//    	test[0]=new Long(123123l);
//    	test[1]=new Long(123l);
//    	test[2]=new Long(234l);
//    	test[3]=new Long(9);
//    	
//    	Byte[] b= new Byte[8];
//    	//THIS is profiler measurement of memory
//    	LinkedList<VertexMessageSteps> nextStepForwardLocalVertexList = new LinkedList<VertexMessageSteps>();
////    	List<VertexMessageSteps> ForwardLocalVertexList = new ArrayList<VertexMessageSteps>();
////    	VertexMessageSteps v=new VertexMessageSteps(1l,1234567l, "" ,3, 987623l,2, 19l, 0);
////    	VertexMessageSteps vs=new VertexMessageSteps(1l,1234567l, "" ,3, 987623l,2, 19l, 0);
////    	System.out.println("HopSize:" + ObjectSizeFetcher.getObjectSize(vnull));
//    	int count=0;
////    	VertexMessageSteps vnull=new VertexMessageSteps(null,null,null,null,null,null,null,null);
//    	while(count < 1000000) {
//    		if(count%1000==0) {
////    			long size=ObjectSizeFetcher.getObjectSize(ForwardLocalVertexList);
////    			System.out.println("ListSize:" + count + "," + size);
//    		}
//    		Path p=new Path(12397l);
//    		p.addEV(23423l, 23478633l);
//    		p.addEV(23423l, 23478633l);
//    		p.addEV(23423l, 23478633l);
//    		p.addEV(23423l, 23478633l);
//    		VertexMessageSteps v=new VertexMessageSteps(1l,1234567l, p ,3, 987623l,2, 19l, 0);
//    		nextStepForwardLocalVertexList.add(v);
//    		count++;
//    	}
//    	
////    String s1="";
////    String s2="aaaaaaaaaaaaaaaaa";
////    String[] s3= {"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","sdfsdfsd","sdfsdfsdfds","asdasdasdasd","sdfsdfsdfsd" , "asfasdasdasd","sdfsdfsdfsdfsda"};
////    System.out.println("s1:"+ObjectSizeFetcher.getObjectSize(s1));	
////    System.out.println("s2:"+ObjectSizeFetcher.getObjectSize(s2));
////    System.out.println("s3:"+ObjectSizeFetcher.getObjectSize(s3));
//    	System.out.println("Done");
//    	while(true) {
//    	
//    	}
//    	String line="1@2%3|";
//    	String[] data =line.split("\\W");
//    	String test="l";
//    	
//    	System.out.println(test.substring(0, test.length() - 1).equals(""));
    	Splitter splitter=Splitter.createSplitter();
        String Val="1#234@2134234#234#";
        byte[] test=Val.getBytes();
        
        long start=System.nanoTime();
        LongArrayList sList=splitter.splitLong(test);
        System.out.println("Splitter Time:" + (System.nanoTime()-start));
       
    	
        start=System.nanoTime();
        String[] tokens= Val.split("\\W");
        LongArrayList l=new LongArrayList();
        for(String t:tokens) {
        	l.add(Long.parseLong(t));
        }
        System.out.println("String Time:" + (System.nanoTime()-start));
    	
    }
}
