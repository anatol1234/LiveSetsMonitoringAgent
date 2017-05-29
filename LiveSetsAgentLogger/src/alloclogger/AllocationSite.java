package alloclogger;

public class AllocationSite {
	private final String allocTypeName;
	private final String className;
	private final String methodName;
	private final int bci;
	private final int lnr;
	
	AllocationSite(String allocTypeName, String className, String methodName, int bci, int lnr){
		this.allocTypeName = allocTypeName;
		this.className = className;
		this.methodName = methodName;
		this.bci = bci;
		this.lnr = lnr;
	}	

	/**
	 * @return the allocTypeName
	 */
	public String getAllocTypeName() {
		return allocTypeName;
	}

	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @return the methodName
	 */
	public String getMethodName() {
		return methodName;
	}

	/**
	 * @return the bci
	 */
	public int getBci() {
		return bci;
	}

	/**
	 * @return the lnr
	 */
	public int getLnr() {
		return lnr;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bci;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AllocationSite other = (AllocationSite) obj;
		if (bci != other.bci)
			return false;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		if (methodName == null) {
			if (other.methodName != null)
				return false;
		} else if (!methodName.equals(other.methodName))
			return false;
		return true;
	}
}
