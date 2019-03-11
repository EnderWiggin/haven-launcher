/*
 *  This file is part of the Haven Java Launcher.
 *  Copyright (C) 2019 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven.launcher;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class AWTStatus implements Status {
    private final Thread mainthread;
    private final JFrame frame;
    private JPanel imgcont, progcont;
    private JLabel message;
    private Component image;
    private JProgressBar prog;

    {
	try {
	    UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
	} catch(Exception e) {}
    }
	
    public AWTStatus() {
	mainthread = Thread.currentThread();
	frame = new JFrame("Launcher");
	frame.setResizable(false);
	frame.add(imgcont = new JPanel() {{
	    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
	    add(image = Box.createHorizontalStrut(450));
	    add(progcont = new JPanel() {{
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(message = new JLabel("Initializing..."));
		add(Box.createGlue());
	    }});
	}});
	frame.pack();
	SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public void announce(Config cfg) {
	String title = cfg.title;
	if(title != null)
	    SwingUtilities.invokeLater(() -> frame.setTitle(title));
    }

    public void message(String text) {
	SwingUtilities.invokeLater(() -> {
		message.setText(text);
		if(prog != null) {
		    progcont.remove(prog);
		    prog = null;
		}
	    });
    }

    private static final String[] units = {"B", "kB", "MB", "GB", "TB", "PB"};
    private static String fmtbytes(long amount) {
	int ui = 0;
	long sz = 1;
	while((ui < units.length - 1) && ((amount / sz) >= 1000)) {
	    ui++;
	    sz *= 1000;
	}
	return(String.format("%d %s", (amount + (sz / 2)) / sz, units[ui]));
    }

    private long lastsize = -1, lastpos = -1, lastupd = 0;
    public void transfer(long size, long pos) {
	SwingUtilities.invokeLater(() -> {
		long now = System.currentTimeMillis();
		if(size < 0) {
		    if(prog != null) {
			progcont.remove(prog);
			prog = null;
		    }
		} else {
		    if(prog == null) {
			lastsize = lastpos = -1;
			lastupd = 0;
			progcont.add(prog = new JProgressBar());
			prog.setMinimumSize(new Dimension(100, 0));
			prog.setStringPainted(true);
		    }
		    if((now - lastupd > 100) || (pos >= size)) {
			prog.setMaximum((int)size);
			prog.setValue((int)pos);
			prog.setString(String.format("%s / %s", fmtbytes(pos), fmtbytes(size)));
			lastupd = now;
		    }
		}
		lastsize = size;
		lastpos = pos;
	    });
    }

    public void progress() {
    }
}
