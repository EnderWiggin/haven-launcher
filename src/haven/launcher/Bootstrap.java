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

import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

public class Bootstrap {
    public static void bootstrap(OutputStream out, InputStream cfg) throws IOException {
	File jarpath = null;
	if(!(Bootstrap.class.getClassLoader() instanceof URLClassLoader))
	    throw(new RuntimeException("Can only bootstrap via URL classloaders"));
	for(URL url : ((URLClassLoader)Bootstrap.class.getClassLoader()).getURLs()) {
	    if(url.getProtocol().equals("file")) {
		File cpath = new File(url.getFile());
		if(cpath.isFile()) {
		    try(ZipFile jar = new ZipFile(cpath)) {
			if(jar.getEntry("haven/launcher/Bootstrap.class") != null) {
			    jarpath = cpath;
			    break;
			}
		    }
		}
	    }
	}
	if(jarpath == null)
	    throw(new RuntimeException("Could not find launcher Jar on classpath"));
	ZipOutputStream outjar = new ZipOutputStream(out);
	byte[] buf = new byte[65536];
	try(ZipFile injar = new ZipFile(jarpath)) {
	    for(Enumeration<? extends ZipEntry> entries = injar.entries(); entries.hasMoreElements();) {
		ZipEntry ent = entries.nextElement();
		if(ent.getName().equals("haven/launcher/bootstrap.hl")) {
		    System.err.println("launcher: warning: removing current bootstrap");
		} else {
		    outjar.putNextEntry(ent);
		    try(InputStream cont = injar.getInputStream(ent)) {
			for(int rv = cont.read(buf); rv >= 0; rv = cont.read(buf))
			    outjar.write(buf, 0, rv);
		    }
		}
	    }
	}
	outjar.putNextEntry(new ZipEntry("haven/launcher/bootstrap.hl"));
	for(int rv = cfg.read(buf); rv >= 0; rv = cfg.read(buf))
	    outjar.write(buf, 0, rv);
	outjar.finish();
    }

    private static void usage(PrintStream out) {
	out.println("usage: Bootstrap [-h] [BOOT-CONFIG|-] OUTPUT");
    }

    public static void main(String[] args) {
	PosixArgs opt = PosixArgs.getopt(args, "h");
	if(opt == null) {
	    usage(System.err);
	    System.exit(1);
	}
	for(char c : opt.parsed()) {
	    switch(c) {
	    case 'h':
		usage(System.out);
		System.exit(0);
		break;
	    }
	}
	if(opt.rest.length < 2) {
	    usage(System.err);
	    System.exit(1);
	}
	try {
	    InputStream cfg, cl = null;
	    if(opt.rest[0].equals("-")) {
		cfg = System.in;
	    } else {
		cl = cfg = new BufferedInputStream(Files.newInputStream(Utils.path(opt.rest[0])));
	    }
	    try {
		try(OutputStream out = new BufferedOutputStream(Files.newOutputStream(Utils.path(opt.rest[1])));) {
		    bootstrap(out, cfg);
		}
	    } finally {
		if(cl != null)
		    cl.close();
	    }
	} catch(Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}
    }
}
