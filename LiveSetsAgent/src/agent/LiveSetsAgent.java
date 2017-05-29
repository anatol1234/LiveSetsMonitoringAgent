package agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;

import alloclogger.AllocationLogger;

public class LiveSetsAgent {	

	public static void premain(String args, Instrumentation instrumentation) {
		main(instrumentation, args);
	}
	
	public static void agentmain(String args, Instrumentation instrumentation) {
		main(instrumentation, args);
	}
	
	private static void main(Instrumentation instrumentation, String args) {		
		AllocationTransformer transformer = new AllocationTransformer();
		AllocationLogger.init(new Runnable(){

			@Override
			public void run() {
				instrumentation.removeTransformer(transformer);
			}
			
		});
		instrumentation.addTransformer(transformer, true);
		List<Class<?>> loadedClasses = new ArrayList<>();
		for(Class<?> clazz: instrumentation.getAllLoadedClasses()) {
			if(instrumentation.isModifiableClass(clazz)){
				String rootPackage = clazz.getName().substring(0, clazz.getName().indexOf("."));
	            if(!"javassist".equals(rootPackage) && !"agent".equals(rootPackage) && !AllocationLogger.class.getPackage().getName().equals(rootPackage)) { 	            	
	            	loadedClasses.add(clazz);
	            }
			}
        }			
		try {
			instrumentation.retransformClasses(loadedClasses.toArray(new Class<?>[loadedClasses.size()]));
		} catch (UnmodifiableClassException e) {						
			System.err.println(e);						
		}
		AllocationLogger.getAllocationLogger().start();
	}
	
}
