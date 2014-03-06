package de.tuebingen.uni.sfs.erdo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author edima
 */
public class Log {
	static Logger logger = LoggerFactory.getLogger(Log.class);
	boolean verbose = false;

	public Log(boolean verbose) {
		this.verbose = verbose;
	}

	public void header(String msg) {
		System.out.println(msg);
		logger.info(msg);
	}

	public void info(String msg) {
		if (verbose) {
			System.out.println("\t" + msg);
		}
		logger.info("\t" + msg);
	}

	public void warn(String msg) {
		System.out.println("!!! " + msg);
		logger.warn(msg);
	}
}
