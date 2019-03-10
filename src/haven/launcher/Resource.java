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
import java.io.*;
import java.net.*;

public class Resource {
    public final URI uri;
    public final Collection<Validator> val;

    public Resource(URI uri, Collection<Validator> val) {
	this.uri = uri;
	this.val = val;
    }

    private void validate(Cached cf) throws ValidationException {
	if(!this.val.isEmpty()) {
	    Collection<ValidationException> errors = new ArrayList<>();
	    validate: {
		for(Validator val : this.val) {
		    try {
			val.validate(cf);
			break validate;
		    } catch(ValidationException e) {
			errors.add(e);
		    }
		}
		ValidationException e = new ValidationException("Could not validate " + uri);
		for(ValidationException err : errors)
		    e.addSuppressed(err);
		throw(e);
	    }
	}
    }

    public File update() throws IOException {
	Cache cache = Cache.get();
	Cached cf = cache.update(uri, false);
	try {
	    validate(cf);
	} catch(ValidationException e) {
	    if(cf.fresh)
		throw(e);
	    cf = cache.update(uri, true);
	    validate(cf);
	}
	return(cf.path);
    }
}
