package de.tuebingen.uni.sfs.erdo.irods;

import de.tuebingen.uni.sfs.erdo.Sys;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSProtocolManager;
import org.irods.jargon.core.connection.IRODSServerProperties;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.connection.IRODSSimpleProtocolManager;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.CollectionAndDataObjectListAndSearchAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.EnvironmentalInfoAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;
import org.irods.jargon.core.pub.IRODSFileSystemAO;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;

/**
 *
 * @author edima
 */
public class IrodsJargonCore implements Irods {
	IrodsAccessConfig config;
	IRODSAccount irodsAccount;
	IRODSProtocolManager irodsConnectionManager;
	IRODSSession irodsSession;
	IRODSAccessObjectFactory irodsAccessObjectFactory;
	IRODSFileSystemAO irodsFileSystem;
	IRODSFileFactory irodsFileFactory;
	CollectionAndDataObjectListAndSearchAO irodsLister;
	DataTransferOperations irodsTransfers;
	DataObjectAO irodsData;
	String currentDirectory = "";

	public IrodsJargonCore(IrodsAccessConfig c) throws JargonException {
		config = c;

		String[] pathComponents = c.path.split("/");
		Sys.expect(pathComponents.length >= 1);
		String zone = pathComponents[pathComponents[0].isEmpty() ? 1 : 0];

		irodsConnectionManager = IRODSSimpleProtocolManager.instance();
		irodsConnectionManager.initialize();
		irodsAccount = IRODSAccount.instance(c.server, c.port, c.username, c.password, c.path, zone, c.resource);
		irodsSession = IRODSSession.instance(irodsConnectionManager);
		irodsAccessObjectFactory = new IRODSAccessObjectFactoryImpl(irodsSession);

		EnvironmentalInfoAO environmentalInfoAO =
				irodsAccessObjectFactory.getEnvironmentalInfoAO(irodsAccount);
		IRODSServerProperties props =
				environmentalInfoAO.getIRODSServerPropertiesFromIRODSServer();
		Sys.expect(props.isTheIrodsServerAtLeastAtTheGivenReleaseVersion("rods3.0"));

		irodsLister = irodsAccessObjectFactory.getCollectionAndDataObjectListAndSearchAO(irodsAccount);
		irodsTransfers = irodsAccessObjectFactory.getDataTransferOperations(irodsAccount);
		irodsFileSystem = irodsAccessObjectFactory.getIRODSFileSystemAO(irodsAccount);
		irodsData = irodsAccessObjectFactory.getDataObjectAO(irodsAccount);
		irodsFileFactory = irodsAccessObjectFactory.getIRODSFileFactory(irodsAccount);

		currentDirectory = getPath(c.path);
	}

	@Override
	public String makeUrl(String path) throws Exception {
		return "irods://" + config.server + ":" + config.port + path;
	}

	private String getPath(String path) {
		String full = path.startsWith("/") ? path : (currentDirectory + "/" + path);
		while (full.endsWith("/") && full.length() > 1) {
			full = full.substring(0, full.length() - 1);
		}
		return full;
	}

	@Override
	public void icd(String path) {
		currentDirectory = getPath(path);
	}

	private Collection<String> ils(boolean collFlag, String path) throws Exception {
		List<String> ret = new ArrayList<String>();
		String d = getPath(path);
		List<CollectionAndDataObjectListingEntry> list = collFlag
				? irodsLister.listCollectionsUnderPath(d, 0) : irodsLister.listDataObjectsUnderPath(d, 0);
		for (CollectionAndDataObjectListingEntry cdoe : list) {
			ret.add(cdoe.getFormattedAbsolutePath());
		}
		return ret;
	}

	@Override
	public Collection<String> ilsColls(String path) throws Exception {
		return ils(true, path);
	}

	@Override
	public Collection<String> ilsFiles(String path) throws Exception {
		return ils(false, path);
	}

	@Override
	public String ichksum(String path) throws Exception {
		IRODSFile f = irodsFileFactory.instanceIRODSFile(getPath(path));
		return irodsData.computeMD5ChecksumOnDataObject(f);
	}

	@Override
	public void imkdir(String path) throws Exception {
		IRODSFile c = irodsFileFactory.instanceIRODSFile(getPath(path));
		irodsFileSystem.mkdir(c, true);
	}

	public void imv(String srcPath, String destPath) throws Exception {
		IRODSFile src = irodsFileFactory.instanceIRODSFile(getPath(srcPath));
		IRODSFile dst = irodsFileFactory.instanceIRODSFile(getPath(destPath));
		if (irodsFileSystem.isDirectory(src)) {
			irodsFileSystem.renameDirectory(src, dst);
		} else {
			irodsFileSystem.renameFile(src, dst);
		}
	}

	public void irm(String path) throws Exception {
		IRODSFile f = irodsFileFactory.instanceIRODSFile(getPath(path));
		if (irodsFileSystem.isDirectory(f)) {
			irodsFileSystem.directoryDeleteNoForce(f);
		} else {
			irodsFileSystem.fileDeleteNoForce(f);
		}
	}

	@Override
	public void iget(String irodsPath, File localFile) throws Exception {
		IRODSFile f = irodsFileFactory.instanceIRODSFile(getPath(irodsPath));
		irodsTransfers.getOperation(f, localFile, null, null);
	}

	@Override
	public void iput(File localFile, String irodsPath) throws Exception {
		IRODSFile f = irodsFileFactory.instanceIRODSFile(getPath(irodsPath));
		irodsTransfers.putOperation(localFile, f, null, null);
	}

	@Override
	public boolean exists(String irodsPath) throws Exception {
		IRODSFile f = irodsFileFactory.instanceIRODSFile(getPath(irodsPath));
		return irodsFileSystem.isFileExists(f);
	}
}
