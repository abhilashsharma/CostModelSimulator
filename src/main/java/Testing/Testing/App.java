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
      String JSONString="[38289734, 'url:String:http://lists.apple.com/archives/Fed-talk/2007/Nov/msg00046.html$domain:String:http://lists.apple.com/$', []]";
      JsonParser GsonParser = new JsonParser();
      JsonArray JSONInput = GsonParser.parse(JSONString).getAsJsonArray();
        System.out.println( "Source Property:" + JSONInput.get(1).toString() );
    }
}
