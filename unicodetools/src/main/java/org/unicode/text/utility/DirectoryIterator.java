package org.unicode.text.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;


public class DirectoryIterator {

    private File baseDirectory;
    private Iterator fileList;
    private DirectoryIterator subdirectory = null;
    private FileFilter filter = null;

    static private Comparator reverseComparator =
            new Comparator() {
        @Override
        public int compare(Object a, Object b) {
            return ((Comparable) b).compareTo(a);
        }
    };

    public DirectoryIterator(File directory, FileFilter filter) {
        setDirectory(directory);
        setFilter(filter);
    }

    public DirectoryIterator(String directory, FileFilter filter) {
        setDirectory(new File(directory));
        setFilter(filter);
    }

    public DirectoryIterator(File directory) {
        setDirectory(directory);
    }

    public DirectoryIterator(String directory) {
        setDirectory(new File(directory));
    }

    public void reset() {
        setDirectory(baseDirectory);
    }

    public void setDirectory(File directory) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }
        baseDirectory = directory;
        final Set tempSet = new TreeSet(reverseComparator);
        tempSet.addAll(java.util.Arrays.asList(directory.list()));
        fileList = tempSet.iterator();
    }

    public File getDirectory() {
        return baseDirectory;
    }

    public void setFilter(FileFilter filter) {
        this.filter = filter;
    }

    public FileFilter getFilter() {
        return filter;
    }

    /** Returns null when done
     */
    public File next() {
        File file = null;
        while (true) {
            if (subdirectory != null) {
                file = subdirectory.next();
                if (file != null) {
                    return file;
                }
                subdirectory = null; // null out when done
            }
            if (!fileList.hasNext()) {
                return null;
            }
            final String filestr = (String)fileList.next();
            file = new File(baseDirectory, filestr);
            if (file.isDirectory()) {
                subdirectory = new DirectoryIterator(file, filter);
                continue;
            }
            if (filter == null) {
                return file;
            }
            if (filter.accept(file)) {
                return file;
            }
        }
    }

    /**
     * Returns the part before any '.' or '-' in the file name, without directory
     */
    static public String getRoot(File f) {
        final String s = f.getName();
        int dotPos = s.indexOf('.');
        if (dotPos < 0) {
            dotPos = s.length();
        }
        int dashPos = s.indexOf('-');
        if (dashPos < 0) {
            dashPos = s.length();
        }
        if (dotPos < dashPos) {
            dashPos = dotPos;
        }
        return s.substring(0,dashPos);
    }

    public static class RootFileFilter implements FileFilter {
        String root;
        public RootFileFilter(String root) {
            setRoot(root);
        }
        public void setRoot(String root) {
            this.root = root;
        }
        public String getRoot() {
            return root;
        }
        @Override
        public boolean accept(File f) {
            return DirectoryIterator.getRoot(f).equals("DerivedCoreProperties");
        }
        public String getDescription() {
            return "Root is '" + root + "'";
        }
        @Override
        public String toString() {
            return getDescription();
        }
    };


    static public void test() {
        final File testDir = new File(Settings.UnicodeTools.UCD_DIR);
        DirectoryIterator di;

        di = new DirectoryIterator(testDir, new RootFileFilter("DerivedBinaryProperties"));
        System.out.println();
        System.out.println("Filter: " + di.getFilter());
        while (true) {
            final File f = di.next();
            if (f == null) {
                break;
            }
            System.out.println(f);
        }

        di.reset();
        di.setFilter(new RootFileFilter("DerivedCoreProperties"));
        System.out.println();
        System.out.println("Filter: " + di.getFilter());
        while (true) {
            final File f = di.next();
            if (f == null) {
                break;
            }
            System.out.println(f);
        }
    }

    static public boolean isAlmostIdentical(File file1, File file2, boolean show) throws IOException {
        final BufferedReader br1 = new BufferedReader(new FileReader(file1), 32*1024);
        final BufferedReader br2 = new BufferedReader(new FileReader(file2), 32*1024);
        try {
            for (int lineCount = 0; ; ++lineCount) {
                final String line1 = br1.readLine();
                final String line2 = br2.readLine();
                if (line1 == null) {
                    if (line2 == null) {
                        return true;
                    }
                    if (show) {
                        Utility.fixDot();
                        System.out.println("Found difference in : " + file1 + ", " + file2);
                        System.out.println(" Line1: " + line1);
                        System.out.println(" Line2: " + line2);
                    }
                    return false;
                }
                if (!line1.equals(line2)) {
                    if (line1.startsWith("# Generated") && line2.startsWith("# Generated")) {
                        continue;
                    }
                    if (line1.startsWith("# Date") && line2.startsWith("# Date")) {
                        continue;
                    }
                    if (lineCount == 0 && line1.startsWith("#") && line2.startsWith("#")) {
                        continue;
                    }
                    if (show) {
                        Utility.fixDot();
                        System.out.println("Found difference in : " + file1 + ", " + file2);
                        System.out.println(" Line1: " + line1);
                        System.out.println(" Line2: " + line2);
                    }
                    return false;
                }
            }
        } finally {
            br1.close();
            br2.close();
        }
    }

}
