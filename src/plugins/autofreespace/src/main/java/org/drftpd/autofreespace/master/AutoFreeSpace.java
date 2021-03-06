/*
 * This file is part of DrFTPD, Distributed FTP Daemon.
 *
 * DrFTPD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * DrFTPD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DrFTPD; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.drftpd.autofreespace.master;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.drftpd.autofreespace.master.event.AFSEvent;
import org.drftpd.common.extensibility.PluginInterface;
import org.drftpd.common.util.Bytes;
import org.drftpd.common.util.ConfigLoader;
import org.drftpd.common.util.PropertyHelper;
import org.drftpd.master.GlobalContext;
import org.drftpd.master.event.ReloadEvent;
import org.drftpd.master.exceptions.NoAvailableSlaveException;
import org.drftpd.master.exceptions.SlaveUnavailableException;
import org.drftpd.master.sections.SectionInterface;
import org.drftpd.master.slavemanagement.RemoteSlave;
import org.drftpd.master.vfs.DirectoryHandle;
import org.drftpd.master.vfs.FileHandle;
import org.drftpd.master.vfs.InodeHandle;

import java.io.FileNotFoundException;
import java.util.*;


/**
 * @author Teflon
 * @author Stevezau
 * @author scitz0
 * @version $Id$
 * adapted to drftpd 3.0.0 by Stevezau
 * @inspired by P
 */

public class AutoFreeSpace implements PluginInterface {
    private static final Logger logger = LogManager.getLogger(AutoFreeSpace.class);
    private HashMap<String, Section> sections;
    private ArrayList<String> _excludeFiles;
    private ArrayList<String> _excludeSlaves;
    private Timer _timer;
    private boolean _onlyAnnounce;
    private boolean deleteOnDate = false;
    private boolean deleteOnSpace = false;

    public void startPlugin() {
        _excludeFiles = new ArrayList<>();
        _excludeSlaves = new ArrayList<>();
        sections = new HashMap<>();
        reload();
        // Subscribe to events
        AnnotationProcessor.process(this);
        logger.info("Autofreespace plugin loaded successfully");
    }

    public void stopPlugin(String reason) {
        AnnotationProcessor.unprocess(this);
        _timer.cancel();
        logger.info("Autofreespace plugin unloaded successfully");
    }

    @EventSubscriber
    public void onReloadEvent(ReloadEvent event) {
        logger.info("Received reload event, reloading");
        reload();
    }

    private void reload() {
        logger.info("AUTODELETE: Reloading {}", AutoFreeSpace.class.getName());
        Properties p = ConfigLoader.loadPluginConfig("autofreespace.conf");
        if (_timer == null) {
            _timer = new Timer();
        } else {
            _timer.cancel();
            _timer = new Timer();
        }

        _excludeFiles.clear();
        _excludeSlaves.clear();

        int id = 1;
        String name;
        long minFreeSpace = Bytes.parseBytes(p.getProperty("keepFree"));
        long cycleTime = Long.parseLong(p.getProperty("cycleTime")) * 60000;
        deleteOnDate = Boolean.parseBoolean(p.getProperty("delete.on.date"));
        deleteOnSpace = Boolean.parseBoolean(p.getProperty("delete.on.space"));
        _excludeSlaves.addAll(Arrays.asList(p.getProperty("excluded.slaves", "").trim().split("\\s")));
        long wipeAfter;

        while ((name = PropertyHelper.getProperty(p, id + ".section", null)) != null) {
            wipeAfter = Long.parseLong(p.getProperty(id + ".wipeAfter")) * 60000;
            sections.put(name, new Section(id, name, wipeAfter));
            id++;
        }

        for (int i = 1; ; i++) {
            String sec = p.getProperty("excluded.file." + i);
            if (sec == null)
                break;
            _excludeFiles.add(sec);
        }
        _excludeFiles.trimToSize();

        _onlyAnnounce = p.getProperty("announce.only", "false").equalsIgnoreCase("true");

        _timer.schedule(new MrCleanit(_excludeFiles, minFreeSpace, sections), cycleTime, cycleTime);
    }

