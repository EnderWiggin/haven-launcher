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
import java.util.jar.*;
import java.io.*;
import java.net.*;
import java.security.cert.*;

public class Utils {
    public static final java.nio.charset.Charset utf8 = java.nio.charset.Charset.forName("UTF-8");

    public static <E> E pop(Collection<E> c) {
	Iterator<E> i = c.iterator();
	E ret = i.next();
	i.remove();
	return(ret);
    }

    public static char num2hex(int num) {
	if(num < 10)
	    return((char)('0' + num));
	else
	    return((char)('A' + num - 10));
    }

    public static String byte2hex(byte[] in) {
	StringBuilder buf = new StringBuilder();
	for(byte b : in) {
	    buf.append(num2hex((b & 0xf0) >> 4));
	    buf.append(num2hex(b & 0x0f));
	}
	return(buf.toString());
    }

    public static String basename(URI uri) {
	String path = uri.getPath();
	int p = path.lastIndexOf('/');
	if(p < 0)
	    return(path);
	return(path.substring(p + 1));
    }

    public static String[] splitwords(String text) {
	ArrayList<String> words = new ArrayList<String>();
	StringBuilder buf = new StringBuilder();
	String st = "ws";
	int i = 0;
	while(i < text.length()) {
	    char c = text.charAt(i);
	    if(st == "ws") {
		if(!Character.isWhitespace(c))
		    st = "word";
		else
		    i++;
	    } else if(st == "word") {
		if(c == '"') {
		    st = "quote";
		    i++;
		} else if(c == '\\') {
		    st = "squote";
		    i++;
		} else if(Character.isWhitespace(c)) {
		    words.add(buf.toString());
		    buf = new StringBuilder();
		    st = "ws";
		} else {
		    buf.append(c);
		    i++;
		}
	    } else if(st == "quote") {
		if(c == '"') {
		    st = "word";
		    i++;
		} else if(c == '\\') {
		    st = "sqquote";
		    i++;
		} else {
		    buf.append(c);
		    i++;
		}
	    } else if(st == "squote") {
		buf.append(c);
		i++;
		st = "word";
	    } else if(st == "sqquote") {
		buf.append(c);
		i++;
		st = "quote";
	    }
	}
	if(st == "word")
	    words.add(buf.toString());
	if((st != "ws") && (st != "word"))
	    return(null);
	return(words.toArray(new String[0]));
    }

    public static Certificate[] checkjar(File path) throws IOException {
	Set<Certificate> ret = null;
	try(JarFile jar = new JarFile(path)) {
	    if(jar.getManifest() == null)
		return(new Certificate[0]);
	    byte[] buf = new byte[65536];
	    for(Enumeration<JarEntry> i = jar.entries(); i.hasMoreElements();) {
		JarEntry ent = i.nextElement();
		if(ent.isDirectory())
		    continue;
		try(InputStream st = jar.getInputStream(ent)) {
		    while(st.read(buf, 0, buf.length) >= 0);
		}
		if(ent.getName().startsWith("META-INF"))
		    continue;
		Certificate[] entc = ent.getCertificates();
		if((entc == null) || (entc.length < 1))
		    return(new Certificate[0]);
		if(ret == null) {
		    ret = new HashSet<>(Arrays.asList(entc));
		} else {
		    ret.retainAll(Arrays.asList(entc));
		    if(ret.size() < 1)
			return(new Certificate[0]);
		}
	    }
	}
	if(ret == null)
	    return(new Certificate[0]);
	return(ret.toArray(new Certificate[0]));
    }

    public static File findjvm() {
	File jvm, javadir = new File(new File(System.getProperty("java.home")), "bin");
	if((jvm = new File(javadir, "java")).exists())
	    return(jvm);
	if((jvm = new File(javadir, "javaw.exe")).exists())
	    return(jvm);
	if((jvm = new File(javadir, "java.exe")).exists())
	    return(jvm);
	throw(new RuntimeException("could not find a Java executable"));
    }
}
