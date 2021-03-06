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
package org.drftpd.master.indexation.analysis;

import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.util.Version;

import java.io.Reader;

/**
 * @author fr0w
 * @version $Id$
 */
public class AlphanumericalTokenizer extends CharTokenizer {
    public AlphanumericalTokenizer(Reader input) {
        super(Version.LUCENE_36, input);
    }

    @Override
    protected boolean isTokenChar(int c) {
        return Character.isLetter(c) || Character.isDigit(c) || isWildcardChar(c);
    }

    private boolean isWildcardChar(int c) {
        return c == '?' || c == '*';
    }

    @Override
    protected int normalize(int c) {
        return (Character.isLetter(c) ? Character.toLowerCase(c) : c);
    }
}
