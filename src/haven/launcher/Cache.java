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
import java.nio.channels.*;
import java.util.*;
import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.*;
import java.security.cert.Certificate;

public class Cache {
    private final File base;

    private static File findbase() {
	try {
	    windows: {
		String path = System.getenv("APPDATA");
		if(path == null)
		    break windows;
		File appdata = new File(path);
		if(!appdata.exists() || !appdata.isDirectory() || !appdata.canRead() || !appdata.canWrite())
		    break windows;
		File base = new File(appdata, "Haven Launcher");
		if(!base.exists() && !base.mkdirs())
		    break windows;
		return(base);
	    }
	    fallback: {
		String path = System.getProperty("user.home", null);
		if(path == null)
		    break fallback;
		File home = new File(path);
		if(!home.exists() || !home.isDirectory() || !home.canRead() || !home.canWrite())
		    break fallback;
		File base = new File(new File(home, ".cache"), "haven-launcher");
		if(!base.exists() && !base.mkdirs())
		    break fallback;
		return(base);
	    }
	} catch(SecurityException e) {
	}
	throw(new UnsupportedOperationException("Found no reasonable place to store local files"));
    }

    public Cache() {
	this.base = findbase();
    }

    private static Cache global = null;
    public synchronized static Cache get() {
	if(global == null)
	    global = new Cache();
	return(global);
    }

    private static final String safe =
	"ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
	"abcdefghijklmnopqrstuvwxyz" +
	"0123456789-_. ";
    private static String mangle(String el) {
	StringBuilder buf = new StringBuilder();
	for(int i = 0; i < el.length(); i++) {
	    char c = el.charAt(i);
	    if((safe.indexOf(c) < 0) || ((i == 0) && (c == '.'))) {
		int cc = (int)c;
		if(c < 256)
		    buf.append(String.format("%%%02x", (int)c));
		else
		    buf.append(String.format("%%%04x", (int)c));
	    } else {
		buf.append(c);
	    }
	}
	return(buf.toString());
    }

    public File mangle(URI uri) {
	File ret = new File(base, "cache");
	ret = new File(ret, mangle(uri.getScheme()));
	ret = new File(ret, mangle(uri.getAuthority()));
	String path = uri.getPath();
	int p = 0;
	while(true) {
	    int n = path.indexOf('/', p);
	    if(n < 0)
		n = path.length();
	    if(n > p)
		ret = new File(ret, mangle(path.substring(p, n)));
	    if(n >= path.length())
		break;
	    p = n + 1;
	}
	if(uri.getQuery() != null)
	    ret = new File(ret, mangle(uri.getQuery()));
	return(ret);
    }

    public File metafile(URI uri, String var) {
	File ret = mangle(uri);
	return(new File(ret.getParentFile(), "." + ret.getName() + "." + var));
    }

