package org.unicode.jsp;

import java.text.DecimalFormat;
import java.text.NumberFormat;

final public class Navigator {
  private double lat1, lon1;
  private transient double cosLat1, sinLat1;

  private double lat2, lon2;
  private double distance, course;
  private transient double sinDistance, cosDistance;

  public static double DEGREE = Math.PI/180.0;

  private static double EPS = 0.000001;   // EPS a small number ~ machine precision

  public static void main(String[] args) {
    double latitude1 = 0; // toRadians(37.0, 28.0, 48.0, false); // N = +
    double longitude1 = 0; // toRadians(180-122.0, 8.0, 39.0, true); // W = -
    Tester tester = new Tester(latitude1, longitude1);

    double[][] tests = {
            {90*DEGREE, -180*DEGREE},
            {-50*DEGREE, -180*DEGREE},
            {25*DEGREE, 10*DEGREE},
            {latitude1, longitude1},
    };
    for (int i = 0; i < tests.length; ++i) {
      tester.testItem(0, tests[i][0], tests[i][1]);
    }
    // exhaustive
    tester.test(0, 0, -90, 90, -180, 180, 1);
  }
  private static class Tester {
    Navigator a = new Navigator();
    Navigator b = new Navigator();
    int counter = 0, failures = 0;

    Tester(double latitude1, double longitude1) {
      a.setLat1Lon1(latitude1, longitude1);
      b.setLat1Lon1(latitude1, longitude1);
      System.out.println("\tLatitude1 " + degrees(a.getLat1()) + "\tLongitude1 " + degrees(a.getLon1()));        
    }

    private void test(double lat0, double lon0, double latMin, double latMax, double lonMin, double lonMax, double inc) {
      for (double dLat = latMin; dLat <= latMax; dLat += inc) {
        for (double dLon = lonMin; dLon <= lonMax; dLon += inc) {
          counter++;
          double lat2a = lat0 + dLat*DEGREE;
          double lon2a = lon0 + dLon*DEGREE;
          if (!testItem(counter, lat2a, lon2a)) {
            failures++;
          }
        }
      }
      System.out.println("Count: " + counter + "\tFailures: " + failures);
      counter = failures = 0;
    }

    boolean testItem(int counter, double lat2a, double lon2a) {
      a.setLat2Lon2(lat2a, lon2a);
      lat2a = a.getLat2();
      lon2a = a.getLon2();
      double distance2 = a.getDistance();
      double course2 = a.getCourse();
      b.setDistanceCourse(distance2, course2);
      double lat2b = b.getLat2();
      double lon2b = b.getLon2();
      boolean success = areClose(lat2b, lat2a) && areClose(lon2a, lon2b);
      if (success && (counter % 1023) != 1) {
        return true;
      }
      System.out.println();
      System.out.println(counter + "\tLat  " + degrees(lat2a) + "\tLong  " + degrees(lon2a)
              + "\tDistance " + degrees(distance2) + "\tCourse " + degrees(course2));
      System.out.println("\t\tLat2 " + degrees(lat2b) + "\tLong2 " + degrees(lon2b)); 
      return success;
    }
  }
  private static boolean areClose(double a, double b) {
    a -= b;
    return (-EPS < a && a < EPS);
  }

  private static final NumberFormat nf = new DecimalFormat("+000.000;-000.000");
  public static String degrees(double in) {
    return nf.format(in/DEGREE) + '°';
  }

  public static double toRadians(double degrees, double minutes, double seconds, boolean northOrWest) {
    double result = (degrees + minutes / 60 + seconds / 3600);
    if (!northOrWest) {
      result = -result;
    }
    return result * DEGREE;
  }

  public Navigator setLat1Lon1(double lat1, double lon1) {
    if (lat1 < -Math.PI/2 + EPS) {
      lat1 = -Math.PI/2;
      lon1 = 0; // no point in distinguishing
    } else if (lat1 > Math.PI/2 + EPS) {
      lat1 = Math.PI/2;
      lon1 = 0; // no point in distinguishing
    } else {
      lon1 = wrap(lon1, -Math.PI, Math.PI);
    }
    this.lat1 = lat1;
    this.lon1 = lon1;
    cosLat1 = Math.cos(lat1);
    sinLat1 = Math.sin(lat1);
    return this;
  }

  public Navigator setLat2Lon2(double lat2, double lon2) {
    if (lat2 < -Math.PI/2 + EPS) {
      lat2 = -Math.PI/2;
      lon2 = 0; // no point in distinguishing
    } else if (lat2 > Math.PI/2 - EPS) {
      lat2 = Math.PI/2;
      lon2 = 0; // no point in distinguishing
    } else {
      lon2 = wrap(lon2, -Math.PI, Math.PI);
    }
    this.lat2 = lat2;
    this.lon2 = lon2;

    double cosLat2 = Math.cos(lat2);
    double sinLat2 = Math.sin(lat2);

    // compute distance
    double halfLatDiff = Math.sin((lat1-lat2)/2);
    double halfLonDiff = Math.sin((lon1-lon2)/2);

    distance = 2*Math.asin(Math.sqrt(halfLatDiff*halfLatDiff + cosLat1*cosLat2*halfLonDiff*halfLonDiff));
    sinDistance = Math.sin(distance);
    cosDistance = Math.cos(distance);

    // compute course
    if (distance < EPS) {
      course = 0;
    } else if (cosLat1 < EPS) {
      if (lat1 > 0) {
        course = Math.PI;   //  starting from N pole
      } else {
        course = 2*Math.PI;          //  starting from S pole
      }
    } else {
      double cosCourse = (sinLat2-sinLat1*cosDistance)/(sinDistance*cosLat1);
      if (cosCourse < -1.0) {
        cosCourse = -1.0;
      }
      if (cosCourse > 1.0) {
        cosCourse = 1.0;
      }
      course=Math.acos(cosCourse);
      if (Math.sin(lon2-lon1) >= 0) {
        course=2*Math.PI-course;
      }               
    }
    return this;
  }

  public Navigator setDistanceCourse(double distance, double course) {
    this.distance = distance;
    this.course = course;        
    sinDistance = Math.sin(distance);
    cosDistance = Math.cos(distance);

    lat2 = Math.asin(sinLat1*cosDistance+cosLat1*sinDistance*Math.cos(course));
    if (lat2 < -Math.PI/2 + EPS || lat2 > Math.PI/2 - EPS) {
      lon2 = 0; // no point in distinguishing
    } else {
      double dlon=Math.atan2(
              Math.sin(course)*sinDistance*cosLat1, 
              cosDistance-sinLat1*Math.sin(lat2));
      lon2 = wrap(lon1-dlon, -Math.PI, Math.PI);
    }
    return this;
  }

  public static double wrap(double aa, double low, double high) {
    double a = aa - low;
    double span = high - low;
    if (a >= 0 && a < span) {
      return aa;
    }
    double intQuotient = Math.floor(a / span);
    return a - intQuotient * span + low;
  }  
  public double getCourse() {
    return course;
  }
  public double getDistance() {
    return distance;
  }
  public double getLat1() {
    return lat1;
  }
  public double getLat2() {
    return lat2;
  }
  public double getLon1() {
    return lon1;
  }
  public double getLon2() {
    return lon2;
  }
}