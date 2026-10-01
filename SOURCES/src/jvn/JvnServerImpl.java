/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Contact: 
 * 
 * Authors: 
 */

package jvn;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class JvnServerImpl
		extends UnicastRemoteObject
		implements JvnLocalServer, JvnRemoteServer {

	private static final long serialVersionUID = 1L;

	// A JVN server is managed as a singleton 
	private static JvnServerImpl js = null;

	private JvnRemoteCoord coord;
	private final Map<Integer, JvnObject> localObjects = new HashMap<>();

	/**
	* Default constructor
	* @throws JvnException
	**/
	private JvnServerImpl() throws Exception {
		super();
		Registry registry = LocateRegistry.getRegistry(1099);
		coord = (JvnRemoteCoord) registry.lookup("JvnCoord");
	}

	/**
	* Static method allowing an application to get a reference to 
	* a JVN server instance
	* @throws JvnException
	**/
	public static JvnServerImpl jvnGetServer() {
		if (js == null) {
			try {
				js = new JvnServerImpl();
			} catch (Exception e) {
				return null;
			}
		}
		return js;
	}

	/**
	* The JVN service is not used anymore
	* @throws JvnException
	**/
	public void jvnTerminate() throws JvnException {
		try {
			coord.jvnTerminate(this);
		} catch (Exception e) {
			throw new JvnException("jvnTerminate failed: " + e.getMessage());
		}
	}

	/**
	* creation of a JVN object
	* @param o : the JVN object state
	* @throws JvnException
	**/
	public JvnObject jvnCreateObject(Serializable o) throws JvnException {
		try {
			int id = coord.jvnGetObjectId();
			// After creation, the app holds a write lock (see Irc comments)
			JvnObjectImpl jo = new JvnObjectImpl(id, o, JvnObjectImpl.LockState.W);
			localObjects.put(id, jo);
			return jo;
		} catch (Exception e) {
			throw new JvnException("jvnCreateObject failed: " + e.getMessage());
		}
	}

	/**
	* Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo : the JVN object 
	* @throws JvnException
	**/
	public void jvnRegisterObject(String jon, JvnObject jo) throws JvnException {
		try {
			localObjects.put(jo.jvnGetObjectId(), jo);
			coord.jvnRegisterObject(jon, jo, this);
		} catch (Exception e) {
			throw new JvnException("jvnRegisterObject failed: " + e.getMessage());
		}
	}

	/**
	* Provide the reference of a JVN object beeing given its symbolic name
	* @param jon : the JVN object name
	* @return the JVN object 
	* @throws JvnException
	**/
	public JvnObject jvnLookupObject(String jon) throws JvnException {
		try {
			JvnObject jo = coord.jvnLookupObject(jon, this);
			if (jo == null) {
				return null;
			}
			// Local proxy for this JVM: same id/state, no lock held here yet
			JvnObjectImpl local = new JvnObjectImpl(
					jo.jvnGetObjectId(),
					jo.jvnGetObjectState(),
					JvnObjectImpl.LockState.NL);
			localObjects.put(local.jvnGetObjectId(), local);
			return local;
		} catch (Exception e) {
			throw new JvnException("jvnLookupObject failed: " + e.getMessage());
		}
	}

	/**
	* Get a Read lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
	public Serializable jvnLockRead(int joi) throws JvnException {
		try {
			return coord.jvnLockRead(joi, this);
		} catch (Exception e) {
			throw new JvnException("jvnLockRead failed: " + e.getMessage());
		}
	}

	/**
	* Get a Write lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
	public Serializable jvnLockWrite(int joi) throws JvnException {
		try {
			return coord.jvnLockWrite(joi, this);
		} catch (Exception e) {
			throw new JvnException("jvnLockWrite failed: " + e.getMessage());
		}
	}

	/**
	* Invalidate the Read lock of the JVN object identified by id 
	* called by the JvnCoord
	* @param joi : the JVN object id
	* @return void
	* @throws java.rmi.RemoteException,JvnException
	**/
	public void jvnInvalidateReader(int joi)
			throws java.rmi.RemoteException, JvnException {
		localObjects.get(joi).jvnInvalidateReader();
	}

	/**
	* Invalidate the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
	public Serializable jvnInvalidateWriter(int joi)
			throws java.rmi.RemoteException, JvnException {
		return localObjects.get(joi).jvnInvalidateWriter();
	}

	/**
	* Reduce the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
	public Serializable jvnInvalidateWriterForReader(int joi)
			throws java.rmi.RemoteException, JvnException {
		return localObjects.get(joi).jvnInvalidateWriterForReader();
	}
}