    static class Section {
        private final int id;
        private final String name;
        private final long wipeAfter;

        public Section(int id, String name, long wipeAfter) {
            this.id = id;
            this.name = name;
            this.wipeAfter = wipeAfter;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public long getWipeAfter() {
            return this.wipeAfter;
        }
    }

    private class MrCleanit extends TimerTask {
        private final ArrayList<String> _excludeFiles;
        private final HashMap<String, Section> _sections;
        private final long _minFreeSpace;
        private final ArrayList<String> checkedReleases = new ArrayList<>();

        public MrCleanit(ArrayList<String> excludeFiles, long minFreeSpace, HashMap<String, Section> sections) {
            _excludeFiles = excludeFiles;
            _minFreeSpace = minFreeSpace;
            _sections = sections;
        }

        private boolean checkInvalidName(String name) {
            for (String regex : _excludeFiles) {
                if (name.matches(regex)) {
                    return true;
                }
            }
            return false;
        }

        private InodeHandle getOldestFile(DirectoryHandle dir, RemoteSlave slave) throws FileNotFoundException {

            Collection<InodeHandle> collection = dir.getInodeHandlesUnchecked();

            if (collection.isEmpty()) {
                logger.debug("AUTODELETE: Empty section: {}, skipping", dir.getName());
                return null; //empty section, just ignore
            }

            TreeSet<InodeHandle> sortedCollection = new TreeSet<>(new AgeComparator());
            sortedCollection.addAll(collection);

            for (InodeHandle inode : sortedCollection) {
                if (checkInvalidName(inode.getName())) {
                    continue;
                }
                try {
                    if (gotFilesOn(inode, slave)) {
                        return inode;
                    }
                } catch (NoAvailableSlaveException e) {
                    logger.warn("AUTODELETE: No slave available", e.getCause());
                }
            }
            logger.debug("AUTODELETE: Could not find a valid release to delete in section {}", dir.getName());
            return null;
        }

        private boolean gotFilesOn(InodeHandle inode, RemoteSlave slave)
                throws NoAvailableSlaveException, FileNotFoundException {

            if (inode.isFile()) {
                return ((FileHandle) inode).getAvailableSlaves().contains(slave);
            } else if (inode.isDirectory()) {
                for (FileHandle file : ((DirectoryHandle) inode).getAllFilesRecursiveUnchecked()) {
                    if (file.getAvailableSlaves().contains(slave)) {
                        return true;
                    }
                }
            }

            return false;
        }

        private InodeHandle getOldestRelease(RemoteSlave slave) throws FileNotFoundException {

            InodeHandle oldest = null;

            for (SectionInterface si :
                    GlobalContext.getGlobalContext().getSectionManager().getSections()) {

                if (!_sections.containsKey(si.getName())) {
                    continue;
                }

                logger.debug("AUTODELETE: Getting oldest release in section {}", si.getName());
                try {
                    InodeHandle file = getOldestFile(si.getBaseDirectory(), slave);

                    if (file == null)
                        continue;

                    logger.debug("AUTODELETE: Oldest file in section {}: {}", si.getName(), file.getName());

                    long age = System.currentTimeMillis() - file.creationTime();
                    Section section = _sections.get(si.getName());
                    long _archiveAfter = section.getWipeAfter();

                    if (oldest == null || file.creationTime() < oldest.creationTime()) {
                        if (age > _archiveAfter && !checkedReleases.contains(file.getName())) {
                            oldest = file;
                            logger.debug("AUTODELETE: New oldest file: {}, oldest in section {}", oldest.getName(), si.getName());
                        } else if (deleteOnSpace) {
                            oldest = file;
                            logger.debug("AUTODELETE: Deleting due to low space on slave: {} in section {}", file.getName(), si.getName());
                        }
                    }
                } catch (FileNotFoundException e) {
                    logger.warn("AUTODELETE: File missing", e.getCause());
                }
            }

            if (oldest == null) {
                throw new FileNotFoundException("Nothing to wipe");
            }

            return oldest;
        }

