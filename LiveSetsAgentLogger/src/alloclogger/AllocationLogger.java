package alloclogger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AllocationLogger {
    private static final int MAX_SHUTDOWN_QUIET_TIME = 10000;
    private static final int MIN_TIME_SINCE_LAST_ALLOC = 1000;
    private static final long MAX_OUTPUT_ALLOCSITES_COUNT = 1000;
    private static final long NO_THREAD = -1;

    private static AllocationLogger allocationLogger;
    private final Map<AllocationSite, Integer> allocSiteMap = new HashMap<>();
    private final NonReentrantLock loggingLock = new NonReentrantLock();
    private final Object shutdownHeuristicLock = new Object();
    private volatile boolean isLoggingEnabled = false;
    private volatile boolean isShuttingDown = false;
    private volatile long excludedThread = NO_THREAD;
    private long shutdownThread;
    private long lastAllocTime;
    private int totalAllocCount = 0;

    private AllocationLogger(Runnable onShutdown) {
        Thread shutdownThread = new Thread(
                new Runnable() {
                    public void run() {
                        onShutdown.run();
                        shutdownLogging();
                        outputResults();
                    }
                }
        );
        this.shutdownThread = shutdownThread.getId();
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    public static void init(Runnable onShutdown) {
        allocationLogger = new AllocationLogger(onShutdown);
    }

    public static AllocationLogger getAllocationLogger() {
        return allocationLogger;
    }

    public void start() {
        if (!isShuttingDown) {
            this.isLoggingEnabled = true;
        }
    }

    public boolean isLogging() {
        return isLoggingEnabled;
    }

    public void allocDetected(String allocTypeName, String className, String methodName, int bci, int lnr) {
        if (lockForLogging()) {
            try {
               logAllocation(new AllocationSite(allocTypeName, className, methodName, bci, lnr));
            } finally {
                loggingLock.unlock();
            }
        }
    }

    public void excludeThread() {
        excludedThread = Thread.currentThread().getId();
    }

    public void includeThread() {
        excludedThread = NO_THREAD;
    }

    private boolean lockForLogging(){
    	if (loggingLock.lock()) {
	    	if (isLoggingEnabled && Thread.currentThread().getId() != excludedThread && Thread.currentThread().getId() != shutdownThread) {
	    		synchronized (shutdownHeuristicLock) { // shutdown heuristic
	    			lastAllocTime = System.currentTimeMillis();
	                shutdownHeuristicLock.notifyAll();
	            }
	            return true;
	    	}else{
	    		loggingLock.unlock();
	    	}
    	}
    	return false;
    }
             
    
    /*
     * Is only allowed to allocate on the current thread.
     */
    private void logAllocation(AllocationSite allocSite) {
        allocSiteMap.put(allocSite, allocSiteMap.getOrDefault(allocSite, 0) + 1);        
        totalAllocCount++;
    }

    /*
     * Is only allowed to allocate on the current thread.
     */
    private void shutdownLogging() {
        System.out.println("Shutting down agent ...");
        long startTime = System.currentTimeMillis();
        synchronized (shutdownHeuristicLock) { // shutdown heuristic
            long ctime;
            while ((ctime = System.currentTimeMillis()) - lastAllocTime < MIN_TIME_SINCE_LAST_ALLOC && ctime - startTime < MAX_SHUTDOWN_QUIET_TIME) {
                try {
                    shutdownHeuristicLock.wait(MIN_TIME_SINCE_LAST_ALLOC);
                } catch (InterruptedException e) { /* continue waiting */ }
            }
        }
        loggingLock.lock();
        isShuttingDown = true;
        isLoggingEnabled = false;
        loggingLock.unlock();
    }

    private void outputResults() {
        List<Map.Entry<AllocationSite, Integer>> topAllocSites = allocSiteMap.entrySet().stream()
                .sorted(new Comparator<Map.Entry<AllocationSite, Integer>>() {
                    public int compare(Map.Entry<AllocationSite, Integer> e1, Map.Entry<AllocationSite, Integer> e2) {
                        return e2.getValue() - e1.getValue();
                    }
                })
                .limit(MAX_OUTPUT_ALLOCSITES_COUNT)
                .collect(Collectors.toList());
        int maxIndexLen = String.valueOf(topAllocSites.size()).length();
        int maxAllocCountLen = topAllocSites.size() > 0 ? String.valueOf(topAllocSites.get(0).getValue()).length() : 0;
        String formatStr = "%-" + (maxIndexLen + 1) + "s %" + maxAllocCountLen + "d %s";
        int index = 1;
        for (Map.Entry<AllocationSite, Integer> entry : topAllocSites) {
            AllocationSite allocSite = entry.getKey();
            String allocSiteRepr = allocSite.getAllocTypeName() + " @ "
                    + allocSite.getClassName() + "::"
                    + allocSite.getMethodName() + "()"
                    + " (" + (allocSite.getLnr() == -1 ? "BCI " + allocSite.getBci() : "Line " + allocSite.getLnr()) + ")";
            System.out.println(String.format(formatStr, index++ + ")", entry.getValue(), allocSiteRepr));
        }
        System.out.println("total allocations count: " + totalAllocCount);
    }

}