    private void addcert(Collection<String> buf, Certificate cert) {
	try {
	    buf.add("dig:sha256:" + Utils.byte2hex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded())));
	    PublicKey key = cert.getPublicKey();
	    if(key instanceof RSAPublicKey) {
		RSAPublicKey rsakey = (RSAPublicKey)key;
		MessageDigest dig = MessageDigest.getInstance("SHA-256");
		dig.update(rsakey.getPublicExponent().toString().getBytes(Utils.utf8));
		dig.update(new byte[] {':'});
		dig.update(rsakey.getModulus().toString().getBytes(Utils.utf8));
		buf.add("key:rsa:" + Utils.byte2hex(dig.digest()));
	    }
	    if(key instanceof DSAPublicKey) {
		DSAPublicKey dsakey = (DSAPublicKey)key;
		MessageDigest dig = MessageDigest.getInstance("SHA-256");
		DSAParams dsapar = dsakey.getParams();
		dig.update(dsapar.getG().toString().getBytes(Utils.utf8));
		dig.update(new byte[] {':'});
		dig.update(dsapar.getP().toString().getBytes(Utils.utf8));
		dig.update(new byte[] {':'});
		dig.update(dsapar.getQ().toString().getBytes(Utils.utf8));
		dig.update(new byte[] {':'});
		dig.update(dsakey.getY().toString().getBytes(Utils.utf8));
		buf.add("key:dsa:" + Utils.byte2hex(dig.digest()));
	    }
	} catch(CertificateEncodingException e) {
	} catch(NoSuchAlgorithmException e) {
	    throw(new AssertionError(e));
	}
    }

    public static class FileReplaceException extends IOException implements ErrorMessage {
	public FileReplaceException() {
	    super("could not replace out-of-date file with newly downloaded file");
	}

	public String usermessage() {
	    return("Could not replace out-of-date file with newly downloaded file. " +
		   "If the program is currently running, please quit it and try again.");
	}
    }

    private static final SslHelper ssl = new SslHelper();
    private Cached update0(URI uri, boolean force) throws IOException {
	try(Status st = Status.local()) {
	    st.messagef("Checking %s...", Utils.basename(uri));
	    File path = mangle(uri);
	    File infop = metafile(uri, "info");
	    File newp = metafile(uri, "new");
	    File dir = path.getParentFile();
	    if(!dir.isDirectory() && !dir.mkdirs())
		throw(new IOException("could not create " + dir));
	    RandomAccessFile fp = new RandomAccessFile(infop, "rw");
	    Properties props = new Properties();
	    Properties nprops = new Properties();
	    nprops.put("source", uri.toString());
	    try(FileLock lk = fp.getChannel().lock()) {
		fp.seek(0);
		props.load(new BufferedReader(new InputStreamReader(new RandInputStream(fp), Utils.utf8)));
		/* Set up connection parameters */
		URL url = uri.toURL();
		URLConnection conn = null;
		if(conn == null)
		    conn = ssl.connect(url);
		if(conn == null)
		    conn = uri.toURL().openConnection();
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(5000);
		HttpURLConnection http = (conn instanceof HttpURLConnection) ? ((HttpURLConnection)conn) : null;
		conn.addRequestProperty("User-Agent", String.format("Haven-Launcher/%d.%d", Config.MAJOR_VERSION, Config.MINOR_VERSION));
		if(http != null) {
		    http.setUseCaches(false);
		    if(!force && props.containsKey("mtime"))
			http.setRequestProperty("If-Modified-Since", (String)props.get("mtime"));
		}
		conn.connect();
		/* Inspect connection state */
		if(conn instanceof HttpsURLConnection) {
		    Collection<String> certinfo = new ArrayList<>();
		    for(Certificate cert : ((HttpsURLConnection)conn).getServerCertificates())
			addcert(certinfo, cert);
		    nprops.put("tls-certs", String.join(" ", certinfo));
		}
		long bytes = 0, expected = -1;
		try(InputStream in = conn.getInputStream()) {
		    if(http != null) {
			expected = http.getContentLengthLong();
			if(!force && (http.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED)) {
			    return(new Cached(path, props, false));
			}
			if(http.getResponseCode() != HttpURLConnection.HTTP_OK)
			    throw(new IOException("Unexpected HTTP response code: " + http.getResponseCode()));
		    }
		    /* Fetch file */
		    st.messagef("Fetching %s...", Utils.basename(uri));
		    st.transfer(expected, 0);
		    byte[] buf = new byte[65536];
		    try(OutputStream out = new FileOutputStream(newp)) {
			for(int rv = in.read(buf); rv >= 0; rv = in.read(buf)) {
			    out.write(buf, 0, rv);
			    bytes += rv;
			    st.transfer(expected, bytes);
			}
		    }
		}
		/* Check completion parameters */
		if(http != null) {
		    long clen = http.getContentLengthLong();
		    /* Because, apparently, Java doesn't make this check itself. */
		    if(clen != bytes)
			throw(new IOException("Premature EOF"));
		    String mtime = http.getHeaderField("Last-Modified");
		    if(mtime != null)
			nprops.put("mtime", mtime);
		}
		String ctype = conn.getContentType();
		if(ctype != null)
		    nprops.put("ctype", ctype);
		if(ctype.equals("application/java-archive")) {
		    st.messagef("Verifying %s...", Utils.basename(uri));
		    Collection<String> certinfo = new ArrayList<>();
		    for(Certificate cert : Utils.checkjar(newp))
			addcert(certinfo, cert);
		    if(!certinfo.isEmpty())
			nprops.put("jar-certs", String.join(" ", certinfo));
		}
		/* Commit file */
		fp.seek(0); fp.setLength(0);
		if(!newp.renameTo(path)) {
		    /* XXX: Arguably, use java.nio.file instead. */
		    path.delete();
		    if(!newp.renameTo(path))
			throw(new FileReplaceException());
		}
		Writer propout = new BufferedWriter(new OutputStreamWriter(new RandOutputStream(fp), Utils.utf8));
		nprops.store(propout, null);
		propout.flush();
		return(new Cached(path, nprops, true));
	    }
	}
    }

    public Cached update(URI uri, boolean force) throws IOException {
	List<IOException> errors = new ArrayList<>();
	for(int retry = 0; retry < 3; retry++) {
	    try {
		return(update0(uri, force));
	    } catch(IOException e) {
		errors.add(e);
	    }
	    force = true;
	}
	IOException first = errors.get(0);
	for(int i = 1; i < errors.size(); i++)
	    first.addSuppressed(errors.get(i));
	throw(first);
    }
}
