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
package org.drftpd.find.master.option;

import org.drftpd.find.master.FindUtils;
import org.drftpd.master.commands.ImproperUsageException;
import org.drftpd.master.indexation.AdvancedSearchParams;

/**
 * @author scitz0
 * @version $Id$
 */
public class NameOption implements OptionInterface {

    @Override
    public void exec(String option, String[] args, AdvancedSearchParams params) throws ImproperUsageException {
        if (args == null) {
            throw new ImproperUsageException("Missing argument for " + option + " option");
        }
        if (option.equalsIgnoreCase("-name")) {
            params.setName(FindUtils.getStringFromArray(args, " "));
        } else if (option.equalsIgnoreCase("-regex")) {
            params.setRegex(FindUtils.getStringFromArray(args, " "));
        } else if (option.equalsIgnoreCase("-exact")) {
            params.setExact(FindUtils.getStringFromArray(args, " "));
        } else if (option.equalsIgnoreCase("-endswith")) {
            params.setEndsWith(FindUtils.getStringFromArray(args, " "));
        }
    }
}
