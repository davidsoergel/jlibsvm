package edu.berkeley.compbio.jlibsvm.legacyexec;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;


class AppletFrame extends Frame {
	AppletFrame(String title, Applet applet, int width, int height)
	{
		super(title);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		applet.init();
		applet.setSize(width,height);
		applet.start();
		this.add(applet);
		this.pack();
		this.setVisible(true);
	}
}