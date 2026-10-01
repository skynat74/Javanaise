/***
 * JAVANAISE Implementation
 * JvnCoordImpl class
 * Contact:  
 *
 * Authors: 
 */

package jvn;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//extends UnicastRemoteObject : Instances of this class can be exposed as RMI remote objects
public class JvnCoordImpl
		extends UnicastRemoteObject
		implements JvnRemoteCoord {

	private static final long serialVersionUID = 1L;

	private int nextObjectId = 0;
	/** symbolic name → shared object (keeps latest known state) */
	private final Map<String, JvnObject> nameToObject = new HashMap<>();

	/** object id → shared object */
	private final Map<Integer, JvnObject> idToObject = new HashMap<>();

	/** object id → unique writer server (or null) */
	private final Map<Integer, JvnRemoteServer> writers = new HashMap<>();

	/** object id → set of reader servers */
	private final Map<Integer, Set<JvnRemoteServer>> readers = new HashMap<>();

	/**
	* Default constructor
	* @throws JvnException
	**/
	public JvnCoordImpl() throws Exception {
		super();
		Registry registry = LocateRegistry.createRegistry(1099);
    //rebind : Binds a name to a remote object, registering the object in the RMI registry.
		registry.rebind("JvnCoord", this);
		System.out.println("Coordinator ready as JvnCoord on port 1099");
	}

	public static void main(String[] args) {
		try {
			new JvnCoordImpl();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
  *  Allocate a NEW JVN object id (usually allocated to a 
  *  newly created JVN object)
	* @throws java.rmi.RemoteException,JvnException
	**/
	public synchronized int jvnGetObjectId()
			throws java.rmi.RemoteException, JvnException {
		return nextObjectId++;
	}

	/**
	* Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo  : the JVN object 
	* @param js  : the remote reference of the JVNServer
	* @throws java.rmi.RemoteException,JvnException
	**/
	public synchronized void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
			throws java.rmi.RemoteException, JvnException {
		int id = jo.jvnGetObjectId();
		nameToObject.put(jon, jo);
		idToObject.put(id, jo);
		// Creator starts with a write lock (see Irc)
		writers.put(id, js);
		readers.put(id, new HashSet<>());
	}

	/**
	* Get the reference of a JVN object managed by a given JVN server 
	* @param jon : the JVN object name
	* @param js : the remote reference of the JVNServer
	* @throws java.rmi.RemoteException,JvnException
	**/
	public synchronized JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
			throws java.rmi.RemoteException, JvnException {
		return nameToObject.get(jon);
	}

	/**
	* Get a Read lock on a JVN object managed by a given JVN server 
	* @param joi : the JVN object identification
	* @param js  : the remote reference of the server
	* @return the current JVN object state
	* @throws java.rmi.RemoteException, JvnException
	**/
	public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js)
			throws java.rmi.RemoteException, JvnException {
		JvnRemoteServer writer = writers.get(joi);
		if (writer != null) {
			// Downgrade writer → reader and refresh coordinator state
			Serializable latest = writer.jvnInvalidateWriterForReader(joi);
			updateObjectState(joi, latest);
			writers.remove(joi);
			readers.get(joi).add(writer);
		}
		readers.get(joi).add(js);
		return idToObject.get(joi).jvnGetObjectState();
	}

	/**
	* Get a Write lock on a JVN object managed by a given JVN server 
	* @param joi : the JVN object identification
	* @param js  : the remote reference of the server
	* @return the current JVN object state
	* @throws java.rmi.RemoteException, JvnException
	**/
	public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js)
			throws java.rmi.RemoteException, JvnException {
		Set<JvnRemoteServer> readerSet = readers.get(joi);

		if (readerSet != null && !readerSet.isEmpty()) {
			for (JvnRemoteServer s : readerSet) {
				if (!s.equals(js)) {
					s.jvnInvalidateReader(joi);
				}
			}
			readerSet.clear();
		} else {
			JvnRemoteServer writer = writers.get(joi);
			if (writer != null && !writer.equals(js)) {
				Serializable latest = writer.jvnInvalidateWriter(joi);
				updateObjectState(joi, latest);
			}
		}

		writers.put(joi, js);
		return idToObject.get(joi).jvnGetObjectState();
	}

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
	public synchronized void jvnTerminate(JvnRemoteServer js)
			throws java.rmi.RemoteException, JvnException {
		for (Set<JvnRemoteServer> set : readers.values()) {
			set.remove(js);
		}
		writers.entrySet().removeIf(e -> js.equals(e.getValue()));
	}

	private void updateObjectState(int joi, Serializable state) throws JvnException {
		JvnObject jo = idToObject.get(joi);
		if (jo instanceof JvnObjectImpl) {
			((JvnObjectImpl) jo).setObjectState(state);
		}
	}
}
