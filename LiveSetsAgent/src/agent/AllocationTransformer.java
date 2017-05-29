package agent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import alloclogger.AllocationLogger;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

class AllocationTransformer implements ClassFileTransformer{
	private static final Logger LOGGER = Logger.getLogger(AllocationTransformer.class.getName());
	
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		AllocationLogger.getAllocationLogger().excludeThread();
		try{
			if(!className.startsWith(AllocationLogger.class.getPackage().getName())){					
				try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(classfileBuffer))) {				
					ClassFile clazz = new ClassFile(in);			
					transform(clazz);
					try(ByteArrayOutputStream out = new ByteArrayOutputStream();
							DataOutputStream dataOut = new DataOutputStream(out)) {
						clazz.write(dataOut);					
						return out.toByteArray();
					}
				}catch(IOException | BadBytecode e) {
					LOGGER.log(Level.SEVERE, "error occured", e);
				}catch(Throwable t) {
					LOGGER.log(Level.SEVERE, "error occured", t);
				}
			}
		}finally{
			AllocationLogger.getAllocationLogger().includeThread();
		}
		return classfileBuffer;
	}
	
	private void transform(ClassFile clazz) throws BadBytecode{				
		@SuppressWarnings("unchecked")
		List<MethodInfo> methods = clazz.getMethods();
		for(MethodInfo method : methods) {			
			transform(clazz.getName(), method);
		}
	}
	
	private void transform(String clazz, MethodInfo method) throws BadBytecode {		
		CodeAttribute code = method.getCodeAttribute();
		if(code != null){
			CodeIterator iterator = code.iterator();			
			while(iterator.hasNext()) {
				int bci = iterator.next();
				int opcode = iterator.byteAt(bci);
				if(opcode == Opcode.NEW) {
					int objectrefIndex = iterator.u16bitAt(bci+1);
					iterator.insert(bci, emitAllocationDetected(clazz, method, bci, objectrefIndex));
				}
			}
			code.computeMaxStack();
		}		
	}

	private byte[] emitAllocationDetected(String clazz, MethodInfo method, int newopbci, int objectrefIndex) {
		Bytecode bytecode = new Bytecode(method.getConstPool());
		bytecode.addInvokestatic(AllocationLogger.class.getName(), "getAllocationLogger", "()L" + AllocationLogger.class.getPackage().getName() + "/AllocationLogger;");
		bytecode.addLdc(method.getConstPool().getClassInfo(objectrefIndex));
		bytecode.addLdc(clazz);
		bytecode.addLdc(method.getName());	
		bytecode.addIconst(newopbci);
		bytecode.addIconst(method.getLineNumber(newopbci));
		bytecode.addInvokevirtual(AllocationLogger.class.getName(), "allocDetected", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V");
		return bytecode.get();
	}	
	
}