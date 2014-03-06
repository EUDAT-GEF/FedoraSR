package de.tuebingen.uni.sfs.erdo.irods;

import java.io.File;
import java.util.Arrays;
import org.irods.jargon.core.exception.JargonException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 * @author edima
 */
@Ignore // we cannot build with unittests, unless we specify a test server configuration below
public class IrodsJargonCoreTest {
	static IrodsAccessConfig config = new IrodsAccessConfig() {
		{
			server = "" ; //clarin
			username = ""; // 
			password = "";
			path = "/vzEKUT/home/fedora";
			resource = "fedoraResc"; // the default here is demoResc
		}
	};

	@Test
	public void testPrint() throws Exception {
		IrodsJargonCore irods = new IrodsJargonCore(config);

		System.out.println("===================================================================");
		System.out.println("mkurl: " + irods.makeUrl("/vzEKUT/fedora/pids.txt"));
		System.out.println("colls: " + irods.ilsColls("/vzEKUT/fedora"));
		System.out.println("files: " + irods.ilsFiles("/vzEKUT/fedora"));
		System.out.println("===================================================================");
	}

	@Test
	public void testIcd() throws Exception {
		IrodsJargonCore irods = new IrodsJargonCore(config);
		irods.icd("/");
		irods.icd("/vzEKUT");
	}

	@Test
	public void testIls() throws Exception {
		IrodsJargonCore irods = new IrodsJargonCore(config);
		irods.icd("/");
		assertEquals(irods.ilsColls("/vzEKUT/fedora"), Arrays.asList(new String[]{"/vzEKUT/fedora/data"}));
		assertEquals(irods.ilsFiles("/vzEKUT/fedora/"), Arrays.asList(new String[]{"/vzEKUT/fedora/pids.txt"}));
		irods.icd("/vzEKUT/home/fedora");
		assertEquals(irods.ilsColls("/vzEKUT/fedora/"), Arrays.asList(new String[]{"/vzEKUT/fedora/data"}));
		assertEquals(irods.ilsFiles("/vzEKUT/fedora"), Arrays.asList(new String[]{"/vzEKUT/fedora/pids.txt"}));
		irods.icd("/vzEKUT/");
		assertEquals(irods.ilsColls("fedora/"), Arrays.asList(new String[]{"/vzEKUT/fedora/data"}));
		assertEquals(irods.ilsFiles("fedora"), Arrays.asList(new String[]{"/vzEKUT/fedora/pids.txt"}));
	}

	@Test
	public void testIchksum() throws Exception {
		IrodsJargonCore irods = new IrodsJargonCore(config);
		irods.icd("/");
		assertTrue(irods.ichksum("/vzEKUT/fedora/pids.txt").matches("[0-9A-Fa-f]*"));
		irods.icd("/vzEKUT/home/fedora");
		assertTrue(irods.ichksum("/vzEKUT/fedora/pids.txt").matches("[0-9A-Fa-f]*"));
		irods.icd("/vzEKUT/");
		assertTrue(irods.ichksum("fedora/pids.txt").matches("[0-9A-Fa-f]*"));
		irods.icd("/vzEKUT/fedora");
		assertTrue(irods.ichksum("pids.txt").matches("[0-9A-Fa-f]*"));
	}

//	@Test
	public void testImkdirImvIrm() throws Exception {
		IrodsJargonCore irods = new IrodsJargonCore(config);
		irods.icd("/vzEKUT");
		irods.imkdir("/vzEKUT/home/fedora/jargontest");
		assertEquals(irods.ilsColls("/vzEKUT/home/fedora"), Arrays.asList(new String[]{"/vzEKUT/home/fedora/jargontest"}));

		irods.imv("/vzEKUT/home/fedora/jargontest", "/vzEKUT/home/fedora/jargontest2");
		assertEquals(irods.ilsColls("/vzEKUT/home/fedora"), Arrays.asList(new String[]{"/vzEKUT/home/fedora/jargontest2"}));

		irods.irm("/vzEKUT/home/fedora/jargontest2");
		assertTrue(irods.ilsColls("/vzEKUT/home/fedora").isEmpty());

		irods.icd("/vzEKUT/home/fedora/");///////////////////////
		irods.imkdir("jargontest");
		assertEquals(irods.ilsColls("/vzEKUT/home/fedora"), Arrays.asList(new String[]{"/vzEKUT/home/fedora/jargontest"}));

		irods.imv("jargontest", "jargontest2");
		assertEquals(irods.ilsColls("/vzEKUT/home/fedora"), Arrays.asList(new String[]{"/vzEKUT/home/fedora/jargontest2"}));

		irods.irm("jargontest2");
		assertTrue(irods.ilsColls("/vzEKUT/home/fedora").isEmpty());
	}

//	@Test
	public void testIgetIput() throws Exception {
		IrodsJargonCore irods = new IrodsJargonCore(config);
		File f = new File("/tmp/test_jargon_lib.tmp");
		if (f.exists()) {
			f.delete();
		}
		assertTrue(!f.exists());
		irods.iget("/vzEKUT/fedora/pids.txt", f);
		assertTrue(f.exists());
		assertTrue(f.length() > 0);

		try {
			irods.iput(f, "/vzEKUT/home/fedora/test.txt");
		} catch (JargonException xc) {
		}
		assertEquals(irods.ilsFiles("/vzEKUT/home/fedora"), Arrays.asList(new String[]{"/vzEKUT/home/fedora/test.txt"}));

		irods.irm("/vzEKUT/home/fedora/test.txt");
		assertTrue(irods.ilsFiles("/vzEKUT/home/fedora").isEmpty());
	}
}
