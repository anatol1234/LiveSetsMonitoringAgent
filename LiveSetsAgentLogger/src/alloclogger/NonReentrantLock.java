package alloclogger;

class NonReentrantLock {
	private static final long NO_THREAD = -1;
    private long lockingThread = NO_THREAD;

    synchronized boolean lock(){
        if(lockingThread == Thread.currentThread().getId()){
        	return false;
        }        
        while (lockingThread != NO_THREAD) {  
        	try{        		
        		wait();
        	}catch(InterruptedException e){/* continue waiting */}
        }
        lockingThread = Thread.currentThread().getId();
        return true;
    }

    synchronized boolean unlock(){
    	if (lockingThread == NO_THREAD || lockingThread != Thread.currentThread().getId()) {
            return false;
        }      
        lockingThread = NO_THREAD;
        notifyAll();
        return true;
    }
    
}
