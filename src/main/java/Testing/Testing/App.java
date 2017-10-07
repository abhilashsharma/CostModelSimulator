package Testing.Testing;

import com.google.gson.*;

/**
 * Hello world!
 *
 */
public class App 
{
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
    String sCurrentLine="(10,[(0,61322797)])";
    
//    System.out.println(sCurrentLine.indexOf(','));
    String pData = sCurrentLine.substring(sCurrentLine.indexOf(',')+2, sCurrentLine.length()-2);
    System.out.println("PData:"+pData);
	 String[] data= pData.split(",\\s+");
	 

		
	
	 for(String tuple:data) {
		 try {
		 String[] rtuple=tuple.substring(1, tuple.length()-1).split(",");
		 System.out.println(Long.parseLong(rtuple[1]) + "," + Long.parseLong(rtuple[0]));
		 
		 }catch(Exception e) {
			 System.out.println("Exception in Line:"+sCurrentLine+"  Exception:" + e.getMessage());
		 }
	 }
    }
}
