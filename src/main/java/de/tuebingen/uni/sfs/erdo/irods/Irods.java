package de.tuebingen.uni.sfs.erdo.irods;

import java.io.File;
import java.util.Collection;

/**
 * @author edima
 */
public interface Irods {
	String makeUrl(String path) throws Exception;

	void icd(String path) throws Exception;

	Collection<String> ilsColls(String path) throws Exception;

	Collection<String> ilsFiles(String path) throws Exception;

	String ichksum(String path) throws Exception;

	void imkdir(String path) throws Exception;

	void imv(String srcPath, String destPath) throws Exception;

	void irm(String path) throws Exception;

	void iget(String irodsPath, File localFile) throws Exception;

	void iput(File localFile, String irodsPath) throws Exception;

	boolean exists(String irodsPath) throws Exception;
}
