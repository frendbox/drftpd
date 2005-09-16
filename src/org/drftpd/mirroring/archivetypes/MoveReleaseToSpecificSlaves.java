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
package org.drftpd.mirroring.archivetypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import net.sf.drftpd.ObjectNotFoundException;
import net.sf.drftpd.mirroring.Job;

import org.apache.log4j.Logger;
import org.drftpd.GlobalContext;
import org.drftpd.PropertyHelper;
import org.drftpd.master.RemoteSlave;
import org.drftpd.mirroring.ArchiveType;
import org.drftpd.plugins.Archive;
import org.drftpd.remotefile.LinkedRemoteFileInterface;
import org.drftpd.sections.SectionInterface;


/**
 * @author zubov
 * @version $Id$
 */
public class MoveReleaseToSpecificSlaves extends ArchiveType {
    private static final Logger logger = Logger.getLogger(MoveReleaseToSpecificSlaves.class);
    private HashSet<RemoteSlave> _destSlaves;
    private int _numOfSlaves;

    public MoveReleaseToSpecificSlaves(Archive archive,
        SectionInterface section, Properties props) {
        super(archive, section, props);
        _destSlaves = new HashSet<RemoteSlave>();

        for (int i = 1;; i++) {
            String slavename = null;

            try {
                slavename = PropertyHelper.getProperty(props,
                        getSection().getName() + ".slavename." + i);
            } catch (NullPointerException e) {
                break; // done
            }

            try {
                _destSlaves.add(GlobalContext.getGlobalContext()
                                       .getSlaveManager().getRemoteSlave(slavename));
            } catch (ObjectNotFoundException e) {
                logger.debug("Unable to get slave " + slavename +
                    " from the SlaveManager");
            }
        }

        if (_destSlaves.isEmpty()) {
            throw new NullPointerException(
                "Cannot continue, 0 destination slaves found for MoveReleaseToSpecificSlave for section " +
                getSection().getName());
        }

        _numOfSlaves = _destSlaves.size();

        if (_numOfSlaves < 1) {
            throw new IllegalArgumentException(
                "numOfSlaves has to be > 0 for section " + section.getName());
        }
    }

    public HashSet findDestinationSlaves() {
        return _destSlaves;
    }

    public void cleanup(ArrayList jobList) {
        for (Iterator iter = jobList.iterator(); iter.hasNext();) {
            Job job = (Job) iter.next();
            job.getFile().deleteOthers(getRSlaves());
        }
    }

    protected boolean isArchivedDir(LinkedRemoteFileInterface lrf)
        throws IncompleteDirectoryException, OfflineSlaveException {
        return isArchivedToXSlaves(lrf, _numOfSlaves);
    }

    public String toString() {
        return "MoveReleaseToSpecificSlaves=[directory=[" +
        getDirectory().getPath() + "]dest=[" + outputSlaves(getRSlaves()) +
        "]numOfSlaves=[" + _numOfSlaves + "]]";
    }
}
