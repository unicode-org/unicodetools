package org.unicode.jsp;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class Globe {
    public static final boolean DEBUG = false;
    static int style = 0;
    static int degreeInterval = 15;

    static String myPictures0 = "bin/jsp/images/";
    static String SOURCE_DIR = myPictures0;
    static String TARGET_DIR = "Pictures/Earth/";

    static JFileChooser loadFileChooser = new JFileChooser();
    static JFileChooser saveFileChooser = new JFileChooser();

    static {
        saveFileChooser.setCurrentDirectory(new File(TARGET_DIR));
        loadFileChooser.setCurrentDirectory(new File(SOURCE_DIR));
    }

    static int QUALITY = 80;

    static double lightness = 0;
    static boolean rotate = false;
    static boolean doLabels = false;

    static int originChoice = 0;
    static String[] originList =
            new String[] {
                "?",
                "North Pole",
                "San Francisco (SFO)",
                "Zürich (ZRH)",
                "Tokyo (NRT)",
                "Wellington (WLG)",
                "Honolulu",
                "Melbourne (MEL)",
                "Caen (CFR)",
                "Cochin (COK)",
                "Cochin (COK) - centering",
                "Moscow, ID"
            };
    // Melbourne, Australia 37 47 S 144 58 E
    // Caen — 49° 10' 59" N 00° 22' 10" W
    // sundance latitude 44.406 and longitude -104.376
    // San Diego, Calif. 32 42 117 10 9:00 a.m.
    // Moscow, ID Latitude: 46.73 N, Longitude: 117.00 W

    static double[][] origins = { // lat, long
        {-Math.PI / 2 + 0.0001, 0.0001}, //
        {-Math.PI / 2 + 0.0001, 0.0001}, //
        {
            Navigator.toRadians(37.0, 37.0, 8.3, false),
            Navigator.toRadians(122.0, 22.0, 29.6, false)
        }, // sf //
        {
            Navigator.toRadians(47, 27, 0, false), Navigator.toRadians(8.0, 33.0, 0, true)
        }, // zurich //
        {
            Navigator.toRadians(35, 45, 50, false), Navigator.toRadians(140.0, 23.0, 30, true)
        }, // Narita 35°45´50"N 140°23´30"E
        {
            Navigator.toRadians(41, 20, 0, true), Navigator.toRadians(174.0, 48.0, 0, true)
        }, // Wellington 41° 20' 0" S 174° 48' 0" E
        {Navigator.toRadians(21, 18, 0, false), Navigator.toRadians(157, 50, 0, false)},
        {Navigator.toRadians(37, 39, 42, true), Navigator.toRadians(144, 50, 0, true)},
        {Navigator.toRadians(49, 10, 24, false), Navigator.toRadians(0, 26, 53, false)},
        {Navigator.toRadians(10, 9, 7, false), Navigator.toRadians(76, 24, 7, true)}, // Cochin
        {Navigator.toRadians(0, 0, 0, false), Navigator.toRadians(70, 0, 0, true)}, // Cochin
        {
            Navigator.toRadians(46.743978, 0, 0, false),
            Navigator.toRadians(116.904176, 0, 0, false)
        }, // Moscow
        // ,-116.904176
        /*
             *  Airport Code  : COK

        Longitude : 76° 24’ 7” E (?)
        Latitude : 10° 9’ 7” N (?)
             */
    };

    static int[][] sizeValues = {
        {640, 320},
        {1024, 512},
        {1280, 640},
        {1280, 1024},
        {1400, 700},
        {1400, 1050},
        {1440, 720},
        {1600, 800},
        {1920, 960},
        {1920, 1200},
        {2400, 1200},
    };

    static String[] sizeList =
            new String[] {
                "640×320",
                "1024×512",
                "1280×640",
                "1280×1024",
                "1400×700",
                "1400×1050",
                "1440×720",
                "1600×800",
                "1920×960",
                "1920×1200",
                "2400×1200"
            };
    static int sizeChoice = 0;

    static String[] gridList = new String[] {"5°", "10°", "15°"};
    static int gridChoice = 0;

    static String[] labelList = new String[] {"no labels", "labels"};

    static String[] localeList = new String[] {"en", "de", "fr", "el", "ru", "ja", "zh"};
    static String[] translatedLocaleList;

    static String[] projectionList =
            new String[] {
                "Plate Carrée",
                "Equal Area Rectangular (Gall)",
                "Equal Area Sinusoidal",
                "Equal Area Ellipse",
                "Equidistant Conic",
                "3D Isometric"
            };
    static int projectionChoice = 0;

    static Transform[] projectionValues =
            new Transform[] {
                new TransformPlateCarree(),
                new TransformGallOrthographic(),
                new TransformSinusoidal(),
                new TransformEqualAreaEllipse(),
                new TransformEquidistantConic(),
                new Transform3DIsometric(),
            };

    static double originLat = origins[0][0]; // N = +
    static double originLong = origins[0][1]; // W = -

    /**
     * Create the GUI and show it. For thread safety, this method should be invoked from the
     * event-dispatching thread.
     *
     * @throws IOException
     */
    static JLabel mainPicture = new JLabel();

    // static ImageIcon sourceIcon, resultIcon;
    static JFrame frame;

    static BufferedImage sourceImage, griddedImage;
    static Image transformedImage;

    static int gradations = 10;

    private static void createAndShowGUI() {
        if (false) {
            Mapper m = new Mapper(3, 7, 100, 140);
            System.out.println(m.map(3) + ", " + m.map(7));
            new Transform.Tester().test();
            return;
        }

        // cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY,".*");

        // Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        // Create and set up the window.
        frame = new JFrame("HelloWorldSwing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add the ubiquitous "Hello World" label.
        // JLabel label = new JLabel("Hello World");
        // frame.getContentPane().add(label);
        String sname = SOURCE_DIR + "earth-living.jpg";
        // "ev11656_land_shallow_topo_8192.tiff";
        // "ev11656_land_shallow_topo_8192.PNG";
        // earthmap1k.jpg";
        // "C:/Documents and Settings/Administrator/Desktop/macchiato-backup/distance/worldmap.jpg"
        loadSourceMap(sname);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEADING));

        JButton but = new JButton("Save");
        but.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        saveFileChooser.setSelectedFile(
                                new File(
                                        "Earth "
                                                + projectionList[projectionChoice]
                                                + ", "
                                                + sizeList[sizeChoice]
                                                + ", "
                                                + gridList[gridChoice]
                                                + ", "
                                                + originList[originChoice]
                                                + ".jpg"));
                        int returnVal = saveFileChooser.showSaveDialog(frame);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            try {
                                String filename =
                                        saveFileChooser.getSelectedFile().getCanonicalPath();
                                System.out.println("You chose to save this file: " + filename);
                                writeImage(griddedImage, filename, QUALITY);
                                // myPictures + "new-earth-living" + style + ".jpg"
                            } catch (IOException e1) {
                                System.out.println("Couldn't save file.");
                            }
                        }
                    }
                });
        topPanel.add(but);

        JButton but2 = new JButton("Load");
        but2.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {

                        //        File file = new File(SOURCE_DIR + "earth-living.jpg");
                        //        try {
                        //          System.out.println("Source Dir: " + file.getCanonicalPath());
                        //        } catch (IOException e2) {
                        //        }
                        //        loadFileChooser.setSelectedFile(file);
                        int returnVal = loadFileChooser.showOpenDialog(frame);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            try {
                                String filename =
                                        loadFileChooser.getSelectedFile().getCanonicalPath();
                                if (!filename.toLowerCase().endsWith(".jpg")) {
                                    filename += ".jpg";
                                }
                                System.out.println("You chose to open this file: " + filename);
                                loadSourceMap(filename);
                                // myPictures + "new-earth-living" + style + ".jpg"
                            } catch (IOException e1) {
                                System.out.println("Couldn't save file.");
                            }
                        }
                    }
                });
        topPanel.add(but2);

        JComboBox box = new JComboBox(projectionList);
        box.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int index = ((JComboBox) e.getSource()).getSelectedIndex();
                        if (index != projectionChoice) {
                            style = projectionChoice = index;
                            changeImage(frame);
                        }
                    }
                });
        topPanel.add(box);

        JComboBox box2 = new JComboBox(sizeList);
        box2.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int index = ((JComboBox) e.getSource()).getSelectedIndex();
                        if (index != sizeChoice) {
                            sizeChoice = index;
                            changeImage(frame);
                        }
                    }
                });
        topPanel.add(box2);
        String[] gradationNames = new String[gradations * 2 - 1];
        for (int i = 0; i < gradationNames.length; ++i) {
            gradationNames[i] =
                    i < gradations - 1
                            ? "Lighten to " + ((i + 1) * 100 / gradations)
                            : i == gradations - 1
                                    ? "Neutral"
                                    : "Darken to " + ((gradations * 2 - i - 1) * 100 / gradations);
        }
        JComboBox box3 = new JComboBox(gradationNames);
        box3.setSelectedIndex(gradations - 1);
        box3.addActionListener(
                new ActionListener() {
                    int lastIndex = gradations - 1;

                    public void actionPerformed(ActionEvent e) {
                        int index = ((JComboBox) e.getSource()).getSelectedIndex();
                        if (index != lastIndex) {
                            lastIndex = index;
                            lightness = (gradations - 1 - index) / (double) gradations;
                            changeImage(frame);
                        }
                    }
                });
        topPanel.add(box3);

        JComboBox box4 = new JComboBox(originList);
        // box4.setSelectedIndex(1);
        box4.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int index = ((JComboBox) e.getSource()).getSelectedIndex();
                        if (index != originChoice) {
                            originChoice = index;
                            originLat = origins[index][0];
                            originLong = origins[index][1];
                            if (projectionValues[projectionChoice].usesOrigin()) {
                                changeImage(frame);
                            } else {
                                addGrid(transformedImage, projectionValues[projectionChoice]);
                                //
                            }
                        }
                    }
                });
        topPanel.add(box4);

        JComboBox box5 = new JComboBox(gridList);
        // box4.setSelectedIndex(1);
        box5.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int index = ((JComboBox) e.getSource()).getSelectedIndex();
                        if (index != gridChoice) {
                            gridChoice = index;
                            degreeInterval = (index + 1) * 5;
                            addGrid(transformedImage, projectionValues[projectionChoice]);
                            // changeImage(frame);
                        }
                    }
                });
        topPanel.add(box5);

        JComboBox box6 = new JComboBox(labelList);
        // box4.setSelectedIndex(1);
        box6.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int index = ((JComboBox) e.getSource()).getSelectedIndex();
                        if ((index == 1) != doLabels) {
                            doLabels = index == 1;
                            addGrid(transformedImage, projectionValues[projectionChoice]);
                            // changeImage(frame);
                        }
                    }
                });
        topPanel.add(box6);

        box7 = new JComboBox(localeList);
        box7.setFont(font);
        // setLocale(0);
        // box4.setSelectedIndex(1);
        box7.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int index = ((JComboBox) e.getSource()).getSelectedIndex();
                        if (index != currentLocaleIndex) {
                            // setLocale(index);
                            ((JComboBox) e.getSource()).setSelectedIndex(index);
                            changeImage(frame);
                        }
                    }
                });
        topPanel.add(box7);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(topPanel, BorderLayout.PAGE_START);
        panel.add(mainPicture, BorderLayout.CENTER);
        panel.add(
                new JLabel("See http://www.3dsoftware.com/Cartography/USGS/MapProjections/"),
                BorderLayout.PAGE_END);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(660, 540));
        // add(scrollPane, BorderLayout.CENTER);

        frame.getContentPane().add(scrollPane);

        // Display the window.
        frame.pack();
        frame.setVisible(true);
        // writeImage(i, myPictures + "new-earth-living" + style + ".jpg", QUALITY);
    }

    // private static void setLocale(int newLocaleIndex) {
    // cldrFile = cldrFactory.make(localeList[newLocaleIndex], true);
    // tzf = new TimezoneFormatter(cldrFactory, localeList[newLocaleIndex], true);
    // currentLocaleIndex = newLocaleIndex;
    // MutableComboBoxModel model = (MutableComboBoxModel) box7.getModel();
    // for (int i = 0; i < localeList.length; ++i) {
    // model.removeElementAt(i);
    // model.insertElementAt(cldrFile.getName(localeList[i], true), i);
    // }
    // }
    static Font font = Font.decode("Arial Unicode MS-9");
    // static CLDRFile.Factory cldrFactory;
    // static CLDRFile cldrFile;
    static int currentLocaleIndex = -1;
    // static TimezoneFormatter tzf;
    static JComboBox box7;

    /*
     public static class LightingImageFilter implements RGBImageFilter {
         /**
    * Subclasses must specify a method to convert a single input pixel
    * in the default RGB ColorModel to a single output pixel.
    * @param x,&nbsp;y the coordinates of the pixel
    * @param rgb the integer pixel representation in the default RGB
    *            color model
    * @return a filtered pixel in the default RGB color model.
    * @see ColorModel#getRGBdefault
    * @see #filterRGBPixels
    * /
         public int filterRGB(int x, int y, int rgb) {
             int a = (rgb >>> 24) & 0xFF;
             int r = (rgb >> 16) & 0xFF;
             int g = (rgb >> 8) & 0xFF;
             int b = rgb & 0xFF;
             return (rgb & 0xFF)

         }
     }
    */

    /**
     * @param sname
     */
    private static void loadSourceMap(String sname) {
        try {
            System.out.println("Check: " + new File(sname).getAbsolutePath());
            ImageIcon sourceIcon = new ImageIcon(sname);
            sourceImage = convertToBuffered(sourceIcon.getImage());
            System.out.println("Loaded " + new File(sname).getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Can't load");
        }
        changeImage(frame);
    }
    /**
     * @param frame
     */
    private static final boolean DEBUG_ICON = false;

    private static void changeImage(JFrame frame) {
        if (DEBUG_ICON) {
            System.out.println("Changing Icon1");
        }
        // System.out.println("Width " + ii.getIconWidth() + ", Height: " + ii.getIconHeight());
        DeformFilter filter =
                new DeformFilter(
                        sourceImage.getWidth(),
                        sourceImage.getHeight(),
                        sizeValues[sizeChoice][0],
                        sizeValues[sizeChoice][1],
                        projectionValues[projectionChoice]);
        // ImageFilter filter = new RotateFilter(Math.PI / 4);

        if (DEBUG_ICON) {
            System.out.println("Changing Icon2");
        }
        ImageProducer ip =
                new FilteredImageSource(sourceImage.getSource(), filter); // modifies filter
        if (DEBUG_ICON) {
            System.out.println("Changing Icon3");
        }
        transformedImage = frame.createImage(ip);
        if (DEBUG_ICON) {
            System.out.println("Changing Icon4");
            // Icon junk = new ImageIcon(transformedImage); // load image (HACK)
        }

        if (DEBUG_ICON) {
            System.out.println("Changing Icon5");
        }
        addGrid(transformedImage, projectionValues[projectionChoice]);
    }

    public static void main(String[] args) throws IOException {
        readData();
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        createAndShowGUI();
                    }
                });
    }

    static void readData() throws IOException {
        File file = new File("classes/jsp/");
        System.out.println(file.getAbsolutePath());

        BufferedReader br =
                new BufferedReader(
                        new InputStreamReader(new FileInputStream("bin/jsp/Globe.txt"), "UTF-8"));
        // BagFormatter.openUTF8Reader("classes/jsp/", "Globe.txt");
        String pat =
                "([^;]+) \\s* [;] \\s* "
                        + "([0-9.]+) [°]* \\s* ([0-9.]+)? [']* \\s* ([0-9.]+)? [\"]* \\s* "
                        + "([NS]) \\s* "
                        + "([0-9.]+) [°]* \\s* ([0-9.]+)? [']* \\s* ([0-9.]+)? [\"]* \\s* "
                        + "([EW]) \\s*";
        Matcher m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE | Pattern.COMMENTS).matcher("");
        System.out.println("Pattern: " + pat);
        List nameData = new ArrayList();
        List posData = new ArrayList();
        String[] pieces = new String[3];
        while (true) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            if (!m.reset(line).matches()) {
                System.out.println("Error in data: " + line);
                continue;
            }
            nameData.add(m.group(1));
            double latitude =
                    Navigator.toRadians(
                            Double.parseDouble(m.group(2)),
                            m.group(3) != null ? Double.parseDouble(m.group(3)) : 0,
                            m.group(4) != null ? Double.parseDouble(m.group(4)) : 0,
                            m.group(5).equalsIgnoreCase("S"));
            double longitude =
                    Navigator.toRadians(
                            Double.parseDouble(m.group(6)),
                            m.group(7) != null ? Double.parseDouble(m.group(7)) : 0,
                            m.group(8) != null ? Double.parseDouble(m.group(8)) : 0,
                            m.group(9).equalsIgnoreCase("E"));
            posData.add(new double[] {latitude, longitude});
            System.out.println(m.group(1) + ", " + latitude + ", " + longitude);
        }
        originList = (String[]) nameData.toArray(originList);
        origins = (double[][]) posData.toArray(origins);
        br.close();
    }

    public static final NumberFormat nf = NumberFormat.getInstance();

    public static void getAntipode(DPoint in) {
        if (in.x > 0) {
            in.x -= Math.PI;
        } else {
            in.x += Math.PI;
        }
        in.y = -in.y;
    }

    public static class DPoint implements Comparable {
        double x, y;

        DPoint() {
            this(0, 0);
        }

        DPoint(double x, double y) {
            set(x, y);
        }

        DPoint set(double x, double y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public int compareTo(Object o) {
            DPoint that = (DPoint) o;
            if (y < that.y) {
                return -1;
            }
            if (y > that.y) {
                return 1;
            }
            if (x < that.x) {
                return -1;
            }
            if (x > that.x) {
                return 1;
            }
            return 0;
        }

        public String toString() {
            return "[" + nf.format(x) + "," + nf.format(y) + "]";
        }
    }

    public static class DRectangle {
        double x0, y0, x1, y1;
    }

    public static class Quad {
        DRectangle containing = new DRectangle();
        DPoint[] p = new DPoint[4];

        // returns the amount (0..1) that the square from x,y to x+1, y+1
        // overlaps the quadralateral
        void set(DPoint a, DPoint b, DPoint c, DPoint d) {
            p[0] = a;
            p[1] = b;
            p[2] = c;
            p[3] = d;
            // sort; so y's are now in sorted order
            Arrays.sort(p);
            // integer bounding rectangle
            // is easy for y's
            containing.y0 = (int) Math.floor(p[0].y);
            containing.y1 = (int) Math.ceil(p[3].y);
            // but for x's we have to compute
            containing.x0 =
                    (int) Math.floor(Math.min(p[0].x, Math.min(p[1].x, Math.min(p[2].x, p[3].x))));
            containing.x1 =
                    (int) Math.ceil(Math.max(p[0].x, Math.max(p[1].x, Math.max(p[2].x, p[3].x))));
        }

        double getWeight(double x, double y) {
            // return the percentage overlap between this quadralateral,
            // and the rectangle from x,y to x+1,y+1
            // simple implementation for now. return 1 if center is in containing, otherwise 0
            if (containing.x0 <= x
                    && x < containing.x1
                    && containing.y0 <= y
                    && y < containing.y1) {
                return 1.0;
            }
            return 0;
        }
    }

    public abstract static class Transform {
        static final boolean debug = false;
        protected double srcW, srcH, dstW, dstH;
        Mapper srcW_long, srcH_lat, long_dstW, lat_dstH;
        Navigator navigator;
        boolean allowRotation = true;
        Shape clip = null;

        /**
         * @return Returns the clip.
         */
        public Shape getClip() {
            if (clip == null) {
                clip = _getClip();
            }
            return clip;
        }
        /**
         * @return
         */
        public boolean usesOrigin() {
            return false;
        }
        // must set before use
        Transform set(double srcW, double srcH, double dstW, double dstH) {
            this.srcW = srcW;
            this.srcH = srcH;
            this.dstW = dstW;
            this.dstH = dstH;
            srcW_long = new Mapper(0, srcW, -Math.PI, Math.PI);
            srcH_lat = new Mapper(0, srcH, -Math.PI / 2, Math.PI / 2);
            long_dstW = new Mapper(-Math.PI, Math.PI, 0, dstW);
            lat_dstH = new Mapper(-Math.PI / 2, Math.PI / 2, 0, dstH);
            navigator = new Navigator().setLat1Lon1(originLat, originLong);
            clip = null;
            return this;
        }
        // Remember that the coordinate system is upside down so apply
        // the transform as if the angle were negated.
        // cos(-angle) =  cos(angle)
        // sin(-angle) = -sin(angle)
        public final boolean transform(double x, double y, DPoint retcoord) {
            retcoord.x = srcW_long.map(x);
            retcoord.y = srcH_lat.map(y);
            if (allowRotation && rotate) {
                navigator.setLat2Lon2(retcoord.y, retcoord.x);
                double dist = navigator.getDistance();
                double course = -navigator.getCourse();
                double offset = Math.PI / 2;
                if (dist > Math.PI / 2) {
                    dist = Math.PI - dist;
                    offset = -offset;
                    course = -course;
                }
                navigator.setLat2Lon2(retcoord.y, retcoord.x);
                retcoord.x = dist;
                retcoord.y = course;
            }
            _transform(retcoord);
            retcoord.x = long_dstW.map(retcoord.x);
            retcoord.y = lat_dstH.map(retcoord.y);
            return retcoord.x >= 0.0 && retcoord.x <= dstW && retcoord.y >= 0 && retcoord.y <= dstH;
        }
        // Remember that the coordinate system is upside down so apply
        // the transform as if the angle were negated.  Since inverting
        // the transform is also the same as negating the angle, itransform
        // is calculated the way you would expect to calculate transform.
        public final boolean itransform(double x, double y, DPoint retcoord) {
            retcoord.x = long_dstW.back(x);
            retcoord.y = lat_dstH.back(y);
            _itransform(retcoord);
            if (allowRotation && rotate) {
                // retcoord.x = Navigator.wrap(retcoord.x + originLong, -Math.PI, Math.PI);
                // retcoord.x = Navigator.wrap(retcoord.x, -Math.PI, Math.PI);
                // System.out.println();
                // System.out.println("lat: " + Navigator.degrees(retcoord.y) + ", lon:" +
                // Navigator.degrees(retcoord.x));
                navigator.setDistanceCourse(retcoord.y, retcoord.x);
                retcoord.y = navigator.getLat2();
                retcoord.x = -navigator.getLon2();
                // System.out.println("lat: " + Navigator.degrees(retcoord.y) + ", lon:" +
                // Navigator.degrees(retcoord.x));
            }
            retcoord.x = srcW_long.back(retcoord.x);
            retcoord.y = srcH_lat.back(retcoord.y);
            return retcoord.x >= 0.0 && retcoord.x <= srcW && retcoord.y >= 0 && retcoord.y <= srcH;
        }
        /**
         * @param input and output: latitude in y (radians from -pi/2 to pi/2) and longitude in x
         *     (radians from -pi to pi)
         */
        protected abstract void _transform(DPoint retcoord);
        /**
         * @param input and output: latitude in y (radians from -pi/2 to pi/2) and longitude in x
         *     (radians from -pi to pi)
         */
        protected abstract void _itransform(DPoint retcoord);

        protected abstract Shape _getClip();
        /**
         * @param style
         * @return
         */
        public static class Tester {
            Transform trans;
            DPoint retcoord = new DPoint();
            DPoint minX, minY, maxX, maxY;

            void test() {
                for (int k = 0; k < projectionValues.length; ++k) {
                    test(projectionValues[k]);
                }
            }

            private void test(Transform trans) {
                System.out.println("Testing: " + trans.getClass().getName());
                // check that points in the source rectangle map to the target rectangle
                trans.set(10, 10, 100, 150);
                int counter = 0;
                minX = new DPoint(Double.MAX_VALUE, Double.MAX_VALUE);
                minY = new DPoint(Double.MAX_VALUE, Double.MAX_VALUE);
                maxX = new DPoint(Double.MIN_VALUE, Double.MIN_VALUE);
                maxY = new DPoint(Double.MIN_VALUE, Double.MIN_VALUE);
                double pLong = trans.srcW_long.back(originLong);
                double pLat = trans.srcH_lat.back(originLat);

                trans.transform(pLong, pLat, retcoord);
                trans.transform(trans.srcW / 2, trans.srcH / 2, retcoord);
                for (double x = 0; x < trans.srcW; ++x) {
                    for (double y = 0; y < trans.srcH; ++y) {
                        counter++;
                        trans.transform(x, y, retcoord);
                        double x2 = retcoord.x;
                        double y2 = retcoord.y;
                        if (x2 < minX.x) {
                            minX.set(x2, y2);
                        }
                        if (x2 > maxX.x) {
                            maxX.set(x2, y2);
                        }
                        if (y2 < minY.y) {
                            minY.set(x2, y2);
                        }
                        if (y2 > maxY.y) {
                            maxY.set(x2, y2);
                        }
                        if (0 <= x2 && x2 < trans.dstW && 0 <= y2 && y2 < trans.dstH) {
                            trans.itransform(x2, y2, retcoord);
                            double x3 = retcoord.x;
                            double y3 = retcoord.y;
                            if (Math.abs(x - x3) > 0.001 || Math.abs(y - y3) > 0.001) {
                                System.out.println(
                                        "Error: " + counter + "\t" + x + ", " + y + " => " + x2
                                                + ", " + y2 + " => " + x3 + ", " + y3);
                            }
                        }
                    }
                }
                System.out.println(
                        "\t minX "
                                + minX
                                + ",\t maxX "
                                + maxX
                                + ",\t minY "
                                + minY
                                + ",\t maxY "
                                + maxY);
            }
        }
    }

    public static class TransformPlateCarree extends Transform {
        public void _transform(DPoint retcoord) {
            // nothing
        }

        public void _itransform(DPoint retcoord) {
            // nothing
        }
        /* (non-Javadoc)
         * @see Globe.Transform#_getClip()
         */
        protected Shape _getClip() {
            return new Rectangle.Double(0, 0, dstW, dstH);
        }
    }

    public static class TransformSinusoidal extends Transform {
        public void _transform(DPoint retcoord) {
            retcoord.x = retcoord.x * Math.cos(retcoord.y);
        }

        public void _itransform(DPoint retcoord) {
            if (!(-Math.PI <= retcoord.x && retcoord.x <= Math.PI)) {
                retcoord.x = Double.NaN;
                return;
            }
            retcoord.x = retcoord.x / Math.cos(retcoord.y);
        }

        protected Shape _getClip() {
            GeneralPath p = new GeneralPath();
            p.moveTo((float) (dstW / 2), 0);
            double limitx = srcW_long.map(0);
            for (int i = 1; i <= dstH; ++i) {
                double y = lat_dstH.back(i);
                // System.out.println(i + ", " + y + ", " + Math.cos(y));
                double x = limitx * Math.cos(y);
                x = long_dstW.map(x);
                p.lineTo((float) x, i);
            }
            limitx = srcW_long.map(srcW);
            for (int i = (int) dstH - 1; i >= 0; --i) {
                double y = lat_dstH.back(i);
                double x = limitx * Math.cos(y);
                x = long_dstW.map(x);
                p.lineTo((float) x, i);
            }
            return p;
        }
    }

    public static class Transform3DIsometric extends Transform {
        static double SHIFT = Math.PI / 3;

        public void _transform(DPoint retcoord) {
            // special shift
            retcoord.x = Navigator.wrap(retcoord.x - SHIFT, -Math.PI, Math.PI);
            // regular stuff
            boolean shift = retcoord.x < -Math.PI / 2 || retcoord.x > Math.PI / 2;
            double offset = shift ? Math.PI / 2 : -Math.PI / 2;
            double cosy = Math.cos(retcoord.y);
            retcoord.y = Math.sin(retcoord.y) * (Math.PI / 2);
            retcoord.x = Math.sin(retcoord.x) * cosy * Math.PI / 2 + offset;
            if (shift) {
                retcoord.x = Math.PI - retcoord.x;
            }
        }

        public void _itransform(DPoint retcoord) {
            retcoord.x *= 2;
            if (retcoord.x < 0) {
                retcoord.x += Math.PI;
                retcoord.y = Math.asin(retcoord.y / (Math.PI / 2));
                retcoord.x = Math.asin(retcoord.x / Math.cos(retcoord.y) / Math.PI);
            } else {
                retcoord.x -= Math.PI;
                retcoord.y = Math.asin(retcoord.y / (Math.PI / 2));
                retcoord.x = Math.asin(retcoord.x / Math.cos(retcoord.y) / Math.PI);
                retcoord.x += Math.PI;
                if (retcoord.x > Math.PI) {
                    retcoord.x -= 2 * Math.PI;
                }
            }
            retcoord.x = Navigator.wrap(retcoord.x + SHIFT, -Math.PI, Math.PI);
        }

        protected Shape _getClip() {
            GeneralPath p = new GeneralPath(new Ellipse2D.Double(0, 0, dstW / 2, dstH));
            p.append(new Ellipse2D.Double(dstW / 2, 0, dstW / 2, dstH), false);
            return p;
        }
    }

    public static class TransformEqualAreaEllipse extends Transform {
        boolean debugTemp = false;

        TransformEqualAreaEllipse() {
            if (debugTemp) {
                double[][] tests = {
                    {0, -Math.PI / 2},
                    {0, -Math.PI / 4},
                    {0, -Math.PI / 8},
                    {0, 0},
                    {0, Math.PI / 8},
                    {0, Math.PI / 4},
                    {0, Math.PI / 2}
                };
                for (int i = 0; i < tests.length; ++i) {
                    DPoint p = new DPoint(tests[i][0], tests[i][1]);
                    System.out.println(p);
                    _itransform(p);
                    System.out.println(" => " + p);
                }
                for (double x = -1; x <= 1; x += 0.1) {
                    double y = oddFunction(x);
                    double xx = inverseOddFunction(y);
                    System.out.println("x: " + x + "\ty: " + y + "\txx: " + xx);
                }
                debugTemp = false;
            }
        }
        // Area of a spherical cap is 2 pi r^2 (1-sin(lat))
        // Area of a circular segment is r^2 ( acos(p) - p sqrt(1-p^2)), where p = dist to chord/r
        // Thus we get the itransform easily:
        // asin(2/pi (acos p - p sqrt(1-p^2))
        public void _transform(DPoint retcoord) {
            double temp2 = Math.PI / 2 * (1 - Math.sin(retcoord.y));
            double p = inverseOddFunction(temp2);
            retcoord.y = (Math.PI / 2) * p;
            double temp = Math.sqrt(1 - p * p);
            retcoord.x = temp * retcoord.x;
        }

        public void _itransform(DPoint retcoord) {
            double p = retcoord.y / (Math.PI / 2);
            if (debugTemp) {
                System.out.println("\tp:\t" + p);
            }
            double temp = Math.sqrt(1 - p * p);
            if (debugTemp) {
                System.out.println("\ttemp:\t" + temp);
            }
            double temp2 = (Math.acos(p) - p * temp);
            if (debugTemp) {
                System.out.println("\ttemp2:\t" + temp2);
            }
            double newy = Math.asin(1 - (2 / Math.PI) * temp2);
            if (debugTemp) {
                System.out.println("\tnewy:\t" + newy);
            }
            double newx = retcoord.x / temp;
            retcoord.y = newy;
            retcoord.x = newx;
        }

        protected Shape _getClip() {
            return new Ellipse2D.Double(0, 0, dstW, dstH);
        }
        /**
         * @param in -1..1
         * @return value in 0..PI
         */
        public double oddFunction(double p) {
            double temp = Math.sqrt(1 - p * p);
            return (Math.acos(p) - p * temp);
        }

        public double oddFunctionDerivative(double p) {
            double temp = Math.sqrt(1 - p * p);
            return (-2 - p + p * p) / temp;
        }

        static final double epsilon = 0.0001;
        final double lowValue = oddFunction(-1);
        final double highValue = oddFunction(1);

        public double inverseOddFunction(double pp) {
            // ugly, have to approximate. Use newton's method.
            double pLow = pp - epsilon;
            double pHigh = pp + epsilon;
            // for the first guess, use high and low bounds
            // (guess - low) / (high - low) = (pp - lowV) / (highV - lowV);
            double guess = -1 + (1 - -1) * (pp - lowValue) / (highValue - lowValue);
            while (true) {
                double p = oddFunction(guess);
                if (pLow < p && p < pHigh) {
                    return guess;
                }
                guess = guess - (p - pp) / oddFunctionDerivative(guess);
                if (debugTemp) {
                    System.out.println("newGuess: " + guess);
                }
            }
        }
    }

    public static class TransformGallOrthographic extends Transform {
        public void _transform(DPoint retcoord) {
            retcoord.y =
                    Math.sin(retcoord.y) * (Math.PI / 2); // transform to -1..1, then -PI/2..PI/2
        }

        public void _itransform(DPoint retcoord) {
            retcoord.y = Math.asin(retcoord.y / (Math.PI / 2)); // transform to -1..1
        }

        protected Shape _getClip() {
            return new Rectangle.Double(0, 0, dstW, dstH);
        }
    }

    public static class TransformEquidistantConic extends Transform {
        {
            allowRotation = false;
        }

        public void _transform(DPoint retcoord) {
            // divide into two cases
            navigator.setLat2Lon2(retcoord.y, retcoord.x);
            double dist = navigator.getDistance();
            double course = -navigator.getCourse();
            double offset = Math.PI / 2;
            if (dist > Math.PI / 2) {
                dist = Math.PI - dist;
                offset = -offset;
                course = -course;
            }
            retcoord.x = Math.sin(course) * dist - offset;
            retcoord.y = Math.cos(course) * dist;
        }

        public void _itransform(DPoint retcoord) {
            double x2 = retcoord.x;
            double y2 = retcoord.y;
            double dist, course;
            if (x2 < 0) {
                x2 += Math.PI / 2; // re-center
                dist = Math.sqrt(x2 * x2 + y2 * y2);
                if (dist > Math.PI / 2) {
                    retcoord.x = Double.NaN;
                    return;
                }
                course = -Math.atan2(x2, y2);
            } else {
                x2 -= Math.PI / 2; // re-center
                dist = Math.sqrt(x2 * x2 + y2 * y2);
                dist = Math.PI - dist;
                if (dist < Math.PI / 2) {
                    retcoord.x = Double.NaN;
                    return;
                }
                course = Math.atan2(x2, y2);
            }
            navigator.setDistanceCourse(dist, course);
            retcoord.y = navigator.getLat2();
            retcoord.x = navigator.getLon2();
        }

        protected Shape _getClip() {
            GeneralPath p = new GeneralPath(new Ellipse2D.Double(0, 0, dstW / 2, dstH));
            p.append(new Ellipse2D.Double(dstW / 2, 0, dstW / 2, dstH), false);
            return p;
        }

        public boolean usesOrigin() {
            return true;
        }
    }

    static class Mapper {
        private double slope, offset;

        Mapper(double sourceMin, double sourceMax, double targetMin, double targetMax) {
            slope = (targetMax - targetMin) / (sourceMax - sourceMin);
            offset = targetMin - slope * sourceMin;
        }

        double map(double in) {
            return in * slope + offset;
        }

        double back(double out) {
            return (out - offset) / slope;
        }
    }

    public static class DeformFilter extends ImageFilter {

        private static ColorModel defaultRGB = ColorModel.getRGBdefault();

        private DPoint coord = new DPoint();

        private int raster[];

        private int xoffset, yoffset;
        private int srcW, srcH;
        private int dstW, dstH;

        int style;

        DeformFilter(int srcW, int srcH, int width, int height, Transform trans) {
            dstW = width;
            dstH = height;
            this.trans = trans;
            trans.set(srcW, srcH, dstW, dstH);
            // this.style = style;
        }

        Transform trans;

        public void transformBBox(Rectangle rect) {
            double minx = Double.POSITIVE_INFINITY;
            double miny = Double.POSITIVE_INFINITY;
            double maxx = Double.NEGATIVE_INFINITY;
            double maxy = Double.NEGATIVE_INFINITY;
            for (int y = 0; y <= 1; y++) {
                for (int x = 0; x <= 1; x++) {
                    trans.transform(rect.x + x * rect.width, rect.y + y * rect.height, coord);
                    minx = Math.min(minx, coord.x);
                    miny = Math.min(miny, coord.y);
                    maxx = Math.max(maxx, coord.x);
                    maxy = Math.max(maxy, coord.y);
                }
            }
            rect.x = (int) Math.floor(minx);
            rect.y = (int) Math.floor(miny);
            rect.width = (int) Math.ceil(maxx) - rect.x + 1;
            rect.height = (int) Math.ceil(maxy) - rect.y + 1;
        }

        public void setDimensions(int width, int height) {
            srcW = width;
            srcH = height;
            Rectangle rect = new Rectangle(0, 0, dstW, dstH);
            xoffset = -rect.x;
            yoffset = -rect.y;
            raster = new int[srcW * srcH];
            consumer.setDimensions(dstW, dstH);

            // for debugging
            debug = false;
            for (int i = 0; i <= rect.width; i += rect.width / 4) {
                for (int j = 0; j <= rect.height; j += rect.height / 4) {
                    trans.transform(i, j, coord);
                    double i2 = coord.x;
                    double j2 = coord.y;
                    trans.itransform(i2, j2, coord);
                    if (debug) {
                        System.out.println(
                                i + ", " + j + "\t=> " + i2 + ", " + j2 + "\t=> " + coord.x + ", "
                                        + coord.y);
                    }
                }
            }
            debug = false;
        }

        static boolean debug = false;

        public void setColorModel(ColorModel model) {
            consumer.setColorModel(defaultRGB);
        }

        public void setHints(int hintflags) {
            consumer.setHints(
                    TOPDOWNLEFTRIGHT | COMPLETESCANLINES | SINGLEPASS | (hintflags & SINGLEFRAME));
        }

        public void setPixels(
                int x,
                int y,
                int w,
                int h,
                ColorModel model,
                byte pixels[],
                int off,
                int scansize) {
            int srcoff = off;
            int dstoff = y * srcW + x;
            for (int yc = 0; yc < h; yc++) {
                for (int xc = 0; xc < w; xc++) {
                    raster[dstoff++] = model.getRGB(pixels[srcoff++] & 0xff);
                }
                srcoff += (scansize - w);
                dstoff += (srcW - w);
            }
        }

        public void setPixels(
                int x, int y, int w, int h, ColorModel model, int pixels[], int off, int scansize) {
            int srcoff = off;
            int dstoff = y * srcW + x;
            if (model == defaultRGB) {
                for (int yc = 0; yc < h; yc++) {
                    System.arraycopy(pixels, srcoff, raster, dstoff, w);
                    srcoff += scansize;
                    dstoff += srcW;
                }
            } else {
                for (int yc = 0; yc < h; yc++) {
                    for (int xc = 0; xc < w; xc++) {
                        raster[dstoff++] = model.getRGB(pixels[srcoff++]);
                    }
                    srcoff += (scansize - w);
                    dstoff += (srcW - w);
                }
            }
        }

        public void imageComplete(int status) {
            if (status == IMAGEERROR || status == IMAGEABORTED) {
                consumer.imageComplete(status);
                return;
            }
            int pixels[] = new int[dstW];
            Quad q = new Quad();
            DPoint coord00 = new DPoint(),
                    coord10 = new DPoint(),
                    coord11 = new DPoint(),
                    coord01 = new DPoint();
            double r, g, b, a, w;
            boolean changeLightness = false;
            double mainProportion = 0, otherProportion = 0;
            if (lightness != 0) {
                changeLightness = true;
                if (lightness < 0) {
                    mainProportion = (1 + lightness); // 0 = 1, -1 = 0
                    // other is zero
                } else {
                    mainProportion = (1 - lightness); // 0 = 1, 1 = 0
                    otherProportion = 0xFF * (1 - mainProportion);
                }
            }
            boolean[] topOk = new boolean[dstW];
            double[] topRowX = new double[dstW];
            double[] topRowY = new double[dstW];
            boolean[] bottomOk = new boolean[dstW];
            double[] bottomRowX = new double[dstW];
            double[] bottomRowY = new double[dstW];

            fillRow(dstW, 0, bottomOk, bottomRowX, bottomRowY);

            for (int dy = 0; dy < dstH; dy++) {
                // exchange rows
                boolean[] temp = bottomOk;
                bottomOk = topOk;
                topOk = temp;
                double[] temp2 = bottomRowX;
                bottomRowX = topRowX;
                topRowX = temp2;
                temp2 = bottomRowY;
                bottomRowY = topRowY;
                topRowY = temp2;
                // and fill
                fillRow(dstW, dy + 1, bottomOk, bottomRowX, bottomRowY);
                for (int dx = 0; dx < dstW - 1; dx++) {
                    // optimize later

                    // find the corners of the destination pixel in source space
                    pixels[dx] = 0;
                    /*
                             if (false) {
                                 int i = (int)Math.round(coord00.x);
                                 int j = (int)Math.round(coord00.y);
                                 if (i < 0 || j < 0 || i >= srcW || j >= srcH) {
                                     pixels[dx] = 0;
                                 } else {
                                     pixels[dx] = raster[j * srcW + i];
                                 }
                                 continue;
                             }
                             if (!toptrans.itransform(dx+1, dy, coord10)) continue;
                             if (!trans.itransform(dx+1, dy+1, coord11)) continue;
                             if (!trans.itransform(dx, dy+1, coord01)) continue;
                    */
                    if (!topOk[dx] || !topOk[dx + 1] || !bottomOk[dx] || !bottomOk[dx + 1]) {
                        // pixels[dx] = 0xFFFFFFFF;
                        continue;
                    }
                    coord00.x = topRowX[dx];
                    coord00.y = topRowY[dx];
                    coord10.x = topRowX[dx + 1];
                    coord10.y = topRowY[dx + 1];
                    coord01.x = bottomRowX[dx];
                    coord01.y = bottomRowY[dx];
                    coord11.x = bottomRowX[dx + 1];
                    coord11.y = bottomRowY[dx + 1];

                    q.set(coord00, coord10, coord11, coord01);

                    // add up the weighted colors
                    r = g = b = a = w = 0;
                    int xx0 = (int) q.containing.x0;
                    int xx1 = (int) q.containing.x1;
                    int yy0 = (int) q.containing.y0;
                    int yy1 = (int) q.containing.y1;
                    for (int x0 = xx0; x0 < xx1; ++x0) {
                        for (int y0 = yy0; y0 < yy1; ++y0) {
                            double weight;
                            // weight = q.getWeight(x0, y0);
                            weight = 1;
                            if (weight == 0.0) {
                                continue;
                            }
                            w += weight;
                            if (x0 < 0 || y0 < 0 || x0 >= srcW || y0 >= srcH) {
                                continue;
                            }
                            int color = raster[y0 * srcW + x0];
                            a += ((color >> 24) & 0xFF) * weight;
                            r += ((color >> 16) & 0xFF) * weight;
                            g += ((color >> 8) & 0xFF) * weight;
                            b += ((color) & 0xFF) * weight;
                        }
                    }
                    // average:
                    r /= w;
                    g /= w;
                    b /= w;
                    a /= w;

                    if (changeLightness) {
                        r = mainProportion * r + otherProportion;
                        g = mainProportion * g + otherProportion;
                        b = mainProportion * b + otherProportion;
                        a = mainProportion * a + otherProportion;
                    }

                    pixels[dx] =
                            ((int) Math.max(0, Math.min(0xFF, Math.round(a))) << 24)
                                    | ((int) Math.max(0, Math.min(0xFF, Math.round(r))) << 16)
                                    | ((int) Math.max(0, Math.min(0xFF, Math.round(g))) << 8)
                                    | ((int) Math.max(0, Math.min(0xFF, Math.round(b))));
                }
                consumer.setPixels(0, dy, dstW, 1, defaultRGB, pixels, 0, dstW);
                if ((dy % 50) == 0) {
                    System.out.println(dy);
                }
            }
            consumer.imageComplete(status);
        }

        /**
         * @param i
         * @param dstW2
         * @param j
         * @param rowX
         * @param rowY
         */
        private void fillRow(int xLimit, int dy, boolean[] ok, double[] rowX, double[] rowY) {
            DPoint coord00 = new DPoint();
            for (int dx = 0; dx < xLimit; dx++) {
                ok[dx] = trans.itransform(dx, dy, coord00);
                rowX[dx] = coord00.x;
                rowY[dx] = coord00.y;
            }
        }
    }

    public static class RotateFilter extends ImageFilter {

        private static ColorModel defaultRGB = ColorModel.getRGBdefault();

        private double angle;

        private double sin;

        private double cos;

        private double coord[] = new double[2];

        private int raster[];

        private int xoffset, yoffset;
        private int srcW, srcH;
        private int dstW, dstH;

        public RotateFilter(double angle) {
            this.angle = angle;
            sin = Math.sin(angle);
            cos = Math.cos(angle);
        }

        public void transform(double x, double y, double[] retcoord) {
            // Remember that the coordinate system is upside down so apply
            // the transform as if the angle were negated.
            // cos(-angle) =  cos(angle)
            // sin(-angle) = -sin(angle)
            retcoord[0] = cos * x + sin * y;
            retcoord[1] = cos * y - sin * x;
        }

        public void itransform(double x, double y, double[] retcoord) {
            // Remember that the coordinate system is upside down so apply
            // the transform as if the angle were negated.  Since inverting
            // the transform is also the same as negating the angle, itransform
            // is calculated the way you would expect to calculate transform.
            retcoord[0] = cos * x - sin * y;
            retcoord[1] = cos * y + sin * x;
        }

        public void transformBBox(Rectangle rect) {
            double minx = Double.POSITIVE_INFINITY;
            double miny = Double.POSITIVE_INFINITY;
            double maxx = Double.NEGATIVE_INFINITY;
            double maxy = Double.NEGATIVE_INFINITY;
            for (int y = 0; y <= 1; y++) {
                for (int x = 0; x <= 1; x++) {
                    transform(rect.x + x * rect.width, rect.y + y * rect.height, coord);
                    minx = Math.min(minx, coord[0]);
                    miny = Math.min(miny, coord[1]);
                    maxx = Math.max(maxx, coord[0]);
                    maxy = Math.max(maxy, coord[1]);
                }
            }
            rect.x = (int) Math.floor(minx);
            rect.y = (int) Math.floor(miny);
            rect.width = (int) Math.ceil(maxx) - rect.x + 1;
            rect.height = (int) Math.ceil(maxy) - rect.y + 1;
        }

        public void setDimensions(int width, int height) {
            Rectangle rect = new Rectangle(0, 0, width, height);
            transformBBox(rect);
            xoffset = -rect.x;
            yoffset = -rect.y;
            srcW = width;
            srcH = height;
            dstW = rect.width;
            dstH = rect.height;
            raster = new int[srcW * srcH];
            consumer.setDimensions(dstW, dstH);
        }

        public void setColorModel(ColorModel model) {
            consumer.setColorModel(defaultRGB);
        }

        public void setHints(int hintflags) {
            consumer.setHints(
                    TOPDOWNLEFTRIGHT | COMPLETESCANLINES | SINGLEPASS | (hintflags & SINGLEFRAME));
        }

        public void setPixels(
                int x,
                int y,
                int w,
                int h,
                ColorModel model,
                byte pixels[],
                int off,
                int scansize) {
            int srcoff = off;
            int dstoff = y * srcW + x;
            for (int yc = 0; yc < h; yc++) {
                for (int xc = 0; xc < w; xc++) {
                    raster[dstoff++] = model.getRGB(pixels[srcoff++] & 0xff);
                }
                srcoff += (scansize - w);
                dstoff += (srcW - w);
            }
        }

        public void setPixels(
                int x, int y, int w, int h, ColorModel model, int pixels[], int off, int scansize) {
            int srcoff = off;
            int dstoff = y * srcW + x;
            if (model == defaultRGB) {
                for (int yc = 0; yc < h; yc++) {
                    System.arraycopy(pixels, srcoff, raster, dstoff, w);
                    srcoff += scansize;
                    dstoff += srcW;
                }
            } else {
                for (int yc = 0; yc < h; yc++) {
                    for (int xc = 0; xc < w; xc++) {
                        raster[dstoff++] = model.getRGB(pixels[srcoff++]);
                    }
                    srcoff += (scansize - w);
                    dstoff += (srcW - w);
                }
            }
        }

        public void imageComplete(int status) {
            if (status == IMAGEERROR || status == IMAGEABORTED) {
                consumer.imageComplete(status);
                return;
            }
            int pixels[] = new int[dstW];
            for (int dy = 0; dy < dstH; dy++) {
                itransform(0 - xoffset, dy - yoffset, coord);
                double x1 = coord[0];
                double y1 = coord[1];
                itransform(dstW - xoffset, dy - yoffset, coord);
                double x2 = coord[0];
                double y2 = coord[1];
                double xinc = (x2 - x1) / dstW;
                double yinc = (y2 - y1) / dstW;
                for (int dx = 0; dx < dstW; dx++) {
                    int sx = (int) Math.round(x1);
                    int sy = (int) Math.round(y1);
                    if (sx < 0 || sy < 0 || sx >= srcW || sy >= srcH) {
                        pixels[dx] = 0;
                    } else {
                        pixels[dx] = raster[sy * srcW + sx];
                    }
                    x1 += xinc;
                    y1 += yinc;
                }
                consumer.setPixels(0, dy, dstW, 1, defaultRGB, pixels, 0, dstW);
            }
            consumer.imageComplete(status);
        }
    }
    /*
     public static double convertDegreesToDecimal(double degrees, double minutes, double seconds, boolean NorthOrEast) {
         double result = (degrees + minutes / 60 + seconds / 3600);
         if (!NorthOrEast) result = -result;
         return result;
     }
    */
    /*
     public static void convertLongitudeLatitudeToWidthHeight(double longitude, double latitude, double width, double height, DPoint output) {
         output.x = (longitude + 180)/360 * width;
         output.y = (90 - latitude)/180 * height;
     }
    */
    /*
     public static void convertPolarRadiansToWidthHeight(double longitudeR, double colatitudeR, double width, double height, DPoint output) {
         // get in range
         longitudeR += Math.PI; // origin on left
         while (longitudeR < 0) longitudeR += Math.PI * 2;
         while (longitudeR > Math.PI * 2) longitudeR -= Math.PI * 2;
         output.x = longitudeR/(Math.PI * 2) * width;
         output.y = colatitudeR/Math.PI * height;
     }
    */
    /*
     public static void convertLongitudeLatitudeToPolarRadians(double longitude, double latitude, DPoint output) {
         output.x = longitude/180 * Math.PI;
         output.y = (90 - latitude)/180 * Math.PI;
     }
    */

    public static BufferedImage convertToBuffered(Image image) {
        int thumbWidth = image.getWidth(null);
        int thumbHeight = image.getHeight(null);
        BufferedImage thumbImage =
                new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = thumbImage.createGraphics();
        graphics2D.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, null);
        return thumbImage;
    }

    public static void addGrid(Image image, Transform trans) {
        int thumbWidth = image.getWidth(null);
        int thumbHeight = image.getHeight(null);
        BufferedImage thumbImage =
                new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = thumbImage.createGraphics();
        graphics2D.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        Color meridian = Color.red;
        Color everyOtherLine = Color.orange;
        if (lightness > 0) {
            graphics2D.setClip(0, 0, thumbWidth, thumbHeight);
            graphics2D.setColor(
                    new Color(
                            (int) (0xFF * lightness),
                            (int) (0xFF * lightness),
                            (int) (0xFF * lightness)));
            graphics2D.fillRect(0, 0, thumbWidth, thumbHeight);
            meridian = new Color(0xFF, (int) (0xFF * lightness), (int) (0xFF * lightness));
        }
        graphics2D.setClip(trans.getClip());
        graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, null);
        // Menlo Park 37? 28' 48" N 122? 08' 39" W

        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // double latitude = convertDegreesToDecimal(37.0, 28.0, 48.0, true); // N = +
        // double longitude = convertDegreesToDecimal(122.0, 8.0, 39.0, false); // W = -
        DPoint retCoord = new DPoint();

        // drawPoint(graphics2D, trans, longitude, latitude);

        drawPoint(graphics2D, trans, Color.green, Color.white, originLong, originLat);
        retCoord.x = originLong;
        retCoord.y = originLat;
        getAntipode(retCoord);
        drawPoint(graphics2D, trans, Color.red, Color.white, retCoord.x, retCoord.y);

        graphics2D.setFont(font);
        FontMetrics fm = graphics2D.getFontMetrics();

        BasicStroke normal = new BasicStroke(1.0f / 3);
        BasicStroke thick = new BasicStroke(2.0f / 3);

        if (true) {
            // hack to draw circles
            // convertLongitudeLatitudeToPolarRadians(originLong, originLat, retCoord);
            // double longR = retCoord.x;
            // double colatR = retCoord.y;

            // SphericalTriangle stri = new SphericalTriangle();
            Navigator navigator = new Navigator().setLat1Lon1(originLat, originLong);
            int increment = 180 / degreeInterval;
            int grain = 3;
            int labelPosition = increment / 2;

            // circles of equal distance
            double dInc = Math.PI / increment;
            // double dInc2 = dInc/grain;
            int distLimit = increment - 1;
            int angleLimit = 2 * increment - 1;
            int halfAngle = increment;

            /*
                 for (int distanceI = 1; distanceI <= distLimit; ++distanceI) {
                     if (distanceI == labelPosition) graphics2D.setColor(Color.black);
                     else graphics2D.setColor(Color.yellow);
                     double distance = dInc * distanceI;
                     double lat1 = 0, lon1 = 0;
                     for (int angleI = 0; angleI <= (angleLimit + 1) * grain; ++angleI) {
                         double angle = dInc2 * angleI;
                         //System.out.println("Distance: " + distance + "\tAngle: " + angle);
                         navigator.setDistanceCourse(distance, angle);
                         double lat2 = trans.srcH_lat.back(navigator.getLat2());
                         double lon2 = trans.srcW_long.back(navigator.getLon2());
                         //System.out.println("Distance: " + distance + "\tAngle: " + angle);
                         if (angleI != 0) drawLine(graphics2D, trans, lon2, lat2, lon1, lat1);
                         lat1 = lat2;
                         lon1 = lon2;
                     }
                 }

                 // lines to antipode
                 for (int angleI = 0; angleI <= angleLimit; ++angleI) {
                     double angle = dInc * angleI;
                     double lat1 = 0, lon1 = 0;
                     if (angleI == 0) graphics2D.setColor(Color.black);
                     else graphics2D.setColor(Color.white);
                     for (int distanceI = grain; distanceI <= distLimit * grain; ++distanceI) {
                         double distance = dInc2 * distanceI;
                         //System.out.println("Distance: " + distance + "\tAngle: " + angle);
                         navigator.setDistanceCourse(distance, angle);
                         double lat2 = trans.srcH_lat.back(navigator.getLat2());
                         double lon2 = trans.srcW_long.back(navigator.getLon2());
                         //System.out.println("Distance: " + distance + "\tAngle: " + angle);
                         if (distanceI != grain) drawLine(graphics2D, trans, lon2, lat2, lon1, lat1);
                         lat1 = lat2;
                         lon1 = lon2;
                     }
            */

            // lines to the antipode
            double gap = 0.02;
            PathTransform pathTransform = new PathTransform(navigator, trans);
            LineDrawer ld = new LineDrawer(graphics2D, pathTransform);
            for (int angleI = 0; angleI <= angleLimit; ++angleI) {
                double angle = dInc * angleI;
                if (angleI == 0 || angleI == halfAngle) {
                    graphics2D.setColor(meridian);
                    graphics2D.setStroke(thick);
                } else if ((angleI % 3) == 0) {
                    graphics2D.setColor(everyOtherLine);
                    graphics2D.setStroke(thick);
                } else {
                    graphics2D.setColor(Color.white);
                    graphics2D.setStroke(normal);
                }
                pathTransform.setAngle(angle);
                ld.draw(gap, 1 - gap);
            }

            AngleCircleTransform angleTransform = new AngleCircleTransform(navigator, trans);
            ld = new LineDrawer(graphics2D, angleTransform);
            for (int distanceI = 1; distanceI <= distLimit; ++distanceI) {
                if (distanceI == labelPosition) {
                    graphics2D.setColor(meridian);
                    graphics2D.setStroke(thick);
                } else {
                    graphics2D.setColor(Color.white);
                    graphics2D.setStroke(normal);
                }
                double distance = dInc * distanceI;
                angleTransform.setDistance(distance);
                ld.draw(0, 1);
            }

            //    if (doLabels) {
            // graphics2D.setClip(null);
            // StandardCodes sc = StandardCodes.make();
            // LabelPosition lp = new LabelPosition(graphics2D, trans.dstW, trans.dstH);
            // Map zones = sc.getZoneData();
            // Set zkeys = sc.getGoodAvailableCodes("tzid");
            //    Date now = new Date();
            //    for (Iterator it = zkeys.iterator(); it.hasNext();) {
            //    String fullkey = (String) it.next();
            //    List data = (List) zones.get(fullkey);
            //    //String key = tzf.getFormattedZone(fullkey,"vvvv", now.getTime(), false); //
            // key.substring(key.lastIndexOf('/')+1);
            //    retCoord.y = ((Double)data.get(0)).doubleValue();
            //    retCoord.y = Navigator.toRadians(retCoord.y, 0, 0, false);
            //    retCoord.x = ((Double)data.get(1)).doubleValue();
            //    retCoord.x = Navigator.toRadians(retCoord.x, 0, 0, true);
            //    drawPoint(graphics2D, trans, Color.white, Color.red, retCoord.x, retCoord.y,
            // null);
            //    lp.add(trans, retCoord.x, retCoord.y, key);
            //    }
            //    lp.draw();
            //    }
            /*
                 graphics2D.setColor(Color.red);
                 graphics2D.setStroke(new BasicStroke(2f));
                 pathTransform = new PathTransform(navigator, trans);
                 pathTransform.setAngle(20*Navigator.DEGREE);
                  ld = new LineDrawer(graphics2D,pathTransform);
                 ld.draw(0,1);
            */

            /*
                 for (int distanceI = 1; distanceI < coord.length; ++distanceI) {
                     boolean doLabel = distanceI == labelPosition;
                     double[][] distanceR = coord[distanceI];
                     for (int angleI = 0; angleI < distanceR.length-1; ++angleI) {
                         double[] angleR00 = distanceR[angleI];
                         double[] angleR01 = distanceR[angleI+1];
                         graphics2D.setColor(Color.white);
                         if (doLabel) {
                             if (AverageWithColor) graphics2D.setColor(Color.gray);
                             else graphics2D.setColor(Color.black);
                         }
                         drawLine(graphics2D, trans, angleR00[0], angleR00[1], angleR01[0], angleR01[1]);
                         if (doLabel) {
                             drawDegrees(graphics2D, trans, fm, 180 * angleI / increment, angleR00[0], angleR00[1]);
                         }
                     }

                     double[] angleR00 = distanceR[distanceR.length-1];
                     double[] angleR01 = distanceR[0];
                     graphics2D.setColor(Color.white);
                     if (doLabel) {
                         if (AverageWithColor) graphics2D.setColor(Color.gray);
                         else graphics2D.setColor(Color.black);
                     }
                     drawLine(graphics2D, trans, angleR00[0], angleR00[1], angleR01[0], angleR01[1]);
                     if (doLabel) {
                         drawDegrees(graphics2D, trans, fm, 180 * (distanceR.length-1) / increment, angleR00[0], angleR00[1]);
                     }

                 }
            */

        }
        // save thumbnail image to OUTFILE
        griddedImage = thumbImage;
        if (DEBUG_ICON) {
            System.out.println("Changing Icon6");
        }
        ImageIcon resultIcon = new ImageIcon(thumbImage); // recreate with buffered version
        if (DEBUG_ICON) {
            System.out.println("Changing Icon7");
        }
        mainPicture.setIcon(resultIcon);
    }

    static class LabelPosition {
        class Chunk implements Comparable {
            double x, y;
            int xStart, yStart, width;
            String s;

            public Chunk(double x2, double y2, String s2) {
                x = x2;
                y = y2;
                s = s2;
                width = 1;
                xStart = (int) (x2 / tileWidth);
                yStart = (int) (y2 / tileHeight);
                if (s == null) {
                    return;
                }
                xStart += 2;

                Rectangle2D r = metrics.getStringBounds(s, graphics2D);
                width = 1 + (int) ((r.getWidth() - 1.0) / tileWidth);

                if (xStart + width >= tileWidthCount) {
                    xStart = tileWidthCount - width;
                }
            }

            public int compareTo(Object o) {
                Chunk that = (Chunk) o;
                if (x != that.x) {
                    return x < that.x ? -1 : 1;
                }
                if (width != that.width) {
                    return width > that.width ? -1 : 1; // largest first
                }
                if (y != that.y) {
                    return y < that.y ? -1 : 1;
                }
                if (s == null) {
                    if (that.s == null) {
                        return 0;
                    }
                    return -1;
                }
                if (that.s == null) {
                    return 1;
                }
                return s.compareTo(that.s);
            }

            public boolean overlaps(Chunk that) {
                if (yStart != that.yStart) {
                    return false;
                }
                if (xStart > that.xStart + that.width) {
                    return false;
                }
                if (that.xStart > xStart + width) {
                    return false;
                }
                return true;
            }
        }

        Graphics2D graphics2D;
        FontMetrics metrics;
        Set[] lineContents;
        double tileWidth, tileHeight;
        int tileWidthCount, tileHeightCount;
        double ascent;
        Set initialContents = new TreeSet();

        LabelPosition(Graphics2D graphics2D, double width, double height) {
            this.graphics2D = graphics2D;
            metrics = graphics2D.getFontMetrics();
            Rectangle2D r = metrics.getStringBounds("n", graphics2D);
            ascent = metrics.getAscent();
            // tile the map into a grid
            tileWidthCount = (int) (width / r.getWidth());
            tileWidth = width / tileWidthCount + 0.0000001;
            tileHeightCount = (int) (height / r.getHeight());
            tileHeight = height / tileHeightCount + 0.0000001;
            lineContents = new Set[tileHeightCount];
            for (int i = 0; i < lineContents.length; ++i) {
                lineContents[i] = new TreeSet();
            }
        }

        void add(Transform trans, double longitude, double latitude, String s) {
            double xx = trans.srcW_long.back(longitude);
            double yy = trans.srcH_lat.back(latitude);
            trans.transform(xx, yy, drawLineP1);
            Chunk c = new Chunk(drawLineP1.x, drawLineP1.y, s);
            initialContents.add(c);
            c = new Chunk(drawLineP1.x, drawLineP1.y, null); // point only
            lineContents[c.yStart].add(c);
        }

        void fixContents() {
            for (Iterator it2 = initialContents.iterator(); it2.hasNext(); ) {
                Chunk c = (Chunk) it2.next();
                findFittingLine(c);
                lineContents[c.yStart].add(c);
            }
        }
        /**
         * @param c
         * @return
         */
        private void findFittingLine(Chunk c) {
            int pos = c.yStart;
            boolean positive = false;
            boolean lastOutOfBounds = false;
            main:
            for (int ii = 0; ; ++ii, positive = !positive) {
                pos += (positive ? ii : -ii);
                if (pos < 0 || pos >= lineContents.length) {
                    if (lastOutOfBounds) {
                        c.yStart = 0;
                        return;
                    }
                    lastOutOfBounds = true;
                    continue;
                }
                lastOutOfBounds = false;
                c.yStart = pos; // assume ok.
                // go x, x+1, x-1, +2, -2, ...
                for (Iterator it = lineContents[pos].iterator(); it.hasNext(); ) {
                    Chunk that = (Chunk) it.next();
                    if (c.overlaps(that)) {
                        if (DEBUG) {
                            System.out.println(
                                    pos + " pushing " + c.s + " (collision with " + that.s + ")");
                        }
                        continue main;
                    }
                }
                return; // yStart now set right.
            }
        }

        void draw() {
            fixContents();
            graphics2D.setColor(Color.pink);
            for (int i = 0; i < lineContents.length; ++i) {
                for (Iterator it = lineContents[i].iterator(); it.hasNext(); ) {
                    Chunk c = (Chunk) it.next();
                    if (c.s == null) {
                        continue; // point
                    }
                    double x2 = tileWidth * c.xStart;
                    double y2 = tileHeight * c.yStart;
                    Line2D.Double line2 = new Line2D.Double(c.x, c.y, x2, y2 + tileHeight / 2);
                    graphics2D.draw(line2);
                    graphics2D.drawString(c.s, (int) x2, (int) (y2 + ascent));
                }
            }
        }
    }

    /**
     * @param graphics2D
     * @param trans
     * @param fill TODO
     * @param line TODO
     * @param longitude
     * @param latitude
     * @return
     */
    private static void drawPoint(
            Graphics2D graphics2D,
            Transform trans,
            Color fill,
            Color line,
            double longitude,
            double latitude,
            String label) {
        double xx = trans.srcW_long.back(longitude);
        double yy = trans.srcH_lat.back(latitude);
        // convertLongitudeLatitudeToWidthHeight(longitude, latitude, trans.srcW, trans.srcH,
        // drawLineP1);
        // double xx = drawLineP1.x;
        // double yy = drawLineP1.y;
        // System.out.println(" xx: " + xx + ", yy: " + yy);
        double radius = 1;
        trans.transform(xx, yy, drawLineP1);

        Ellipse2D.Double ellipse = new Ellipse2D.Double();
        ellipse.x = drawLineP1.x - radius;
        ellipse.y = drawLineP1.y - radius;
        ellipse.height = ellipse.width = radius * 2;
        graphics2D.setColor(fill);
        graphics2D.fill(ellipse);
        graphics2D.setColor(line);
        graphics2D.draw(ellipse);
        /*
           if (label == null) return;
           if (label != null) {
               Line2D.Double line2 = new Line2D.Double(drawLineP1.x, drawLineP1.y, drawLineP1.x + 5, drawLineP1.y + 5);
               graphics2D.draw(line2);
           }

           if (label != null) graphics2D.drawString(label, (int)drawLineP1.x + 5, (int)drawLineP1.y + 5);
        */
    }

    private static void drawPoint(
            Graphics2D graphics2D,
            Transform trans,
            Color fill,
            Color line,
            double longitude,
            double latitude) {
        drawPoint(graphics2D, trans, fill, line, longitude, latitude, null);
    }

    /**
     * @param graphics2D
     * @param trans
     * @param fm
     * @param retCoord
     * @param increment
     * @param angleI
     * @param angleR00
     */
    private static void drawDegrees(
            Graphics2D graphics2D,
            Transform trans,
            FontMetrics fm,
            double degrees,
            double x,
            double y) {
        String degreesStr = nf.format(degrees) + "°";
        Rectangle2D r = fm.getStringBounds(degreesStr, graphics2D);
        trans.transform(x - r.getWidth() / 2, y, drawLineP1);
        graphics2D.drawString(degreesStr, (int) drawLineP1.x, (int) drawLineP1.y);
    }

    private static DPoint drawLineP1 = new DPoint();
    /*

     private static void drawLine(Graphics2D graphics2D, Transform trans, double x1, double y1, double x2, double y2) {
         // check for cases where it crosses a boundary
         double xDist = Math.abs(x1 - x2);
         double yDist = Math.abs(y1 - y2);
         if (xDist > trans.srcW/2) {
             if (yDist > trans.srcH/2) {
                 // skip, don't care about opposite corners
                 System.out.println("Skipping opposite corners");
             } else {
                 if (x1 < x2) {
                     drawLine2(graphics2D, trans, x1, y1, x2 - trans.srcW, y2);
                     drawLine2(graphics2D, trans, x1 + trans.srcW, y1, x2, y2);
                 } else {
                     drawLine2(graphics2D, trans, x1, y1, x2 + trans.srcW, y2);
                     drawLine2(graphics2D, trans, x1 - trans.srcW, y1, x2, y2);
                 }
             }
         } else if (yDist > trans.srcH/2) {
             if (y1 < y2) {
                 drawLine2(graphics2D, trans, x1, y1, x2, y2 - trans.srcH);
                 drawLine2(graphics2D, trans, x1, y1 + trans.srcH, x2, y2);
             } else {
                 drawLine2(graphics2D, trans, x1, y1, x2, y2 + trans.srcH);
                 drawLine2(graphics2D, trans, x1, y1 - trans.srcH, x2, y2);
             }
         } else {
             drawLine2(graphics2D, trans, x1, y1, x2, y2);
         }
     }

     private static void drawLine2(Graphics2D graphics2D, Transform trans, double x, double y, double x2, double y2) {
         trans.transform(x, y, drawLineP1);
         int ix = (int) Math.round(drawLineP1.x);
         int iy = (int) Math.round(drawLineP1.y);
         trans.transform(x2, y2, drawLineP1);
         int ix2 = (int) Math.round(drawLineP1.x);
         int iy2 = (int) Math.round(drawLineP1.y);
         graphics2D.drawLine(ix, iy, ix2, iy2);
     }
    */

    abstract static class TTransform {
        double x, y;
        // t is 0..1
        abstract void transform(double t);
    }

    static class PathTransform extends TTransform {
        private Navigator navigator;
        private Transform trans;
        private double angle;

        PathTransform(Navigator navigator, Transform trans) {
            this.navigator = navigator;
            this.trans = trans;
        }

        void setAngle(double angle) {
            this.angle = angle;
        }

        transient DPoint temp = new DPoint();

        void transform(double t) {
            navigator.setDistanceCourse(t * Math.PI, angle);
            y = trans.srcH_lat.back(navigator.getLat2());
            x = trans.srcW_long.back(navigator.getLon2());
            trans.transform(x, y, temp);
            x = temp.x;
            y = temp.y;
        }
    }

    static class AngleCircleTransform extends TTransform {
        private Navigator navigator;
        private Transform trans;
        private double distance;

        AngleCircleTransform(Navigator navigator, Transform trans) {
            this.navigator = navigator;
            this.trans = trans;
        }

        void setDistance(double distance) {
            this.distance = distance;
        }

        transient DPoint temp = new DPoint();

        void transform(double t) {
            navigator.setDistanceCourse(distance, t * (2 * Math.PI));
            y = trans.srcH_lat.back(navigator.getLat2());
            x = trans.srcW_long.back(navigator.getLon2());
            trans.transform(x, y, temp);
            x = temp.x;
            y = temp.y;
        }
    }

    static class LineDrawer {
        double distanceSquaredLimit = 10 * 10;
        Graphics2D graphics2D;
        Line2D.Double line = new Line2D.Double();
        transient double startX, startY, startT;
        // transient double endX, endY, endT;
        TTransform ttransform;
        // int segments = 0;
        LineDrawer(Graphics2D graphics2D, TTransform ttransform) {
            this.graphics2D = graphics2D;
            this.ttransform = ttransform;
        }
        // t is 0..1
        void draw(double startT, double endT) {
            this.startT = startT;
            // this.endT = endT;
            ttransform.transform(startT);
            startX = ttransform.x;
            startY = ttransform.y;
            ttransform.transform(endT);
            double endX = ttransform.x;
            double endY = ttransform.y;
            draw(3, 10, endT, endX, endY);
            // System.out.println("segments: " + segments);
        }

        void draw(int minDepth, int maxDepth, double endT, double endX, double endY) {
            // System.out.println(maxDepth + "\t" + startT + ", " + startX + ", " + startY + "\t" +
            // endT + ", " + endX + ", " + endY);
            // at the end of a draw, the startT is always moved up to the endT
            boolean divide = false;
            // if we've reached the limit, draw
            if (minDepth > 0) { // if we are under the depth, divide and conquer
                divide = true;
            } else {
                // if the distance is large, and still not too deep, divide and conquer
                double dx = endX - startX;
                double dy = endY - startY;
                // System.out.println("dist: " + Math.sqrt(dx*dx + dy*dy));
                if ((dx * dx + dy * dy) > distanceSquaredLimit) {
                    if (maxDepth <= 0) {
                        return; // skip if too long
                    }
                    divide = true;
                }
            }
            if (divide) {
                double midT = (startT + endT) / 2;
                ttransform.transform((startT + endT) / 2);
                double midX = ttransform.x; // keep, since ttransform gets overridden
                double midY = ttransform.y;
                draw(minDepth - 1, maxDepth - 1, midT, midX, midY);
                draw(minDepth - 1, maxDepth - 1, endT, endX, endY);
            } else {
                // System.out.println("Drawing");
                // segments++;
                line.x1 = startX;
                line.y1 = startY;
                line.x2 = endX;
                line.y2 = endY;
                graphics2D.draw(line);
                // graphics2D.drawLine((int) Math.round(startX), (int) Math.round(startY),
                //        (int) Math.round(endX), (int) Math.round(endY));
            }
            startT = endT;
            startX = endX;
            startY = endY;
        }
    }

    public static void writeImage(BufferedImage image, String filename, float quality) {
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
            //      JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
            //      JPEGEncodeParam param = encoder
            //      .getDefaultJPEGEncodeParam(image);
            //      quality = Math.max(0, Math.min(quality, 100));
            //      param.setQuality(quality / 100.0f, false);
            //      encoder.setJPEGEncodeParam(param);
            //      encoder.encode(image);
            //      out.close();
            ImageIO.write(image, "jpg", new File(filename));
            System.out.println("Saving on: " + new File(filename).getCanonicalPath());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed write of image");
        }
    }

    public static class Thumbnail {
        public static void main(String[] args) throws Exception {
            if (args.length != 5) {
                System.err.println(
                        "Usage: java Thumbnail INFILE " + "OUTFILE WIDTH HEIGHT QUALITY");
                System.exit(1);
            }
            // load image from INFILE
            Image image = Toolkit.getDefaultToolkit().getImage(args[0]);
            MediaTracker mediaTracker = new MediaTracker(new Container());
            mediaTracker.addImage(image, 0);
            mediaTracker.waitForID(0);
            // determine thumbnail size from WIDTH and HEIGHT
            int thumbWidth = Integer.parseInt(args[2]);
            int thumbHeight = Integer.parseInt(args[3]);
            double thumbRatio = (double) thumbWidth / (double) thumbHeight;
            int imageWidth = image.getWidth(null);
            int imageHeight = image.getHeight(null);
            double imageRatio = (double) imageWidth / (double) imageHeight;
            if (thumbRatio < imageRatio) {
                thumbHeight = (int) (thumbWidth / imageRatio);
            } else {
                thumbWidth = (int) (thumbHeight * imageRatio);
            }
            // draw original image to thumbnail image object and
            // scale it to the new size on-the-fly
            BufferedImage thumbImage =
                    new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics2D = thumbImage.createGraphics();
            graphics2D.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, null);
            // save thumbnail image to OUTFILE
            //      BufferedOutputStream out = new BufferedOutputStream(new
            //              FileOutputStream(args[1]));
            //      JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
            //      JPEGEncodeParam param = encoder.
            //      getDefaultJPEGEncodeParam(thumbImage);
            //      int quality = Integer.parseInt(args[4]);
            //      quality = Math.max(0, Math.min(quality, 100));
            //      param.setQuality((float)quality / 100.0f, false);
            //      encoder.setJPEGEncodeParam(param);
            //      encoder.encode(thumbImage);
            //      out.close();
            File out = new File(args[1]);
            ImageIO.write(thumbImage, "jpg", out);
            // docs say imageio can handle quality settings.
            System.out.println("Done.");
            System.exit(0);
        }
    }
}
