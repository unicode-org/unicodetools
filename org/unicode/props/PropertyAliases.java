package org.unicode.props;

import java.util.List;

public interface PropertyAliases {
	public String getShortName();
	public String getLongName();
	/**
	 * Return a list of all the names, starting with the short name, then the long name, then others (if any).
	 */
	public List<String> getAllNames();
}
