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
import java.io.*;
import java.net.*;

public class Config {
    public static final int MAJOR_VERSION = 1;
    public static final int MINOR_VERSION = 1;
    public final Collection<Resource> classpath = new ArrayList<>();
    public final Collection<Resource> include = new ArrayList<>();
    public final Collection<URI> included = new HashSet<>();
    public final Collection<NativeLib> libraries = new ArrayList<>();
    public final Map<String, String> sysprops = new HashMap<>();
    public Resource chain = null;
    public String mainclass = null;
    public Resource execjar = null;
    public String title = null;
    public Resource splashimg = null, icon = null;
    public int heapsize = 0;

    public static class Environment {
	public static final URI opaque = URI.create("urn:nothing");
	public Collection<Validator> val = Collections.emptyList();
	public URI rel = opaque;

	public Environment val(Collection<Validator> val) {this.val = val; return(this);}
	public Environment rel(URI rel) {this.rel = rel; return(this);}

	public static Environment from(Resource res) {
	    return(new Environment().val(res.val).rel(res.uri));
	}
    }

    public void read(Reader in, Environment env) throws IOException {
	BufferedReader fp = new BufferedReader(in);
	for(String ln = fp.readLine(); ln != null; ln = fp.readLine()) {
	    if((ln.length() > 0) && (ln.charAt(0) == '#'))
		continue;
	    String[] words = Utils.splitwords(ln);
	    if((words == null) || (words.length < 1))
		continue;
	    switch(words[0]) {
	    case "require": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: require MAJOR.MINOR"));
		int maj, min;
		try {
		    int p = words[1].indexOf('.');
		    if(p < 0)
			throw(new RuntimeException("usage: require MAJOR.MINOR"));
		    maj = Integer.parseInt(words[1].substring(0, p));
		    min = Integer.parseInt(words[1].substring(p + 1));
		} catch(NumberFormatException e) {
		    throw(new RuntimeException("usage: require MAJOR.MINOR", e));
		}
		if((maj != MAJOR_VERSION) || (min > MINOR_VERSION))
		    throw(new RuntimeException(String.format("invalid version of launcher; launch file requires %d.%d, this is %d.%d",
							     maj, min, MAJOR_VERSION, MINOR_VERSION)));
		break;
	    }
	    case "rel": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: rel URI"));
		try {
		    env.rel(new URI(words[1]));
		} catch(URISyntaxException e) {
		    throw(new RuntimeException("usage: rel URL", e));
		}
		break;
	    }
	    case "validate": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: validate VALIDATOR..."));
		Collection<Validator> nval = new ArrayList<>();
		for(int i = 1; i < words.length; i++) {
		    Validator v = Validator.parse(words[i]);
		    if(v != null)
			nval.add(v);
		}
		env.val = nval;
		break;
	    }
	    case "title": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: title TITLE"));
		title = words[1];
		break;
	    }
	    case "splash-image": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: splash-image URL"));
		try {
		    splashimg = new Resource(env.rel.resolve(new URI(words[1])), env.val);
		} catch(URISyntaxException e) {
		    throw(new RuntimeException("usage: splash-image URL", e));
		}
		break;
	    }
	    case "icon": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: icon URL"));
		try {
		    icon = new Resource(env.rel.resolve(new URI(words[1])), env.val);
		} catch(URISyntaxException e) {
		    throw(new RuntimeException("usage: icon URL", e));
		}
		break;
	    }
	    case "chain": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: chain URL"));
		try {
		    chain = new Resource(env.rel.resolve(new URI(words[1])), env.val);
		} catch(URISyntaxException e) {
		    throw(new RuntimeException("usage: chain URL", e));
		}
		break;
	    }
	    case "main-class": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: main-class CLASS-NAME"));
		mainclass = words[1];
		break;
	    }
	    case "exec-jar": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: exec-jar URL"));
		try {
		    execjar = new Resource(env.rel.resolve(new URI(words[1])), env.val);
		} catch(URISyntaxException e) {
		    throw(new RuntimeException("usage: exec-jar URL", e));
		}
		break;
	    }
	    case "include": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: include URL"));
		try {
		    include.add(new Resource(env.rel.resolve(new URI(words[1])), env.val));
		} catch(URISyntaxException e) {
		    throw(new RuntimeException("usage: include URL", e));
		}
		break;
	    }
	    case "class-path": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: classpath URL"));
		try {
		    classpath.add(new Resource(env.rel.resolve(new URI(words[1])), env.val));
		} catch(URISyntaxException e) {
		    throw(new RuntimeException("usage: classpath URL", e));
		}
		break;
	    }
	    case "property": {
		if(words.length < 3)
		    throw(new RuntimeException("usage: property NAME VALUE"));
		sysprops.put(words[1], words[2]);
		break;
	    }
	    case "heap-size": {
		if(words.length < 2)
		    throw(new RuntimeException("usage: heap-size MBYTES"));
		try {
		    heapsize = Integer.parseInt(words[1]);
		} catch(NumberFormatException e) {
		    throw(new RuntimeException("usage: heap-size MBYTES", e));
		}
		break;
	    }
	    case "native-lib": {
		if(words.length < 4)
		    throw(new RuntimeException("usage: native-lib OS ARCH URL"));
		try {
		    Pattern os = Pattern.compile(words[1], Pattern.CASE_INSENSITIVE);
		    Pattern arch = Pattern.compile(words[2], Pattern.CASE_INSENSITIVE);
		    Resource lib = new Resource(env.rel.resolve(new URI(words[3])), env.val);
		    libraries.add(new NativeLib(os, arch, lib));
		} catch(PatternSyntaxException | URISyntaxException e) {
		    throw(new RuntimeException("usage: native-lib OS ARCH URL", e));
		}
		break;
	    }
	    }
	}
    }
}
