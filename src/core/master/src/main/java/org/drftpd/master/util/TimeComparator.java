/*
 * This file is part of DrFTPD, Distributed FTP Daemon.
 *
 * DrFTPD is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * DrFTPD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DrFTPD; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.drftpd.master.util;

import org.drftpd.master.vfs.InodeHandle;

import java.io.FileNotFoundException;
import java.util.Comparator;

/**
 * @author zubov
 * @version $Id$
 */
public class TimeComparator implements Comparator<InodeHandle> {

    /**
     * Sorts FileSystem objects by their date modified
     */
    public TimeComparator() {

    }

    public int compare(InodeHandle arg0, InodeHandle arg1) {
        if (arg0 == null || arg1 == null) {
            throw new IllegalArgumentException(
                    "Neither arg0 nor arg1 can be null");
        }
        try {
            return Long.signum(arg1.lastModified() - arg0.lastModified());
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File does not exist", e);
        }
    }

}
