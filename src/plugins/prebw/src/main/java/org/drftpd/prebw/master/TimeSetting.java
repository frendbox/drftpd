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
package org.drftpd.prebw.master;

/**
 * @author lh
 */
public class TimeSetting {
    private final String _times;
    private final String _interval;
    private final String _countAvg;

    public TimeSetting(String times, String interval, String countAvg) {
        _times = times;
        _interval = interval;
        _countAvg = countAvg;
    }

    public String getTimes() {
        return _times;
    }

    public String getStartInterval() {
        String[] interval = _interval.split(":");
        try {
            return interval[0];
        } catch (Exception ex) {
            return "*";
        }
    }

    public String getEndInterval() {
        String[] interval = _interval.split(":");
        try {
            return interval[1];
        } catch (Exception ex) {
            return "*";
        }
    }

    public String getCountAvg() {
        return _countAvg;
    }
}
