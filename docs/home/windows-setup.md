# Windows Setup

[TOC]

This section provides comprehensive, step-by-step instructions for setting up
the Unicode Tools on Windows, from installing the prerequisites to building and
running the Tools.

## **(1) Browse the documentation**

For a basic idea about the Unicode Tools and the available documentation, begin
by browsing this site and noting the prerequisites (JDK, Eclipse, etc.).

## **(2) Install the 64-bit JDK**

Visit the Java SE Development Kit Downloads site,
<http://www.oracle.com/technetwork/java/javase/downloads/index.html>. In the
present instructions, JDK 8 is used: click on the JDK Download button under Java
SE 8u151/ 8u152 and install Java SE Development Kit 8u152, Windows x64,
jdk-8u152-windows-x64.exe. This installs the JDK and the JRE. By default, the
(64-bit) JDK goes into C:\\Program Files\\Java\\jdk..., but a different location
can be chosen, e.g., C:\\Programs\\Java\\jdk1.8.0_152. By default, besides the
JRE inside the JDK, a separate JRE goes into C:\\Program Files\\Java\\jre...,
but the JDK (with its own JRE inside) can be installed without the separate JRE.
A JRE may already be present on the system, e.g., in C:\\Program
Files\\Java\\jre1.8.0_151, and can be kept there. For Java post-installation
documentation, visit <http://docs.oracle.com/javase/8/docs/>.

The 32-bit JDK requires using some Java VM arguments later on to build the
Tools, which can be avoided by using the 64-bit version.

Update (Jan 15, 2020) - Oracle now charges for older versions of java. JDK 8
based on OpenJDK is avaliable here: <https://adoptopenjdk.net/> This version of
JDK 8 contains a "jre" folder inside. You'll need to manually point to the jre
location from eclipse. Alternatively, Azul also releases uncustomized versions
of OpenJDK called zulu jdk. Can be obtained here:
<https://www.azul.com/downloads/zulu-community/?&architecture=x86-64-bit&package=jdk>
. RedHat developer also has a version of OpenJDK for windows, however, it
requires an account.

## **(3) Install Eclipse**

Skim through the instructions at
<http://cldr.unicode.org/development/eclipse-setup>, then go to
<http://www.eclipse.org/> and click on the orange Download button in the
upper-right, which takes you to <http://www.eclipse.org/downloads/>. Under Get
Eclipse Oxygen, click on the faint gray link Download Packages, which goes to
<http://www.eclipse.org/downloads/eclipse-packages/>. Download and install
"Eclipse IDE for Java EE Developers", 64-bit, from the orange "64 bit" link on
the right. This downloads eclipse-jee-oxygen-1a-win32-x86_64.zip. Expand the
.zip in a desired directory; e.g., expanding all in C:\\Programs, the .zip
becomes C:\\Programs\\eclipse. The .zip is big (over 300 MB) and takes a while
to expand.

The Eclipse site UI changes over time, although the main page URLs seem stable.
The Install Eclipse step of these notes was written in October 2017.

