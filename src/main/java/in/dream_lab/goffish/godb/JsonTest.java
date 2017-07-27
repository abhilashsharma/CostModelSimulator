package in.dream_lab.goffish.godb;

import com.google.gson.*;
public class JsonTest {

	public static void main(String[] args){
	  String jsonLine="[1,\"properties\",[[1,2,1],[2,2,2]]]";
	  JsonElement jelement = new JsonParser().parse(jsonLine);
	  JsonArray jarray = jelement.getAsJsonArray();
	  JsonArray edgeList=(JsonArray) jarray.get(2);
	  System.out.println("First Edge:" + edgeList.get(0));
	  for (Object edgeInfo : edgeList) {
	    JsonArray edgeValues = ((JsonArray) edgeInfo).getAsJsonArray();
	    System.out.println(edgeValues.get(0).toString());
	    System.out.println(edgeValues.get(1).toString());
	    System.out.println(edgeValues.get(2).toString());
	  }
	}
}
