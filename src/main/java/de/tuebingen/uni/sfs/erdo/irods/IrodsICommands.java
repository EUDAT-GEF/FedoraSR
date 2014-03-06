package de.tuebingen.uni.sfs.erdo.irods;

import de.tuebingen.uni.sfs.erdo.Sys;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author edima
 */
public class IrodsICommands implements Irods {
	String urlRoot = "";

	@Override
	public String makeUrl(String path) throws Exception {
		if (!path.startsWith("/")) {
			throw new IllegalArgumentException("/ is Sys.expected to start the argument: " + path);
		}
		if (urlRoot.isEmpty()) {
			urlRoot = null;
			String prefixHost = "irodsHost=";
			String prefixPort = "irodsPort=";
			String host = "unknown", port = null;
			String res = exec("ienv");
			for (String s : res.split("\\s+")) {
				if (s.startsWith(prefixHost)) {
					host = s.substring(prefixHost.length());
				} else if (s.startsWith(prefixPort)) {
					port = s.substring(prefixPort.length());
				}
			}
			urlRoot = "irods://" + host + (port != null ? (":" + port) : "");
		}
		return urlRoot + path;
	}

	@Override
	public String ichksum(String path) throws Exception {
		String output = exec("ichksum", path);
		String[] lines = output.split("\n");
		Sys.expect(lines.length == 2);
		String[] parts = lines[0].trim().split("\\s+");
		Sys.expect(parts.length == 2);
		String chksum = parts[1];
		Sys.expect(chksum.length() == 32);
		Sys.expect(chksum.matches("[0-9A-Fa-f]*"));
		return chksum;
	}

	@Override
	public void icd(String path) throws Exception {
		exec("icd", path);
	}

	private void ils(String path, List<String> colls, List<String> files) throws Exception {
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}

		boolean expectColl = false;
		String output = exec("ils", path);
		String[] lines = output.split("\\s+");
		Sys.expect(lines[0].equals(path + ":"));
		for (int i = 1; i < lines.length; ++i) {
			String s = lines[i];
			if (s.equals("C-")) {
				if (expectColl) {
					throw new Exception("invalid state, ils returned: " + Arrays.asList(lines));
				}
				expectColl = true;
			} else {
				if (expectColl) {
					colls.add(s);
					expectColl = false;
				} else {
					Sys.expect(!s.contains("/"));
					files.add(path + "/" + s);
				}
			}
		}
	}

	@Override
	public Collection<String> ilsColls(String path) throws Exception {
		List<String> res = new ArrayList<String>();
		ils(path, res, new ArrayList<String>());
		return res;
	}

	@Override
	public Collection<String> ilsFiles(String path) throws Exception {
		List<String> res = new ArrayList<String>();
		ils(path, new ArrayList<String>(), res);
		return res;
	}

	@Override
	public void imkdir(String path) throws Exception {
		exec("imkdir", path);
	}

	@Override
	public void irm(String path) throws Exception {
		exec("irm", path);
	}

	@Override
	public void imv(String srcPath, String destPath) throws Exception {
		exec("imv", srcPath, destPath);
	}

	@Override
	public void iget(String path, File file) throws Exception {
		exec("iget", path, file.getAbsolutePath());
	}

	@Override
	public void iput(File file, String path) throws Exception {
		exec("iput", file.getAbsolutePath(), path);
	}

	private static class Prox {
		int exitValue = 0;
		String output = null;
		String error = null;
	}

	private String exec(String... args) throws Exception {
		Prox r = execute(args);
		if (r.exitValue != 0) {
			throw new IOException("error executing " + Arrays.asList(args)
					+ "\nPlease make sure you are logged into irods (use iinit)"
					+ "\nNormal output:" + r.output
					+ "\nError output:" + r.error);
		}
		return r.output;
	}

	private Prox execute(String... args) throws InterruptedException, IOException {
		Prox r = new Prox();
		Process p = Runtime.getRuntime().exec(args);
		r.output = new String(ByteStreams.toByteArray(p.getInputStream()), Charsets.UTF_8);
		r.error = new String(ByteStreams.toByteArray(p.getErrorStream()), Charsets.UTF_8);
		r.exitValue = p.waitFor();
		return r;
	}

	public boolean exists(String irodsPath) throws Exception {
		Prox r = execute("ils", irodsPath);
		return r.exitValue != 0 ? false : r.output.trim().endsWith(irodsPath);
	}
}
