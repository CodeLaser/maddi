package org.e2immu.analyzer.modification.analyzer.clonebench;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestVarious2 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.lang.reflect.Modifier;
            import java.util.Vector;
            
            public class Function925996_file124449 {
              public static Class[] getStaticClasses(Class cl) {
                Class[] clazzes = cl.getClasses();
                if (clazzes == null) return null;
                Vector vec = new Vector();
                int n = clazzes.length;
                for (int i = 0; i < n; i++) {
                  Class clazz = clazzes[i];
                  if (isStatic(clazz)) {
                    vec.add(clazz);
                  }
                }
                if (vec.size() == 0) {
                  return null;
                }
                Class[] out = new Class[vec.size()];
                vec.toArray(out);
                return out;
              }
            
              /**
               * Indicates if a class is static
               *
               * @param clazz class
               * @return true if the class is static
               */
              public static boolean isStatic(Class clazz) {
                return (clazz.getModifiers() & Modifier.STATIC) != 0;
              }
            }
            """;

    @DisplayName("null formalToConcrete translation map in ExpressionVisitor")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT2 = """
            import java.text.BreakIterator;
            import java.util.ArrayList;
            
            public class Function3998254_file1352239 {
              public static String[] toLowerCaseWordArray(String text) {
                if (text == null || text.length() == 0) {
                  return new String[0];
                }
                ArrayList<String> wordList = new ArrayList<String>();
                BreakIterator boundary = BreakIterator.getWordInstance();
                boundary.setText(text);
                int start = 0;
                for (int end = boundary.next(); end != BreakIterator.DONE; start = end, end = boundary.next()) {
                  String tmp = text.substring(start, end).trim();
                  tmp = replace(tmp, "+", "");
                  tmp = replace(tmp, "/", "");
                  tmp = replace(tmp, "\\\\", "");
                  tmp = replace(tmp, "#", "");
                  tmp = replace(tmp, "*", "");
                  tmp = replace(tmp, ")", "");
                  tmp = replace(tmp, "(", "");
                  tmp = replace(tmp, "&", "");
                  if (tmp.length() > 0) {
                    wordList.add(tmp);
                  }
                }
                return wordList.toArray(new String[wordList.size()]);
              }
            
              /**
               * Replaces all instances of oldString with newString in string.
               *
               * @param string the String to search to perform replacements on
               * @param oldString the String that should be replaced by newString
               * @param newString the String that will replace all instances of oldString
               * @return a String will all instances of oldString replaced by newString
               */
              public static String replace(String string, String oldString, String newString) {
                if (string == null) {
                  return null;
                }
                if (newString == null) {
                  return string;
                }
                int i = 0;
                if ((i = string.indexOf(oldString, i)) >= 0) {
                  char[] string2 = string.toCharArray();
                  char[] newString2 = newString.toCharArray();
                  int oLength = oldString.length();
                  StringBuffer buf = new StringBuffer(string2.length);
                  buf.append(string2, 0, i).append(newString2);
                  i += oLength;
                  int j = i;
                  while ((i = string.indexOf(oldString, i)) > 0) {
                    buf.append(string2, j, i - j).append(newString2);
                    i += oLength;
                    j = i;
                  }
                  buf.append(string2, j, string2.length - j);
                  return buf.toString();
                }
                return string;
              }
            }
            """;

    @DisplayName("null pointer in WriteLinksAndModification")
    @Test
    public void test2() {
        TypeInfo B = javaInspector.parse(INPUT2);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT3 = """
            import javax.sound.midi.*;
            
            public class Function5100944_file1476424 {
            
                public static String MidiMessageToString(MidiMessage msg, long time) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(msg.getClass().getName() + " st=" + msg.getStatus());
                    if (msg instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) msg;
                        sb.append(" ch=" + sm.getChannel());
                        String cmd;
                        switch(sm.getCommand()) {
                            case 128:
                                cmd = new String("NOTE_OFF");
                                break;
                            case 144:
                                cmd = new String("NOTE_ON");
                                break;
                            case 176:
                                cmd = new String("CTRL_CHG");
                                break;
                            case 192:
                                cmd = new String("PRG_CHG");
                                break;
                            case 208:
                                cmd = new String("AFTRTCH");
                                break;
                            case 224:
                                cmd = new String("PTCH_BND");
                                break;
                            default:
                                cmd = new String("" + sm.getCommand());
                        }
                        sb.append(" cmd=" + cmd);
                        sb.append(" d1=" + sm.getData1());
                        sb.append(" d2=" + sm.getData2());
                    } else if (msg instanceof SysexMessage) {
                        SysexMessage sm = (SysexMessage) msg;
                        sb.append(" SysEx");
                    } else if (msg instanceof MetaMessage) {
                        MetaMessage mm = (MetaMessage) msg;
                        sb.append("ty=" + mm.getType());
                    }
                    sb.append(" t=" + time);
                    return sb.toString();
                }
            }
            """;

    @DisplayName("writing casts on an immutable Map")
    @Test
    public void test3() {
        TypeInfo B = javaInspector.parse(INPUT3);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.io.IOException;
            import java.nio.channels.FileChannel;
            import java.nio.channels.FileLock;
            import java.nio.file.FileStore;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import java.nio.file.Paths;
            import java.nio.file.StandardOpenOption;
            import java.util.concurrent.CountDownLatch;
            import java.util.concurrent.atomic.AtomicInteger;
            
            public class TestNioLock {
              public static void main(String[] args) throws IOException, InterruptedException {
                if (args.length != 3) {
                  System.out.println("demo.TestNioLock <test|perform> <file> <sleepMs>");
                  System.exit(EC_ERROR);
                }
                String mode = args[0];
                Path path = Paths.get(args[1]).toAbsolutePath();
                Path latchFile = path.getParent().resolve(TestNioLock.class.getName() + ".latchFile");
                if (Files.isDirectory(path)) {
                  System.out.println("The <file> cannot be directory.");
                  System.exit(EC_ERROR);
                }
                if (!Files.isRegularFile(latchFile)) {
                  Files.createFile(latchFile);
                }
                if ("test".equals(mode)) {
                  System.out.println("Testing file locking on");
                  System.out.println(
                      "  Java "
                          + System.getProperty("java.version")
                          + ", "
                          + System.getProperty("java.vendor"));
                  System.out.println(
                      "  OS "
                          + System.getProperty("os.name")
                          + " "
                          + System.getProperty("os.version")
                          + " "
                          + System.getProperty("os.arch"));
                  FileStore fileStore = Files.getFileStore(path.getParent());
                  System.out.println("  FS " + fileStore.name() + " " + fileStore.type());
                  System.out.println();
                  AtomicInteger oneResult = new AtomicInteger(-1);
                  AtomicInteger twoResult = new AtomicInteger(-1);
                  CountDownLatch latch = new CountDownLatch(2);
                  String javaCmd = System.getProperty("java.home") + "/bin/java";
                  try (FileChannel latchChannel =
                      FileChannel.open(latchFile, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                    try (FileLock latchLock = latchChannel.lock(0L, 1L, false)) {
                      new Thread(
                              () -> {
                                try {
                                  oneResult.set(
                                      new ProcessBuilder(
                                              javaCmd, TestNioLock.class.getName(), "perform", args[1], args[2])
                                          .inheritIO()
                                          .start()
                                          .waitFor());
                                } catch (Exception e) {
                                  oneResult.set(EC_FAILED);
                                } finally {
                                  latch.countDown();
                                }
                              })
                          .start();
                      new Thread(
                              () -> {
                                try {
                                  twoResult.set(
                                      new ProcessBuilder(
                                              javaCmd, TestNioLock.class.getName(), "perform", args[1], args[2])
                                          .inheritIO()
                                          .start()
                                          .waitFor());
                                } catch (Exception e) {
                                  twoResult.set(EC_FAILED);
                                } finally {
                                  latch.countDown();
                                }
                              })
                          .start();
                      // give them a bit of time (to both block)
                      Thread.sleep(1000);
                      latchLock.release();
                      latch.await();
                    }
                  }
                  int oneExit = oneResult.get();
                  int twoExit = twoResult.get();
                  if ((oneExit == EC_WON && twoExit == EC_LOST) || (oneExit == EC_LOST && twoExit == EC_WON)) {
                    System.out.println("OK");
                    System.exit(0);
                  } else {
                    System.out.println("FAILED: one=" + oneExit + " two=" + twoExit);
                    System.exit(EC_FAILED);
                  }
                } else if ("perform".equals(mode)) {
                  String processName = "this came from ManagementFactory.getRuntimeMXBean().getName()";
                  System.out.println(processName + " > started");
                  boolean won = false;
                  long sleepMs = Long.parseLong(args[2]);
                  try (FileChannel latchChannel = FileChannel.open(latchFile, StandardOpenOption.READ)) {
                    try (FileLock latchLock = latchChannel.lock(0L, 1L, true)) {
                      System.out.println(processName + " > latchLock acquired");
                      try (FileChannel channel =
                          FileChannel.open(
                              path,
                              StandardOpenOption.READ,
                              StandardOpenOption.WRITE,
                              StandardOpenOption.CREATE)) {
                        try (FileLock lock = channel.tryLock(0L, 1L, false)) {
                          if (lock != null && lock.isValid() && !lock.isShared()) {
                            System.out.println(processName + " > WON");
                            won = true;
                            Thread.sleep(sleepMs);
                          } else {
                            System.out.println(processName + " > LOST");
                          }
                        }
                      }
                    }
                  }
                  System.out.println(processName + " > ended");
                  if (won) {
                    System.exit(EC_WON);
                  } else {
                    System.exit(EC_LOST);
                  }
                } else {
                  System.err.println("Unknown mode: " + mode);
                }
                System.exit(EC_ERROR);
              }
            
              private static final int EC_WON = 10;
              private static final int EC_LOST = 20;
              private static final int EC_FAILED = 30;
              private static final int EC_ERROR = 100;
            }
            """;

    @DisplayName("MLV can be null in TypeModIndyAnalyzerImpl")
    @Test
    public void test4() {
        TypeInfo B = javaInspector.parse(INPUT4);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.awt.geom.AffineTransform;
            import java.awt.geom.GeneralPath;
            import java.awt.geom.PathIterator;
            
            public class Function4382409_file1762255 {
            
                @SuppressWarnings("unused")
                public static Object[] findPartsPoints(GeneralPath[] paths, int i) {
                    PathIterator pit = paths[i].getPathIterator(new AffineTransform());
                    int nPoints = 0;
                    int nParts = 0;
                    float[] coords = { 0, 1, 2, 3, 4, 5, 6 };
                    while (!pit.isDone()) {
                        int segType = pit.currentSegment(coords);
                        if (segType == PathIterator.SEG_MOVETO) {
                            nParts++;
                        }
                        nPoints++;
                        pit.next();
                    }
                    int[] parts = new int[nParts];
                    double[][] thePoints = new double[nPoints][2];
                    pit = paths[i].getPathIterator(null);
                    nParts = 0;
                    nPoints = 0;
                    while (!pit.isDone()) {
                        int segType = pit.currentSegment(coords);
                        if (segType == PathIterator.SEG_MOVETO) {
                            parts[nParts] = nPoints;
                            nParts++;
                        }
                        thePoints[nPoints][0] = coords[0];
                        thePoints[nPoints][1] = coords[1];
                        nPoints++;
                        pit.next();
                    }
                    Object[] returns = { parts, thePoints };
                    return returns;
                }
            }
            """;

    // cause of the "problem": one but last line
    @DisplayName("expansion of LinkGraph.ensureArraysWhenSubIsIndex")
    @Test
    public void test5() {
        TypeInfo B = javaInspector.parse(INPUT5);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }
}