The smaller, non-EE version may work, but the Unicode Tools Eclipse setup page
(<http://cldr.unicode.org/development/eclipse-setup>) states: Make sure you have
"Eclipse for Java EE". To compare Eclipse packages, see
<http://www.eclipse.org/downloads/compare.php>.

The processor architecture might have to match the JDK's, hence 64-bit Eclipse.

### ***(3a) Run eclipse.exe and select a workspace folder***

Create a workspace folder (avoiding deep paths on Windows). For example,
C:\\Work\\svn. Run Eclipse for the first time, and use the workspace location
created earlier. (By default, it is C:\\Users\\*username*\\eclipse-workspace.)
That workspace directory will contain cldr\\trunk, draft\\trunk,
unicodetools\\trunk. This file tree is intended to replicate the subversion
structure (repository listing) shown at <http://www.unicode.org/repository/>.
See also the simple view at http://www.unicode.org/repos/ {
[cldr/](http://www.unicode.org/repos/cldr/) ,
[draft/](http://www.unicode.org/repos/draft/) ,
[unicodetools/](http://www.unicode.org/repos/unicodetools/) } (not just
http://www.unicode.org/repos/) as well as
<http://www.unicode.org/utility/trac/browser/> for unicodetools.

### ***(3b) Check that Eclipse is working***

Check as follows:

*   Help » Check for Updates, then
*   Help » Install New Software... » Work with: --All Available Sites-- (just
    run; do not install anything quite yet). If you get an error message,
    something went wrong and the installation did not complete successfully. Try
    reinstalling Eclipse.
*   When using the 64-bit Eclipse, check in Task Manager (Ctrl+Shift+Esc) that
    the eclipse.exe process is indeed 64-bit, i.e., is not shown with "(32 bit)"
    in the process name.

### ***(3c) Set preferences***

From the Welcome page, click on Review IDE configuration settings and set the
few preferences shown there. Then change EOL and other settings in Window »
Preferences:

Window » Preferences › General » Editors » Text Editors:

*   Insert spaces for tabs
*   Show whitespace characters (and see "configure visibility")

Window » Preferences › General » Workspace:

*   Save automatically before build
*   Text file encoding = Other: UTF-8
*   New text file line delimiter = Other: Unix

### ***(3d) Set or update the JRE***

Eclipse should have added the JRE from the JDK installed earlier. To check and
update, go to Window » Preferences › Java » Installed JREs, and add/replace the
JRE with the one from the JDK installed in step (**2**), selecting a Standard
VM:

*   JRE home: C:\\Programs\\Java\\jdk1.8.0_152\\jre
*   JRE name (autopopulated): jdk1.8.0_152

Checkmark it as default, and it probably does not hurt to keep the other one
around (C:\\Program Files\\Java\\jre1.8.0_151) if that had already been
installed before. See <http://stackoverflow.com/a/998643/185799>.

## **(4) Install and configure external tools**

Install and configure Ant, Subclipse, and Tomcat.

### ***(4a) Install Ant (binaries only)***

Eclipse comes with Ant – see

*   Window » Preferences › Ant » Runtime › Ant Home Entries, or
*   Help » About Eclipse » Installation Details › (Eclipse Installation Details
    window) Plug-ins › Plug-in Id › org.apache.ant.

However, if missing, do the following. Read "Setup User Libraries" on
<http://cldr.unicode.org/development/eclipse-setup>. Go to
<http://ant.apache.org/>, then to <http://ant.apache.org/bindownload.cgi>, and
download (from a US mirror selected on that webpage) the .zip archive
apache-ant-1.10.1-bin.zip. Using the sample path from step (**2**), expand the
.zip in C:\\Programs\\Java\\apache-ant-1.10.1, i.e, extract-all to
C:\\Programs\\Java, as a sibling directory of the JDK (and any JRE) directories
under C:\\Programs\\Java.

The Ant executable is

*   C:\\Programs\\eclipse\\plugins\\org.apache.ant_1.10.1.v20170504-0840\\bin\\ant
    if from Eclipse, or
*   C:\\Programs\\Java\\apache-ant-1.10.1\\bin\\ant if installed separately.

The Ant JAR is

*   C:\\Programs\\eclipse\\plugins\\org.apache.ant_1.10.1.v20170504-0840\\lib\\ant.jar
    if from Eclipse, or
*   C:\\Programs\\Java\\apache-ant-1.10.1\\lib\\ant.jar if installed separately.

### ***(4b) Define ANT_LIB***

Define ANT_LIB. In Eclipse, from the menu, Window » Preferences › Java » Build
Path » User Libraries › New... › type ANT_LIB and click OK (do not check the
"System library" checkbox). Select ANT_LIB and click Add External JARs... (not
Add JARs...) and navigate to the Ant JAR located in step (**4a**). Click OK.

### ***(4c) Install Subclipse***

Eclipse does not come with Subclipse, but check in Eclipse Installation Details
if it did. Do the following to install it. Go to <http://subclipse.tigris.org/>,
then to <https://github.com/subclipse/subclipse/wiki>, and copy the URL listed
under Installation, Latest:
<https://dl.bintray.com/subclipse/releases/subclipse/latest/>. In Eclipse, go to
Help » Install New Software... › (Install window) and uncheck Hide items that
are already installed. In Work with: paste the URL copied earlier and press
Enter. Check both Core SVNKit Library and Subclipse groups with all their
subitems. Click Next, Next, I accept the terms, Finish. Click to see details
while installing. Click OK on the Security Warning dialog about unsigned
content. Click Restart Now to restart Eclipse.

### ***(4d) Configure SVNKit***

After restart, on the Eclipse menu, Window » Preferences › Team » SVN. Dismiss
the dialog that pops up about missing JavaHL libraries. Then on the SVN page,
SVN interface » Client: pick SVNKit and click Apply. According to
<http://cldr.unicode.org/development/eclipse-setup/subeclipse-setup>, if you do
not pick SVNKit, you get an error when checking out files with svn+ssh://.

### ***(4e) Install Tomcat***

Go to <http://tomcat.apache.org>, choose a version (e.g., Tomcat 8.5.24
Released) and click on Download. From the Downloads page, click on zip (e.g.,
<http://apache.mirrors.lucidnetworks.net/tomcat/tomcat-8/v8.5.24/bin/apache-tomcat-8.5.24.zip>)
and expand in a directory which is a sibling to the JDK, e.g.,
C:\\Programs\\Java (the .zip will expand to apache-tomcat-8.5.24 under Java
there). The directory will contain bin, lib, etc., subdirectories, similar to
the JDK directory.

The .zip does not include Javadoc documentation. That needs to be downloaded
separately.

## **(5) Check out projects from SVN: draft, unicodetools, and cldr**

In the steps below, the local project names will be: draft-trunk,
unicodetools-trunk, cldr-trunk. They will be created in the local workspace
directory, such as C:\\Work\\svn\\unicodetools-trunk etc.

### ***(5a) Check out draft***

The following steps (**5a**–**c**) illustrate checking out a subset of the tree.
A unicode.org account is required.

On the Eclipse menu, File » Import... › (Import window) SVN » Checkout Projects
from SVN » Next › (Checkout from SVN) Create a new repository location » Next.
In Url:, type svn+ssh://unicode.org/repos/draft » Next » (Enter SSH Credentials)
type username and password for unicode.org account, select Use password
authentication, check Save information, click OK. In the Subversion
Authentication dialog, click Yes to save unicode.org server's key fingerprint.
In the next dialog that pops up, enter password again and check Save.

### ***(5b) Check out the first tree level under draft/trunk/***

On the Select Folder page, select trunk, expand it to see what its
subdirectories (many are not needed), click Next. On the Check Out As page, type
Project Name: draft-trunk, select Depth: Immediate children, including folders,
click Next. Keep default settings on the following page (Use default workspace
location: C:\\Work\\svn, etc.), click Finish. In the Java perspective, in
Package Explorer, see the shallow file hierarchy created under draft-trunk.
Still in the Java perspective, see in the Console pane the SVN commands issued
by these actions in the UI, similar to the following:

checkout svn+ssh://unicode.org/repos/draft/trunk -r HEAD --depth=immediates
--force

A C:/Work/svn/draft-trunk/test

A C:/Work/svn/draft-trunk/other

. . .

A C:/Work/svn/draft-trunk/.settings

Checked out revision 4348.

### ***(5c) Select which subdirectories to download and which to exclude***

Downloading only the first level under a node is to avoid recursively
downloading the entire tree, because some parts of the tree contain a lot of
files, e.g., draft/trunk/reports/tr51/images/, others are old and stale, e.g.,
draft/trunk/ucd/. The approach is to download only the immediate child nodes,
select which to keep and exclude the others, repeat as desired for other nodes,
and finally recursively download the subtrees under the chosen nodes chosen to
keep.

A good way to choose what to download (check out) is to browse the file tree
under draft/trunk in the Web view, sorting by age to see which are the most
actively updated files and folders:
<http://www.unicode.org/repository/draft/trunk/?sortby=date#dirlist>, or which
contain a lot of files.

Typical operations:

*   To exclude a directory, right-click on it, Team » Update to Version... ›
    (Update Resources) Depth: Exclude » OK.
*   To exclude multiple directories at the same time, select all of them,
    right-click, and continue as above.
*   To download a single tree level under one node, select that node,
    right-click on it, Team » Update to Version... › (Update Resources) Depth:
    Immediate children, including folders, and check Change working copy to
    specified depth, then OK.
*   To download a full subtree, right-click on it, Team » Update to Version... ›
    (Update Resources) Depth: Fully recursive, and check Change working copy to
    specified depth, then OK.
*   To restore a complete tree level or full subtree, update it as above
    checkmarking Change working copy to specified depth.

For example, a downloaded draft-trunk tree may look like the following:

draft/trunk

\\ charts (full)

\\ emoji (full)

\\ Public (full)

\\ reports (selective)

\\ tr10 (full)

\\ tr14 (full)

. . .

\\ tr51 (selective)

\\ images (empty)

\\ tr9 (full)

\\ reports-v2.css

. . .

### ***(5d) Check out unicodetools***

The following steps (**5d**–**e**) illustrate an approach of checking out
multiple projects at once: first download a fully recursive tree, then import
projects from the downloaded file tree on the local disk. This will create
different views into the same single copy of the file tree, e.g., one view as a
general project for the entire tree and additional views as Eclipse projects for
the subtrees:

*   UnicodeJsps (rooted at unicodetools-trunk\\UnicodeJsps),
*   UnicodeJspsTest (rooted at unicodetools-trunk\\UnicodeJspsTest), and
*   unicodetools (rooted at unicodetools-trunk\\unicodetools).

This is actually convenient, because multiple projects can be synced with a
single command applied to the whole tree. Alternatively, individual projects can
be created, but it is a bit tricky to place them in the same directory structure
as on the server, and they will have to be synced individually with one command
per project.

Normally, when the local tree goes out of sync w.r.t. the corresponding tree in
SVN, the trunk projects will have white asterisks on black squares to mark that
they are behind, but the subprojects will not show those markers. The asterisks
go away after syncing.

On the Eclipse menu, File » Import... › (Import window) SVN » Checkout Projects
from SVN » Next › (Select/Create Location page) Create a new repository location
» Next › svn+ssh://unicode.org/repos/unicodetools » Next (Select Folder page) »
select trunk » Next » (Check Out As page) Choose how to check out folder trunk ›
Check out as a project in the workspace, Project Name: unicodetools-trunk,
Depth: Fully recursive » Next » Use default workspace location » Finish. This is
a big tree, so the download will take some time.

### ***(5e) Create projects from the full local tree just imported***

In the Package Explorer pane, right-click on unicodetools-trunk (or the blank
area after having selected unicodetools-trunk), click Configure » Configure and
Detect Nested Projects... » (Import Projects from File System or Archive page)
Import source: (path to unicodetools-trunk) C:\\Work\\svn\\unicodetools-trunk ›
Folder › select the folders › Finish.

This will add the projects to the list in Package Explorer and configure them as
Eclipse projects (such as Java projects) rather than general projects.

### ***(5f) Repeat for cldr***

Import svn+ssh://unicode.org/repos/cldr fully recursive, with Project Name:
cldr-trunk. Then Configure and Detect Nested Projects... with Import source:
C:\\Work\\svn\\cldr-trunk.

### ***(5g) Optionally repeat for icu4j***

The project icu4j-trunk-main can also be checked out, but is not strictly needed
for running the Tools in unicodetools-trunk.

### ***(5h) Sync the files***

Every so often, and before using the Tools, the local files will need to be
synced. To sync, right-click on a folder in Package Explorer and click Team »
Update to HEAD.

## **(6) Fix compile errors due to missing dependencies**

After importing the projects, some compile errors will be shown with red markers
on the icons in Package Explorer. The errors are also shown on the Problems pane
at the bottom of the Java perspective. Some of those errors are due to missing
dependencies. To fix those errors, add the necessary dependencies to the
project.

### ***(6a) The import javax.servlet cannot be resolved***

This error can show on files under cldr-apps. There are two ways to configure
the Apache Tomcat server.

*   Via Preferences. On the Eclipse menu, Window » Preferences » Server »
    Runtime Environments » Add... » (New Server Runtime Environment window)
    Select the type of runtime environment: Apache › Apache Tomcat v9.0 › Next »
    (Tomcat Server window) Tomcat installation directory: › Browse... » (Browse
    For Folder window)
*   Or via the Servers pane. Open the Servers pane. On the Eclipse menu, Window
    » Show View » Other... » (Show View window) Server › Servers › Open. Then,
    in the Servers pane at the bottom, click on the link to create a new server
    or right-click » New » Server » (New Server window) Select the server type:
    Apache » Tomcat 8.5 Server » Next » (Tomcat Server window) Tomcat
    installation directory: › Browse... » (Browse For Folder window)

In the Browse For Folder window, select the Tomcat installation directory
created in step (**4e**) and click Finish.

Then, right-click on the project (cldr-apps) » Properties » Targeted Runtimes ›
check (and select) Apache Tomcat v8.5 › Apply. At this point, Eclipse should add
the necessary libraries to the build path, and the error should disappear. See
also
<https://stackoverflow.com/questions/4076601/how-do-i-import-the-javax-servlet-api-in-my-eclipse-project>.

The same error is reported for UnicodeJsps, for instance for the \*.jsp files
(bidi.jsp etc.). Repeat the step above with project properties.

### ***(6b) Ant errors***

Ant-related errors can be similarly fixed by adding the libraries to the build
path.

### ***(6c) Resolve other errors shown in Problems***

You may see an error of type Build Path Problem: Project 'UnicodeJsps' is
missing required library:
'\\Users\\markdavis\\Documents\\apache-tomcat-8.0.43\\lib\\servlet-api.jar'.
Right-click on UnicodeJsps » Properties » Java Build Path › (Libraries tab) see
the same error and select it › Edit... » (Edit JAR window) Select
servlet-api.jar under C:\\Programs\\Java\\apache-tomcat-8.5.24\\lib › Open ›
Apply.

After a few seconds, the error should disappear from Problems.

## **(7) Configure the build and execution environment**

Several settings are needed to build and run or debug the Tools: a project named
Generated and a few macros.

### ***(7a) Create a project named Generated***

The directory for this project will contain generated UCD caches, UCD derived
files, UCA files, and result files from running the invariant tests. The
directory without a project would suffice, but a project allows viewing the
files in Eclipse.

Create a subdirectory named Generated under unicodetools-trunk, and another
named BIN under Generated.

On the Eclipse menu, File » New » Project... » (New Project window) Wizards:
General \\ Project › Next »

Project name: Generated

Uncheck Use default location

Location: unicodetools-trunk\\Generated (e.g.,
C:\\Work\\svn\\unicodetools-trunk\\Generated)

» Next

Referenced projects: (none)

» Finish

### ***(7b) Define macros***

In Package Explorer, locate the file Main.java in the unicodetools-trunk tree.
(A fully qualified path may look like
C:\\Work\\svn\\unicodetools-trunk\\unicodetools\\org\\unicode\\text\\UCD\\Main.java.)
Right-click on it and click Run As » Run Configurations... or Debug As » Debug
configurations... Double-click on Java Application and set the following
settings:

*   On the Main tab, check Stop in main (for debugging).
*   On the Arguments tab, set Program arguments: build MakeUnicodeFiles.
*   On the Arguments tab, set VM arguments with the directories where
    unicodetools-trunk and cldr-trunk were created, e.g.,

-DOTHER_WORKSPACE=C:\\Work\\svn\\unicodetools-trunk

-DSVN_WORKSPACE=C:\\Work\\svn\\unicodetools-trunk\\unicodetools

-DUCD_DIR=C:\\Work\\svn\\unicodetools-trunk\\unicodetools\\data

-DCLDR_DIR=C:\\Work\\svn\\cldr-trunk

Apply and close without running yet.

The VM settings can be set globally in the JRE default VM arguments or in each
Run or Debug configuration.

All settings are saved in an XML Eclipse configuration file in the current
workspace, e.g.,
C:\\Work\\svn\\.metadata\\.plugins\\org.eclipse.debug.core\\.launches\\Main.launch.
Save that file for an easier migration when installing the Tools on a different
machine.

## **(8) Build and run the Tools**

A few source changes are necessary to build and run the Tools.

### ***(8a) Make Windows-specific changes to the sources***

Try running with the settings and arguments "build MakeUnicodeFiles" described
in step (**7b**). Output messages will start flowing in the Console pane,
followed by an exception in java.lang.ExceptionInInitializerError, caused by a
java.io.FileNotFoundException due to an improperly formatted path of the form
\\C:\\Work\\svn\\unicodetools-trunk\\unicodetools\\data\\ucd\\11.0.0-Update\\PropertyValueAliases.txt.
The file PropertyValueAliases.txt is present, but the path has an extraneous
leading backslash. To fix that, modify the function openFile() in the cldr-tools
file FileUtilities.java by adding the an if-else statement as shown below. In
Package Explorer, that file is under cldr-tools \\ org.unicode.cldr.draft; on
the disk, it is
C:\\Work\\svn\\cldr-trunk\\tools\\java\\org\\unicode\\cldr\\draft\\FileUtilities.java.

public static BufferedReader openFile(String directory, String file, Charset
charset) {

try {

**if (directory.equals(""))**

**return new BufferedReader(new InputStreamReader(new FileInputStream(new
File(file)), charset));**

**else**

return new BufferedReader(new InputStreamReader(new FileInputStream(new
File(directory, file)), charset));

} catch (FileNotFoundException e) {

throw new ICUUncheckedIOException(e); // handle dang'd checked exception

}

}

The inserted code is around line 258 as of December 2017.

A similar change is needed for running GenerateEnums.java as part of the process
to update PropertyValueAliases.txt. Modify the function openWriter() in the same
file FileUtilities.java around line 65 as follows:

public static PrintWriter openWriter(String dir, String filename, String
encoding) throws IOException {

File file**;**

**if (dir.equals(""))**

**file = new File(filename);**

**else**

**file** = new File(dir, filename);

if (SHOW_FILES && log != null) {

### ***(8b) Run the Tools***

Run Main.java again with "build MakeUnicodeFiles". The output in the Console
pane should end with a line that starts with "\*\*\* Done \*\*\*", and a bunch
of files should be generated in the subdirectories under Generated. Refresh that
folder in Package Explorer to see them directly in Eclipse. See
<https://sites.google.com/site/unicodetools/#TOC-Building-Files> for additional
details.

Every time the Tools are run to regenerate the derived UCD files for a new
version, it is essential to delete the UCD caches before running "build
MakeUnicodeFiles".

### ***(8c) Update PropertyAliases.txt and PropertyValueAliases.txt***

When making changes to PropertyAliases.txt and/or PropertyValueAliases.txt, run
GenerateEnums.java to regenerate two Java source files, UcdProperty.java and
UcdPropertyValues.java, then move those two files to their home in
unicodetools\\org\\unicode\\props, and rerun Main.java to regenerate the UCD
files. The sequence of steps is as follows:

*   Edit PropertyAliases.txt and PropertyValueAliases.txt by hand
*   Run unicodetools\\org\\unicode\\props\\GenerateEnums.java (the path on the
    disk looks like
    C:\\Work\\svn\\unicodetools-trunk\\unicodetools\\org\\unicode\\props\\GenerateEnums.java)
*   This will create two Java source files, UcdProperty.java and
    UcdPropertyValues.java, in a parallel path unicodetools\\org\\unicode\\props
    under unicodetools, i.e., on a path such as
    C:\\Work\\svn\\unicodetools-trunk\\unicodetools\\unicodetools\\org\\unicode\\props
    on the disk.
    *   If you set `-DUNICODETOOLS_DIR=<svn unicodetools root>` then the tool
        overwrites the live .java files directly, and you can inspect the
        changes using `svn diff`.
*   Take the generated UcdPropertyValues.java, and UcdProperty.java if it has
    any changes, and move them to replace the files with the same names in
    unicodetools\\org\\unicode\\props in-place (to navigate easily from one
    directory to the other, simply delete the duplicate unicodetools in the
    path)
*   For older properties, also edit UCD_Names.java and UCD_Types.java in
    unicodetools\\org\\unicode\\text\\UCD; newer properties only use the props
    mechanism
*   Run Main.java to regenerate the derived UCD files
*   Compare the generated PropertyAliases.txt and PropertyValueAliases.txt with
    the hand-edited ones

### ***(8d) Run the invariant tests***

In Package Explorer, locate the file TestUnicodeInvariants.java in
unicodetools\\org\\unicode\\text\\UCD in the unicodetools-trunk tree. (A fully
qualified path may look like
C:\\Work\\svn\\unicodetools-trunk\\unicodetools\\org\\unicode\\text\\UCD\\TestUnicodeInvariants.java.)
Create a Run or Debug Configuration the same way as in step (**7b**), with the
same VM arguments:

Right-click on TestUnicodeInvariants.java and click Run As » Run
Configurations... or Debug As » Debug configurations... Double-click on Java
Application and set the following settings:

*   On the Main tab, check Stop in main (for debugging).
*   On the Arguments tab, set Program arguments: -fUnicodeInvariantTest.txt.

The file UnicodeInvariantTest.txt is the test data. It is located in the same
directory as TestUnicodeInvariants.java.

The data syntax is documented in comments in the test source file itself.

Note the file name. There are some older test source data files there.

To run a smaller, custom set of tests, create a different test source data file
and pass it via the -f argument.

*   On the Arguments tab, set VM arguments with the directories where
    unicodetools-trunk and cldr-trunk were created, e.g.,

-DOTHER_WORKSPACE=C:\\Work\\svn\\unicodetools-trunk

-DSVN_WORKSPACE=C:\\Work\\svn\\unicodetools-trunk\\unicodetools

-DUCD_DIR=C:\\Work\\svn\\unicodetools-trunk\\unicodetools\\data

-DCLDR_DIR=C:\\Work\\svn\\cldr-trunk

Apply and close without running yet.

The VM settings can be set globally in the JRE default VM arguments or in each
Run or Debug configuration.

All settings are saved in an XML Eclipse configuration file in the current
workspace, e.g.,
C:\\Work\\svn\\.metadata\\.plugins\\org.eclipse.debug.core\\.launches\\TestUnicodeInvariants.launch.
Save that file for an easier migration when installing the Tools on a different
machine.

An alternative way of creating a Run or Debug Configuration is to open Run
Configurations... or Debug configurations..., right-click on Java Application \\
Main created in step (**7b**), click on Duplicate, and edit its name and Program
arguments.

See <https://sites.google.com/site/unicodetools/#TOC-Invariant-Checking> for
additional details on running the invariant tests.
