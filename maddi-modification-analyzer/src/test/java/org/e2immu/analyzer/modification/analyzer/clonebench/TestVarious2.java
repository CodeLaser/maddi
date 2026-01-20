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
}
