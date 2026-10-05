package jvn;
import java.io.Serializable;

/**
 * Implementation of a JVN object.
 *
 * The object maintains a local lock state:
 *
 * NL  : No lock
 * RC  : Read lock cached (not currently used)
 * WC  : Write lock cached (not currently used)
 * R   : Read lock taken
 * W   : Write lock taken
 * RWC : Write lock cached + read lock taken
 */
public class JvnObjectImpl implements JvnObject {

    private static final long serialVersionUID = 1L;

    // Possible states of the JVN object
    public enum LockState {
        NL,   // No lock
        RC,   // Read lock cached
        WC,   // Write lock cached
        R,    // Read lock taken
        W,    // Write lock taken
        RWC   // Write lock cached + read lock taken
    }

    // Object identification
    private final int objectId;

    // Actual object state/data
    private Serializable objectState;

    // Current lock state
    private LockState lockState;

    /**
     * Constructor.
     *
     * @param objectId     unique identifier of the JVN object
     * @param objectState  initial state of the object
     * @param lockState    initial lock state
     */
    public JvnObjectImpl(
            int objectId,
            Serializable objectState,
            LockState lockState) {

        this.objectId = objectId;
        this.objectState = objectState;
        this.lockState = lockState;
    }

    /**
     * Update the object state.
     * @param state the new object state
     */
    public synchronized void setObjectState(Serializable state) {
        this.objectState = state;
    }

    /**
     * Get a Read lock on the object 
     * @throws JvnException
     **/
    @Override
    public synchronized void jvnLockRead()
            throws JvnException {

        switch (lockState) {

            case NL:
                objectState = JvnServerImpl.jvnGetServer().jvnLockRead(objectId);
                lockState = LockState.R;
                break;

            case RC:
                // Cached read lock can be taken immediately.
                lockState = LockState.R;
                break;

            case WC:
                // A cached write lock can be used as a read lock.
                lockState = LockState.RWC;
                break;

            case R:
                // Already have the read lock.
                break;

            case W:
                // Already have a write lock, so reading is allowed.
                break;

            case RWC:
                // Already have read access.
                break;

            default:
                throw new JvnException(
                        "Unknown lock state: " + lockState);
        }
    }


    /**
     * Get a Write lock on the object 
     * @throws JvnException
     **/
    @Override
    public synchronized void jvnLockWrite()
            throws JvnException {

        switch (lockState) {

            case NL:
            case RC:
            case R:
                objectState = JvnServerImpl.jvnGetServer().jvnLockWrite(objectId);
                lockState = LockState.W;
                break;

            case WC:
                // Already have a cached write lock.
                lockState = LockState.W;
                break;

            case W:
                // Already have the write lock.
                break;

            case RWC:
                /*
                 * We have a cached write lock and
                 * currently hold the read lock.
                 *
                 * We can take the write lock.
                 */
                lockState = LockState.W;
                break;

            default:
                throw new JvnException(
                        "Unknown lock state: " + lockState);
        }
    }


    /**
     * Unlock the object 
     * @throws JvnException
     **/
    @Override
    public synchronized void jvnUnLock()
            throws JvnException {

        switch (lockState) {

            case R:
                // Read lock is released but can be cached.
                lockState = LockState.RC;
                break;

            case W:
                // Write lock is released and can be cached.
                lockState = LockState.WC;
                break;

            case RWC:
                // Release the read part, keep write lock cached.
                lockState = LockState.WC;
                break;

            case RC:
            case WC:
            case NL:
                /*
                 * Nothing is currently taken.
                 */
                break;

            default:
                throw new JvnException(
                        "Unknown lock state: " + lockState);
        }
        // Wake up a pending invalidation waiting for unlock
        notifyAll();
    }


    /**
     * Get the object identification
     * @throws JvnException
     **/
    @Override
    public synchronized int jvnGetObjectId()
            throws JvnException {

        return objectId;
    }


    /**
     * Get the object state
     * @throws JvnException
     **/
    @Override
    public synchronized Serializable jvnGetObjectState()
            throws JvnException {

        return objectState;
    }


    /**
     * Invalidate the Read lock of the JVN object 
     * @throws JvnException
     **/
    @Override
    public synchronized void jvnInvalidateReader()
            throws JvnException {

        try {
            // Wait until the application releases an active read lock
            while (lockState == LockState.R) {
                wait();
            }
        } catch (InterruptedException e) {
            throw new JvnException("jvnInvalidateReader interrupted");
        }

        if (lockState == LockState.RC) {
            lockState = LockState.NL;
        }
    }


    /**
     * Invalidate the Write lock of the JVN object  
     * @return the current JVN object state
     * @throws JvnException
     **/
    @Override
    public synchronized Serializable jvnInvalidateWriter()
            throws JvnException {

        try {
            while (lockState == LockState.W || lockState == LockState.RWC) {
                wait();
            }
        } catch (InterruptedException e) {
            throw new JvnException("jvnInvalidateWriter interrupted");
        }

        if (lockState != LockState.WC) {
            throw new JvnException(
                    "Cannot invalidate write lock in state: " + lockState);
        }

        Serializable state = objectState;
        lockState = LockState.NL;
        return state;
    }


    /**
     * Reduce the Write lock of the JVN object 
     * @return the current JVN object state
     * @throws JvnException
     **/
    @Override
    public synchronized Serializable jvnInvalidateWriterForReader()
            throws JvnException {

        try {
            while (lockState == LockState.W || lockState == LockState.RWC) {
                wait();
            }
        } catch (InterruptedException e) {
            throw new JvnException("jvnInvalidateWriterForReader interrupted");
        }

        if (lockState != LockState.WC) {
            throw new JvnException(
                    "Cannot reduce write lock in state: " + lockState);
        }

        Serializable state = objectState;
        // Downgrade cached write → cached read
        lockState = LockState.RC;
        return state;
    }
}