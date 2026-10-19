/***
 * Irc class : simple implementation of a chat using JAVANAISE
 * Contact: 
 *
 * Authors: 
 */

package irc;

import java.awt.*;
import java.awt.event.*; 


import jvn.*;
import java.io.*;


public class Irc {
	public TextArea		text;
	public TextField	data;
	Frame 			frame;
	ISentence       proxy;	// created once, reused on each read/write


  /**
  * main method
  * create a JVN object nammed IRC for representing the Chat application
  **/
	public static void main(String argv[]) {
	   try {
		   
		// initialize JVN
		JvnServerImpl js = JvnServerImpl.jvnGetServer();
		
		// look up the IRC object in the JVN server
		// if not found, create it, and register it in the JVN server
		JvnObject jo = js.jvnLookupObject("IRC");
		   
		if (jo == null) {
			jo = js.jvnCreateObject((Serializable) new Sentence());
			// after creation, I have a write lock on the object
			jo.jvnUnLock();
			js.jvnRegisterObject("IRC", jo);
		}
		// create the graphical part of the Chat application
		 new Irc(jo);
	   
	   } catch (Exception e) {
		   System.out.println("IRC problem : " + e.getMessage());
	   }
	}

  /**
   * IRC Constructor
   @param sentence the JVN object representing the Chat
   **/
	public Irc(JvnObject sentence) throws JvnException {
		proxy = (ISentence) JvnProxy.newInstance(sentence);
		frame=new Frame();
		frame.setLayout(new GridLayout(1,1));
		text=new TextArea(10,60);
		text.setEditable(false);
		text.setForeground(Color.red);
		frame.add(text);
		data=new TextField(40);
		frame.add(data);
		Button read_button = new Button("read");
		read_button.addActionListener(new readListener(this));
		frame.add(read_button);
		Button write_button = new Button("write");
		write_button.addActionListener(new writeListener(this));
		frame.add(write_button);
		frame.setSize(545,201);
		text.setBackground(Color.black); 
		frame.setVisible(true);
	}
}


 /**
  * Internal class to manage user events (read) on the CHAT application
  **/
 class readListener implements ActionListener {
	Irc irc;
  
	public readListener (Irc i) {
		irc = i;
	}
   
 /**
  * Management of user events
  **/
	public void actionPerformed (ActionEvent e) {
	 try {
		// @Read → proxy takes the read lock
		String s = irc.proxy.read();
		
		// display the read value
		irc.data.setText(s);
		irc.text.append(s+"\n");
	   } catch (Exception ex) {
		   System.out.println("IRC problem : " + ex.getMessage());
	   }
	}
}

 /**
  * Internal class to manage user events (write) on the CHAT application
  **/
 class writeListener implements ActionListener {
	Irc irc;
  
	public writeListener (Irc i) {
        	irc = i;
	}
  
  /**
    * Management of user events
   **/
	public void actionPerformed (ActionEvent e) {
	   try {	
		// get the value to be written from the buffer
		String s = irc.data.getText();

		// @Write → proxy takes the write lock
		irc.proxy.write(s);
	 } catch (Exception ex) {
		   System.out.println("IRC problem  : " + ex.getMessage());
	 }
	}
}
