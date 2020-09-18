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
import java.net.*;
import java.util.*;

public class Driver {
    public static void execute(Config cfg) {
	try {
	    if(cfg.chain != null) {
		Config chained = new Config();
		try(InputStream src = new FileInputStream(cfg.chain.update())) {
		    chained.read(new InputStreamReader(src, Utils.utf8), Config.Environment.from(cfg.chain));
		}
		run(chained);
	    } else if((cfg.mainclass != null) || (cfg.execjar != null)) {
		List<String> args = new ArrayList<>();
		args.add(Utils.findjvm().toString());
		Collection<File> classpath = new ArrayList<>();
		for(Resource res : cfg.classpath) {
		    classpath.add(res.update());
		}
		if(cfg.heapsize > 0)
		    args.add(String.format("-Xmx%dm", cfg.heapsize));
		for(Map.Entry<String, String> prop : cfg.sysprops.entrySet())
		    args.add(String.format("-D%s=%s", prop.getKey(), prop.getValue()));
		if(!classpath.isEmpty()) {
		    args.add("-classpath");
		    args.add(String.join(File.pathSeparator, (Iterable<String>)classpath.stream().map(File::toString)::iterator));
		}

		{
		    Collection<String> libdirs = new ArrayList<>();
		    for(NativeLib lib : cfg.libraries) {
			if(lib.use())
			    libdirs.add(lib.extract().toString());
		    }
		    if(libdirs.size() > 0) {
			String dirs = String.join(File.pathSeparator, libdirs);
			String cur = System.getProperty("java.library.path");
			if((cur != null) && (cur.length() > 0))
			    dirs = dirs + File.pathSeparator + cur;
			args.add(String.format("-Djava.library.path=%s", dirs));
		    }
		}

		if(cfg.mainclass != null) {
		    args.add(cfg.mainclass);
		} else {
		    args.add("-jar");
		    args.add(cfg.execjar.update().toString());
		}
		Status.current().message("Launching...");
		ProcessBuilder spec = new ProcessBuilder(args);
		spec.inheritIO();
		spec.start();
	    } else {
		throw(new RuntimeException("no execution data in configuration"));
	    }
	} catch(IOException e) {
	    /* XXX */
	    throw(new RuntimeException(e));
	}
    }

    public static void run(Config cfg) {
	Status.current().announce(cfg);
	try {
	    while(!cfg.include.isEmpty()) {
		Resource res = Utils.pop(cfg.include);
		if(cfg.included.contains(res.uri))
		    continue;
		cfg.included.add(res.uri);
		File path = res.update();
		try(InputStream src = new FileInputStream(path)) {
		    cfg.read(new InputStreamReader(src, Utils.utf8), Config.Environment.from(res));
		}
	    }
	    execute(cfg);
	} catch(IOException e) {
	    /* XXX */
	    throw(new RuntimeException(e));
	}
    }

    private static void usage(PrintStream out) {
	out.println("usage: launcher.jar [-h] [CONFIG-URL|FILE]");
    }

    public static void main(String[] args) {
	Status.use(new AWTStatus());
	try {
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
	    Config cfg = new Config();
	    if(opt.rest.length > 0) {
		try {
		    if(opt.rest[0].indexOf("://") < 0) {
			try(InputStream src = new FileInputStream(opt.rest[0])) {
			    cfg.read(new InputStreamReader(src, Utils.utf8), new Config.Environment());
			}
		    } else {
			URI uri;
			try {
			    uri = new URI(opt.rest[0]);
			} catch(URISyntaxException e) {
			    System.err.printf("launcher: invalid url: %s\n", opt.rest[0]);
			    System.exit(1); return;
			}
			Resource res = new Resource(uri, Collections.emptyList());
			try(InputStream src = new FileInputStream(res.update())) {
			    cfg.read(new InputStreamReader(src, Utils.utf8), Config.Environment.from(res));
			}
		    }
		} catch(IOException e) {
		    System.err.printf("launcher: could not read %s: %s\n", opt.rest[0], e);
		    System.exit(1); return;
		}
	    } else {
		InputStream src = Driver.class.getResourceAsStream("bootstrap.hl");
		if(src == null) {
		    System.err.println("launcher: no bootstreap config found\n");
		    usage(System.err);
		    System.exit(1);
		}
		try {
		    try {
			cfg.read(new InputStreamReader(src, Utils.utf8), new Config.Environment());
		    } finally {
			src.close();
		    }
		} catch(IOException e) {
		    throw(new AssertionError(e));
		}
	    }
	    run(cfg);
	} catch(Throwable t) {
	    Status.current().error(t);
	}
	System.exit(0);
    }
}
