Setting up Eclipse
- Create a Java project, then use CVS to load into it.
- In Workbench Preferences, set the encoding to UTF-8, line ending to Windows.
- In each project, set the Console encoding to UTF-8
- To compare in Eclipse, select both files with command-shift (Mac), control-shift (Windows), then right click>Compare>Each Other

Updating to a new version of Unicode
(This is a bit ugly, because the code has grown organically)

Create a new folder in the DATA directory, and populate from unicode.org/Public/...
  Eg
  6.0.0-Update
MakeUnicodeFiles.txt
  Generate: .*
  DeltaVersion: 1
  CopyrightYear: 2010 (or whatever)
   - new value under DerivedAge
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
  
Run MakeUnicodeFiles

If there is a new property value name, it will show up as an exception:
eg. Exception at: 06C3; TEH MARBUTA GOAL; R; TEH MARBUTA GOAL, Bad field name= "jg", value= "TEH MARBUTA GOAL"
Find the name in UCD_Types, add to end, change the LIMIT, eg:
  TEH_MARBUTA_GOAL = 57,
  LIMIT_JOINING_GROUP = 58;
Do the same in UCD_Names:
    "TEH_MARBUTA_GOA",

