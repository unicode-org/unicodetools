Setting up Eclipse
- Create a Java project, then use CVS to load into it.
- In Workbench Preferences, set the encoding to UTF-8, line ending to Windows.
- In each project, set the Console encoding to UTF-8
- To compare in Eclipse, select both files with command-shift (Mac), control-shift (Windows), then right click>Compare>Each Other

Updating to a new version of Unicode
(This is a bit ugly, because the code has grown organically)

Update in UCD_Names:
  AGE to add newest version.
Update in UCD to fix:
  public static final String latestVersion = "5.2.0" + (FIX_FOR_NEW_VERSION == 0 ? "" : "");
  public static final String lastVersion = "5.1.0"; // last released version
Update in utility/Utility:
  searchPath
Update in UCD_Types
  LIMIT_AGE
  AGE_VERSIONS
  
Then to run,
  Open MakeUnicodeFiles, & change 
    Generate: xxx
  to
    Generate: .*
    
Add to MakeUnicodeFiles
 - new value under DerivedAge
