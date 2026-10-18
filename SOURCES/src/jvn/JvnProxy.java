package jvn;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static java.lang.System.out;
/**
 * Dynamic proxy: intercepts method calls on a shared object,
 * takes the right JVN lock from @Read / @Write, then unlocks.
 */
public class JvnProxy implements InvocationHandler {

	private final JvnObject jvnObject;

	private JvnProxy(JvnObject jvnObject) {
		this.jvnObject = jvnObject;
	}

	/**
	 * Create a proxy around an existing JvnObject.
	 */
	public static Object newInstance(JvnObject jo) throws JvnException {
		Serializable obj = jo.jvnGetObjectState();
		System.out.println("object state: " + obj);
		return Proxy.newProxyInstance(
				obj.getClass().getClassLoader(),
				obj.getClass().getInterfaces(),
				new JvnProxy(jo));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {

		boolean read = method.isAnnotationPresent(Read.class);
		boolean write = method.isAnnotationPresent(Write.class);

		if (write) {
			jvnObject.jvnLockWrite();
		} else if (read) {
			jvnObject.jvnLockRead();
		}

		try {
			return method.invoke(jvnObject.jvnGetObjectState(), args);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		} finally {
			if (read || write) {
				jvnObject.jvnUnLock();
			}
		}
	}
}
