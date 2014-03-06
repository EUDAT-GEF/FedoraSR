package de.tuebingen.uni.sfs.erdo;

/**
 *
 * @author edima
 */
public class Sys {
	public static void expect(boolean cond) {
		if (!cond) {
			throw new AssertionError();
		}
	}
}
