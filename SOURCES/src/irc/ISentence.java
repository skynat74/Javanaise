package irc;

import jvn.Read;
import jvn.Write;

/**
 * Shared chat sentence API (used by the dynamic proxy).
 */
public interface ISentence {

	@Read
	String read();

	@Write
	void write(String text);
}
