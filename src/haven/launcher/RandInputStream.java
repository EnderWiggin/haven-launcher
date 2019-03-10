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

public class RandInputStream extends InputStream {
    private final RandomAccessFile bk;

    RandInputStream(RandomAccessFile bk) {
	this.bk = bk;
    }

    public int read() throws IOException {
	return(bk.read());
    }

    public int read(byte[] buf, int off, int len) throws IOException {
	return(bk.read(buf, off, len));
    }

    public void close() throws IOException {
	bk.close();
    }

    public int available() throws IOException {
	return((int)Math.min(bk.length() - bk.getFilePointer(), Integer.MAX_VALUE));
    }

    public long skip(long n) throws IOException {
	long p = bk.getFilePointer();
	n = Math.min(n, bk.length() - p);
	bk.seek(p + n);
	return(n);
    }
}