        public void run() {
            try {
                for (RemoteSlave remoteSlave :
                        GlobalContext.getGlobalContext().getSlaveManager().getAvailableSlaves()) {
                    if (_excludeSlaves.contains(remoteSlave.getName())) {
                        continue;
                    }

                    if (deleteOnDate) {
                        try {
                            InodeHandle oldestRelease;
                            while ((oldestRelease = getOldestRelease(remoteSlave)) != null) {
                                GlobalContext.getEventService().publishAsync(new AFSEvent(oldestRelease, remoteSlave));
                                if (_onlyAnnounce) {
                                    checkedReleases.add(oldestRelease.getName());
                                    continue;
                                }
                                oldestRelease.deleteUnchecked();
                                logger.info("AUTODELETE: Removing {}", oldestRelease.getName());
                            }
                        } catch (FileNotFoundException e) {
                            logger.warn("AUTODELETE: Oldest release not found for slave {}: {}", remoteSlave.getName(), e);
                        }
                    } else {

                        try {
                            long freespace = remoteSlave.getSlaveStatus().getDiskSpaceAvailable();
                            long freespaceBak = freespace;

                            if (freespace < _minFreeSpace) {
                                logger.debug("AUTODELETE: Space under limit for {}, will clean: {}<{}", remoteSlave.getName(), Bytes.formatBytes(freespace), Bytes.formatBytes(_minFreeSpace));
                                GlobalContext.getEventService().publishAsync(new AFSEvent(null, remoteSlave));
                                if (_onlyAnnounce) {
                                    return;
                                }
                            } else {
                                logger.debug("AUTODELETE: Space over limit for {} will not clean: {}>{}", remoteSlave.getName(), Bytes.formatBytes(freespace), Bytes.formatBytes(_minFreeSpace));
                            }

                            while (freespace < _minFreeSpace) {

                                InodeHandle oldestRelease;

                                try {
                                    oldestRelease = getOldestRelease(remoteSlave);

                                    GlobalContext.getEventService().publishAsync(new AFSEvent(oldestRelease, remoteSlave));

                                    //issue somewhere around here, we find the release but it never gets deleted..
                                    oldestRelease.deleteUnchecked();

                                    freespace = remoteSlave.getSlaveStatus().getDiskSpaceAvailable();

                                    logger.info("AUTODELETE: Removing {}, cleared {} on {}", oldestRelease.getName(), Bytes.formatBytes(remoteSlave.getSlaveStatus().getDiskSpaceAvailable() - freespace), remoteSlave.getName());

                                } catch (FileNotFoundException e) {
                                    logger.warn("AUTODELETE: Oldest release not found for slave {}: {}", remoteSlave.getName(), e);
                                }

                                if (freespaceBak == freespace) {
                                    logger.warn("Tried, but could not clean the slave to meet your demands, giving up for now");
                                    break;
                                }

                                freespaceBak = freespace;
                            }
                        } catch (SlaveUnavailableException e) {
                            logger.warn("Slave suddenly went offline");
                        }
                    }
                }
            } catch (NoAvailableSlaveException nase) {
                logger.warn("AUTODELETE: No slaves online, no point in running the cleaning procedure");
            }
        }

        private class AgeComparator implements Comparator<InodeHandle> {

            // Compare two InodeHandle.
            public final int compare(InodeHandle a, InodeHandle b) {
                Long aLong, bLong;
                try {
                    aLong = a.creationTime();
                    bLong = b.creationTime();
                } catch (FileNotFoundException fnfe) {
                    logger.warn("AUTODELETE: File missing when comparing age", fnfe.getCause());
                    return 0;
                }
                int result = aLong.compareTo(bLong);
                if (result == 0)
                    return a.getName().compareTo(b.getName());
                else
                    return result;
            }
        }
    }
}
