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

import java.util.*;
import java.util.regex.*;
import java.util.jar.*;
import java.io.*;
import java.net.*;

public class NativeLib {
    public final Pattern os, arch;
    public final Resource jar;

    public NativeLib(Pattern os, Pattern arch, Resource jar) {
	this.os = os;
	this.arch = arch;
	this.jar = jar;
    }

    public boolean use() {
	return(os.matcher(System.getProperty("os.name")).matches() && arch.matcher(System.getProperty("os.arch")).matches());
    }

    public File extract() throws IOException {
	File jar = this.jar.update();
	File dir = this.jar.metafile("lib");
	boolean fresh = false;
	if(!dir.isDirectory()) {
	    fresh = true;
	    if(!dir.mkdirs())
		throw(new IOException("could not create " + dir));
	}
	if(fresh || (jar.lastModified() > dir.lastModified())) {
	    JarFile fp = new JarFile(jar);
	    for(Enumeration<JarEntry> i = fp.entries(); i.hasMoreElements();) {
		JarEntry ent = i.nextElement();
		if(ent.isDirectory())
		    continue;
		if((ent.getName().indexOf('/') >= 0) || (ent.getName().charAt(0) == '.'))
		    continue;
		try(InputStream in = fp.getInputStream(ent)) {
		    try(OutputStream out = new FileOutputStream(new File(dir, ent.getName()))) {
			byte[] buf = new byte[65536];
			int rv;
			while((rv = in.read(buf)) >= 0)
			    out.write(buf, 0, rv);
		    }
		}
	    }
	}
	return(dir);
    }
}
