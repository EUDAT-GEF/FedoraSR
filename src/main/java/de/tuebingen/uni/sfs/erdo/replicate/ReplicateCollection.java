package de.tuebingen.uni.sfs.erdo.replicate;

import de.tuebingen.uni.sfs.erdo.Log;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import de.tuebingen.uni.sfs.epicpid.PidServer;
import de.tuebingen.uni.sfs.epicpid.PidServerConfig;
import de.tuebingen.uni.sfs.epicpid.impl.PidServerImpl;
import de.tuebingen.uni.sfs.erdo.irods.Irods;
import de.tuebingen.uni.sfs.erdo.irods.IrodsAccessConfig;
import de.tuebingen.uni.sfs.erdo.irods.IrodsICommands;
import de.tuebingen.uni.sfs.erdo.irods.IrodsJargonCore;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class ReplicateCollection {
	static final boolean USE_ICOMMANDS = true;
	static final String PID_MAP_FILE_NAME = "pids.txt";
	static final String OLD_PID_MAP_FILE_NAME = "pids.txt.old";
	static String REPLICATE_EXTENSION = ".replicate";

	public static void main(String[] args) throws Exception {
		OptionParser parser = new OptionParser() {
			{
				acceptsAll(Arrays.asList("h", "?"), "show help").forHelp();
				acceptsAll(Arrays.asList("v", "verbose"), "verbose mode on");

				accepts("epic-pid-server").withRequiredArg().required()
						.describedAs("URL of the PID handle server");
				accepts("epic-pid-prefix").withRequiredArg().required()
						.describedAs("prefix of the PID handles");
				accepts("epic-pid-user").withRequiredArg().required()
						.describedAs("registered user on PID handle server");
				accepts("epic-pid-pass").withRequiredArg().required()
						.describedAs("password for PID account");

				if (!USE_ICOMMANDS) {
					accepts("irods-server").withRequiredArg().required();
					accepts("irods-port").withRequiredArg().ofType(Integer.class);
					accepts("irods-username").withRequiredArg().required();
					accepts("irods-password").withRequiredArg().required();
					accepts("irods-resource").withRequiredArg().required();
				}

				accepts("irods-source-path").withRequiredArg().required()
						.describedAs("irods collection to replicate");
				accepts("irods-target-path").withRequiredArg().required()
						.describedAs("irods target collection path for replication");

				accepts("only-first-file", "use this flag to test the replication of a single file");
			}
		};

		final OptionSet opt = parser.parse(args);
		if (opt.has("h")) {
			parser.printHelpOn(System.out);
			return;
		}

		Log log = new Log(opt.has("v"));

		PidServerImpl pider = new PidServerImpl(new PidServerConfig() {
			{
				epicServerUrl = (String) opt.valueOf("epic-pid-server");
				localPrefix = (String) opt.valueOf("epic-pid-prefix");
				username = (String) opt.valueOf("epic-pid-user");
				password = (String) opt.valueOf("epic-pid-pass");
			}
		});

		String irodsSourceColl = (String) opt.valueOf("irods-source-path");
		while (irodsSourceColl.endsWith(
				"/")) {
			irodsSourceColl = irodsSourceColl.substring(0, irodsSourceColl.length() - 1);
		}
		String irodsTargetColl = (String) opt.valueOf("irods-target-path");
		while (irodsTargetColl.endsWith(
				"/")) {
			irodsTargetColl = irodsTargetColl.substring(0, irodsTargetColl.length() - 1);
		}

		IrodsAccessConfig irodsConfig = new IrodsAccessConfig();
		if (!USE_ICOMMANDS) {
			irodsConfig.server = (String) opt.valueOf("irods-server");
			if (opt.has("irods-port")) {
				irodsConfig.port = (Integer) opt.valueOf("irods-port");
			}
			irodsConfig.username = (String) opt.valueOf("irods-username");
			irodsConfig.password = (String) opt.valueOf("irods-password");
			irodsConfig.resource = (String) opt.valueOf("irods-resource");
		}
		boolean onlyFirstFile = opt.has("only-first-file");

		Irods irods = USE_ICOMMANDS ? new IrodsICommands() : new IrodsJargonCore(irodsConfig);

		ReplicateCollection rc = new ReplicateCollection(log, irods);
		rc.replicate(irodsSourceColl, irodsTargetColl, pider, onlyFirstFile);
	}
	//
	Log log;
	Irods irods;

	public ReplicateCollection(Log log, Irods irods) {
		this.log = log;
		this.irods = irods;
	}

	public void replicate(String irodsSourceColl, String irodsTargetColl, PidServer pider, boolean onlyFirstFile) throws Exception {
		Map<String, ReplicaItem> pidMap = prepareReplication(irodsSourceColl, irodsTargetColl, pider);
		doReplication(irodsSourceColl, irodsTargetColl, pidMap, onlyFirstFile);
	}

	static class ReplicaItem {
		String pid;
		String irodsPath;
		boolean valid = false;

		public ReplicaItem(String pid, String irodsPath) {
			this.pid = pid;
			this.irodsPath = irodsPath;
		}
	}

	Map<String, ReplicaItem> prepareReplication(String irodsSourceColl, String irodsTargetColl, PidServer pider) throws Exception {
		final File pidFile = new File(PID_MAP_FILE_NAME);
		if (pidFile.exists()) {
			pidFile.renameTo(new File(OLD_PID_MAP_FILE_NAME));
		}

		irods.icd(irodsSourceColl);
		log.header("reading pids");

		irods.iget(pidFile.getName(), pidFile);
		Map<String, ReplicaItem> pidMap = readPidMappings(pidFile);

		irods.icd(irodsSourceColl);
		log.header("scanning collection");
		scan(irodsSourceColl, pidMap, pider);

		irods.icd(irodsSourceColl);
		writePidMap(pidMap, pidFile);

		try {
			irods.imv(PID_MAP_FILE_NAME, OLD_PID_MAP_FILE_NAME);
		} catch (Exception xc) {
		}

		irods.iput(pidFile, pidFile.getName());

		try {
			irods.irm(OLD_PID_MAP_FILE_NAME);
		} catch (Exception xc) {
		}

		return pidMap;
	}

	void doReplication(String irodsSourceColl, String irodsTargetColl, Map<String, ReplicaItem> pidMap, boolean onlyFirstFile) throws Exception {
		String replColl = "/" + irodsSourceColl.split("/")[1] + "/replicate";
		irods.icd(replColl);
		log.header("performing replication");
		for (Map.Entry<String, ReplicaItem> e : pidMap.entrySet()) {
			ReplicaItem ri = e.getValue();
			if (ri.valid) {
				String target = irodsTargetColl + ri.irodsPath.substring(irodsSourceColl.length());

				String replFileName = ri.irodsPath.substring(1)
						.replaceAll("/", "_").replaceAll(":", "_") + REPLICATE_EXTENSION;
				String replCommand = String.format("%s;%s;%s", ri.pid, ri.irodsPath, target);
				File replFile = new File(replFileName);
				Files.append(replCommand, replFile, Charsets.UTF_8);

				log.info("    " + ri.irodsPath + " -> " + target);

				try {
					irods.irm(replFileName);
				} catch (Exception xc) {
				}
				irods.iput(new File(replFileName), replFileName);

				if (onlyFirstFile) {
					return;
				}

				replFile.delete();
			} else {
				log.info("    " + ri.irodsPath + " -- old file, deleted?");
			}
		}
	}

	void scan(String irodsColl, Map<String, ReplicaItem> pidMap, PidServer pider) throws Exception {
		log.info(irodsColl);
		for (String path : irods.ilsFiles(irodsColl)) {
			expect(path.startsWith(irodsColl + "/"));
			if (!pidMap.containsKey(path)) {
				log.info(path);
				String url = irods.makeUrl(path);
				String checksum = irods.ichksum(path);
				String pid = pider.makePid(url, checksum, null).getId();
				pidMap.put(path, new ReplicaItem(pid, path));
			}
			pidMap.get(path).valid = true;
		}
		for (String coll : irods.ilsColls(irodsColl)) {
			expect(coll.startsWith(irodsColl + "/"));
			scan(coll, pidMap, pider);
		}
	}

	private void writePidMap(Map<String, ReplicaItem> pidMap, File pidFile) throws IOException {
		BufferedWriter w = Files.newWriter(pidFile, Charsets.UTF_8);
		try {
			for (Map.Entry<String, ReplicaItem> e : pidMap.entrySet()) {
				ReplicaItem ri = e.getValue();
				w.write(ri.pid + "\t" + ri.irodsPath + "\n");
			}
		} finally {
			w.close();
		}
	}

	private Map<String, ReplicaItem> readPidMappings(File pidFile) throws IOException {
		BufferedReader r = Files.newReader(pidFile, Charsets.UTF_8);
		Map<String, ReplicaItem> map = new LinkedHashMap<String, ReplicaItem>();
		for (String l = r.readLine(); l != null; l = r.readLine()) {
			if (l.trim().isEmpty()) {
				continue;
			}
			String[] arr = l.split("\t");
			expect(arr.length == 2);
			map.put(arr[1].trim(), new ReplicaItem(arr[0].trim(), arr[1].trim()));
		}
		return map;
	}

	public static void expect(boolean cond) {
		if (!cond) {
			throw new AssertionError();
		}
	}
}
