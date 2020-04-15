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
package org.drftpd.common.slave;

import java.io.Serializable;

/**
 * @author zubov
 * @version $Id$
 */
@SuppressWarnings("serial")
public class ConnectInfo implements Serializable {
    private final int _port;

    private final TransferIndex _transferIndex;

    private final TransferStatus _transferStatus;

    public ConnectInfo(int port, TransferIndex transferIndex, TransferStatus ts) {
        _port = port;
        _transferIndex = transferIndex;
        _transferStatus = ts;
    }

    public int getPort() {
        return _port;
    }

    public TransferIndex getTransferIndex() {
        return _transferIndex;
    }

    public TransferStatus getTransferStatus() {
        return _transferStatus;
    }
}
