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
import java.nio.*;
import java.nio.channels.*;

public class RandInputStream extends InputStream {
    private final ReadableByteChannel bk;
    private final SeekableByteChannel sk;

    RandInputStream(ReadableByteChannel bk) {
	this.bk = bk;
	sk = (bk instanceof SeekableByteChannel) ? (SeekableByteChannel)bk : null;
    }

    public int read() throws IOException {
	byte[] buf = new byte[1];
	while(true) {
	    int rv = read(buf, 0, 1);
	    if(rv > 0)
		return(buf[0]);
	    if(rv < 0)
		return(-1);
	}
    }

    public int read(byte[] buf, int off, int len) throws IOException {
	return(bk.read(ByteBuffer.wrap(buf, off, len)));
    }

    public void close() throws IOException {
	bk.close();
    }

    public int available() throws IOException {
	if(sk == null)
	    return(0);
	return((int)Math.min(sk.size() - sk.position(), Integer.MAX_VALUE));
    }

    public long skip(long n) throws IOException {
	if(sk == null)
	    return(Math.max(read(new byte[(int)Math.min(n, 1 << 20)]), 0));
	long p = sk.position();
	n = Math.min(n, sk.size() - p);
	sk.position(p + n);
	return(n);
    }
}
