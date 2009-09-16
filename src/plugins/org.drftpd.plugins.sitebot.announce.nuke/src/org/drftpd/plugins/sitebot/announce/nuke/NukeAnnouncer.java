/*
 * This file is part of DrFTPD, Distributed FTP Daemon.
 *
 * DrFTPD is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * DrFTPD is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * DrFTPD; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston, MA 02111-1307 USA
 */
package org.drftpd.plugins.sitebot.announce.nuke;

import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Map;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.drftpd.GlobalContext;
import org.drftpd.Bytes;
import org.drftpd.usermanager.User;
import org.drftpd.usermanager.NoSuchUserException;
import org.drftpd.usermanager.UserFileException;
import org.drftpd.vfs.DirectoryHandle;
import org.drftpd.event.NukeEvent;
import org.drftpd.plugins.sitebot.AnnounceInterface;
import org.drftpd.plugins.sitebot.AnnounceWriter;
import org.drftpd.plugins.sitebot.OutputWriter;
import org.drftpd.plugins.sitebot.SiteBot;
import org.drftpd.plugins.sitebot.config.AnnounceConfig;
import org.drftpd.util.ReplacerUtils;
import org.tanesha.replacer.ReplacerEnvironment;

/**
 * @author scitz0
 * @version $Id$
 */
public class NukeAnnouncer implements AnnounceInterface {

	private AnnounceConfig _config;

	private ResourceBundle _bundle;

	private String _keyPrefix;

	public void initialise(AnnounceConfig config, ResourceBundle bundle) {
		_config = config;
		_bundle = bundle;
		_keyPrefix = this.getClass().getName()+".";
		// Subscribe to events
		AnnotationProcessor.process(this);
	}

	public void stop() {
		// The plugin is unloading so stop asking for events
		GlobalContext.getEventService().unsubscribe(NukeEvent.class, this);
	}

	public String[] getEventTypes() {
		return new String[] { "nuke", "unnuke" };
	}

	@EventSubscriber
	public void onNukeEvent(NukeEvent event) {
		String type = "NUKE".equals(event.getCommand()) ? "nuke" : "unnuke";
		StringBuffer output = new StringBuffer();

		ReplacerEnvironment env = new ReplacerEnvironment(SiteBot.GLOBAL_ENV);
		DirectoryHandle nukeDir = new DirectoryHandle(event.getPath());
		String section = GlobalContext.getGlobalContext().getSectionManager().lookup(nukeDir).getName();
		env.add("section", section);
		env.add("dir", nukeDir.getName());
		env.add("path", event.getPath());
		env.add("relpath", event.getPath().replaceAll("/"+section+"/",""));
		env.add("user", event.getUser().getName());
		env.add("multiplier", ""+event.getMultiplier());
		env.add("nukedamount", Bytes.formatBytes(event.getNukedAmount()));
		env.add("reason", event.getReason());
		env.add("size", Bytes.formatBytes(event.getSize()));

		output.append(ReplacerUtils.jprintf(_keyPrefix+type, env, _bundle));

		for (Map.Entry<String,Long> entry : event.getNukees().entrySet()) {
			User user;
			try {
				user = GlobalContext.getGlobalContext().getUserManager().getUserByName(entry.getKey());
			} catch (NoSuchUserException e1) {
				continue;
			} catch (UserFileException e1) {
				continue;
			}
			ReplacerEnvironment nukeeenv = new ReplacerEnvironment(SiteBot.GLOBAL_ENV);
			nukeeenv.add("user", user.getName());
			nukeeenv.add("group", user.getGroup());
			nukeeenv.add("nukedamount", Bytes.formatBytes(entry.getValue()));
			output.append(ReplacerUtils.jprintf(_keyPrefix+type+".nukees", nukeeenv, _bundle));
		}

		AnnounceWriter writer = _config.getPathWriter(type, nukeDir);
		// Check we got a writer back, if it is null do nothing and ignore the event
		if (writer != null) {
			sayOutput(output.toString(), writer);
		}
	}

	private void sayOutput(String output, AnnounceWriter writer) {
		StringTokenizer st = new StringTokenizer(output,"\n");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			for (OutputWriter oWriter : writer.getOutputWriters()) {
				oWriter.sendMessage(token);
			}
		}
	}
}