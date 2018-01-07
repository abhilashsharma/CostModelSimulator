package Testing.Testing;

import java.lang.instrument.Instrumentation;

public class ObjectSizeFetcher {
    private static Instrumentation instrumentation;

    public static void premain(String args, Instrumentation inst) {
        instrumentation = inst;
    }

    public static long getObjectSize(Object o) {
    	System.out.println(instrumentation + "," + o);
        return instrumentation.getObjectSize(o);
    }
}