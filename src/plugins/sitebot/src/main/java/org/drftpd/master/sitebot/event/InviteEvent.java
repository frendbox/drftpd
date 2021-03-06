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
package org.drftpd.master.sitebot.event;

import org.drftpd.master.event.Event;
import org.drftpd.master.usermanager.User;

/**
 * @author mog
 * @version $Id$
 */
public class InviteEvent extends Event {
    private final String _ircNick;

    private final User _user;

    private final String _targetBot;

    public InviteEvent(String command, String ircUser, User user, String targetBot) {
        super(command, System.currentTimeMillis());
        _ircNick = ircUser;
        _user = user;
        _targetBot = targetBot;
    }

    public String getIrcNick() {
        return _ircNick;
    }

    public User getUser() {
        return _user;
    }

    public String getTargetBot() {
        return _targetBot;
    }
}
