package net.sf.drftpd.master;

import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.util.Collection;

import net.sf.drftpd.NoAvailableSlaveException;
import net.sf.drftpd.event.SlaveEvent;
import net.sf.drftpd.slave.Slave;
import net.sf.drftpd.slave.SlaveStatus;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Class would fit both in net.sf.drftpd.slave and net.sf.drftpd.master.
 * However, as it is instantiated from the slave (or master with local slave),
 * and mainly because it is a helper class for Slave, it is located in net.sf.drftpd.slave.
 * 
 * @author <a href="mailto:drftpd@mog.se">Morgan Christiansson</a>
 */
public class RemoteSlave implements Serializable, Comparable {

	private static Logger logger =
		Logger.getLogger(RemoteSlave.class.getName());
	static {
		logger.setLevel(Level.ALL);
	}

	private SlaveManagerImpl manager;
	private String name;
	private Slave slave;
	private SlaveStatus status;
	private long statusTime;
	private Collection masks;
	
	public RemoteSlave(String name) {
		this.name = name;
	}

	public RemoteSlave(String name, Collection masks, SlaveManagerImpl manager) {
		this(name, masks);
		this.manager = manager;
	}
	public RemoteSlave(String name, Collection masks) {
		this.name = name;
		this.masks = masks;
	}
	
	/**
	 * @deprecated
	 */
	public RemoteSlave(String name, Slave slave) {
		if (name == null)
			throw new IllegalArgumentException("name cannot be null (did you set slave.name?)");
		this.slave = slave;
		this.name = name;
	}
	
	public SlaveManagerImpl getManager() {
		return manager;
	}

	/**
	 * Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * Throws NoAvailableSlaveException only if slave is offline
	 * @return
	 * @throws NoAvailableSlaveException
	 */
	public Slave getSlave() throws NoAvailableSlaveException {
		if (slave == null)
			throw new NoAvailableSlaveException("slave is offline");
		return slave;
	}

	/**
	 * Get's slave status, caches the status for 10 seconds.
	 * @return
	 * @throws RemoteException
	 * @throws NoAvailableSlaveException
	 */
	public SlaveStatus getStatus()
		throws RemoteException, NoAvailableSlaveException {
		return getSlave().getSlaveStatus();
			
//		if (statusTime < System.currentTimeMillis() - 10000) {
//			status = getSlave().getSlaveStatus();
//			statusTime = System.currentTimeMillis();
//		}
//		return status;
	}
	
	/**
	 * @param ex RemoteException
	 * @return true If exception was fatal and the slave was removed 
	 */
	public boolean handleRemoteException(RemoteException ex) {
		if (!isFatalRemoteException(ex)) {
			logger.log(Level.WARN, "Caught non-fatal exception from "+getName()+", not removing", ex);
			return false;
		}
		logger.warn("Fatal exception from "+getName()+", removing", ex);
		setOffline(ex.getCause().getMessage());
		return true;
	}

	public int hashCode() {
		return this.getName().hashCode();
	}

	public static boolean isFatalRemoteException(RemoteException ex) {
		return (ex instanceof ConnectException || ex instanceof ConnectIOException);
	}
	private InetAddress inetAddress;
	
	public void setManager(SlaveManagerImpl manager) {
		if(this.manager != null) throw new IllegalStateException("Can't overwrite manager");
		this.manager = manager;
	}

	private long lastPing;
	public void ping() throws RemoteException, NoAvailableSlaveException {
		if(slave == null) throw new NoAvailableSlaveException(getName()+" is offline");
		if(System.currentTimeMillis() > lastPing+1000) {
			getSlave().ping();
		}
	}
	
	public boolean isAvailablePing() {
		try {
			getSlave().ping();
		} catch (RemoteException e) {
			handleRemoteException(e);
			return false;
		} catch (NoAvailableSlaveException e) {
			return false;
		}
		return isAvailable();
	}
	public boolean isAvailable() {
		return slave != null;
	}

	public String toString() {
		String str = this.getName();
		try {
			//System.out.println("getRef().remoteToString(): "+
			str = str + "[slave=" + this.getSlave().toString() + "]";
		} catch (NoAvailableSlaveException e) {
			str = str + "[slave=offline]";
		}
		return str.toString();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof RemoteSlave) {
			RemoteSlave rslave = (RemoteSlave) obj;
			if (rslave.getName().equals(this.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return
	 */
	public Collection getMasks() {
		return masks;
	}

	/**
	 * @param slave
	 */
	public void setSlave(Slave slave, InetAddress inetAddress) {
		if(slave == null && this.slave != null) {
			manager.getConnectionManager().dispatchFtpEvent(new SlaveEvent("DELSLAVE", this));
		}
		this.slave = slave;
		this.inetAddress = inetAddress;
	}

	/**
	 * @param collection
	 */
	public void setMasks(Collection collection) {
		masks = collection;
	}
	private long lastUploadReceiving=0;
	public long getLastUploadReceiving() {
		return this.lastUploadReceiving;
	}
	public void setLastUploadReceiving(long lastUploadReceiving) {
		this.lastUploadReceiving = lastUploadReceiving;
	}

	private long lastDownloadSending=0;
	public long getLastDownloadSending() {
		return this.lastDownloadSending;
	}
	public void setLastDownloadSending(long lastDownloadSending) {
		this.lastDownloadSending = lastDownloadSending;
	}
	
	public long getLastTransfer() {
		return Math.max(getLastDownloadSending(), getLastUploadReceiving());
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		if(!(o instanceof RemoteSlave)) throw new IllegalArgumentException();
		return getName().compareTo(((RemoteSlave)o).getName());
	}
	/**
	 * @return
	 */
	public InetAddress getInetAddress() {
		return inetAddress;
	}

	/**
	 * 
	 */
	public void setOffline(String reason) {
		manager.getConnectionManager().dispatchFtpEvent(new SlaveEvent("DELSLAVE", reason, this));
		this.slave = null;
		this.inetAddress = null;
	}

}
