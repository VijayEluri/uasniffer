// Copyright 2007-2009, PensioenPage B.V.
package com.pensioenpage.albizia.delichon.tests.http;

import com.pensioenpage.albizia.delichon.math.Rounding;
import com.pensioenpage.albizia.delichon.http.UserAgentSniffer;

import java.io.*;
import java.util.*;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.xins.common.MandatoryArgumentChecker;
import org.xins.common.text.TextUtils;
import org.xins.common.xml.Element;

/**
 * Tests for the <code>UserAgentSniffer</code> class.
 *
 * @version $Revision: 9774 $ $Date: 2009-06-26 10:25:17 +0200 (vr, 26 jun 2009) $
 * @author <a href="mailto:ernst@pensioenpage.com">Ernst de Haan</a>
 */
public class UserAgentSnifferTests extends Object {

   //-------------------------------------------------------------------------
   // Fields
   //-------------------------------------------------------------------------

   private TestData _testData;


   //-------------------------------------------------------------------------
   // Methods
   //-------------------------------------------------------------------------

   @Before
   public void loadTestData() throws Exception {
      InputStream byteStream = getClass().getResourceAsStream("UserAgentSnifferTests-input.txt");
      Reader      charStream = new InputStreamReader(byteStream, "UTF-8");
      LineNumberReader lines = new LineNumberReader(charStream);

      _testData = new TestData(lines);
   }

   @Test
   public void testUserAgentSniffer() throws Exception {

      long start = System.currentTimeMillis();

      long     maxTestDuration = -0L;
      String maxTestDurationUA = null;
      for (TestData.Entry entry : _testData.getEntries()) {
         String agentString = entry.getAgentString();

         long    testStart = System.currentTimeMillis();
         Element       xml = UserAgentSniffer.buildElement(agentString);
         long testDuration = System.currentTimeMillis() - testStart;

         // System.out.println(getClass().getSimpleName() + ": Sniffed in " + testDuration + " ms: " + agentString);

         if (testDuration > maxTestDuration) {
            maxTestDuration   = testDuration;
            maxTestDurationUA = agentString;
         }

         assertEquals("UserAgent", xml.getLocalName()        );
         assertNull  (             xml.getNamespacePrefix()  );
         assertNull  (             xml.getNamespaceURI()     );
         assertEquals(agentString, xml.getAttribute("string"));

         // Find all <Recognized> names
         Collection<String> actualNames = new HashSet<String>();
         for (Element child : xml.getChildElements("Recognized")) {
            actualNames.add(child.getAttribute("name"));
         }

         // Compare expected and recognized
         Collection<String> actualNames2 = new HashSet<String>(actualNames);
         for (String expectedName : entry.getOutputStrings()) {
            if (actualNames.contains(expectedName)) {
               actualNames.remove(expectedName);
            } else if (expectedName.startsWith("BrowserLocale-")) {
               // skip (TODO)
            } else {
               String message = "For agent string \"" + agentString + "\": Missing expected name \"" + expectedName + "\".";
               System.out.println(message);
               for (String name : actualNames2) {
                  System.out.println("-- did find name: " + name);
               }
               fail(message);
               throw new Error();
            }
         }

         // Some unexpected ones remain
         if (actualNames.size() > 0) {
            fail("For agent string \"" + agentString + "\": Found " + actualNames.size() + " unexpected name(s), like \"" + actualNames.iterator().next() + "\".");
         }
      }

      long      duration = System.currentTimeMillis() - start;
      int      testCount = _testData.getEntries().size();
      double timePerTest = Rounding.roundDecimals(((double) duration) / ((double) testCount), 2);
      System.out.println(getClass().getSimpleName() + ": Performed " + testCount + " tests in " + duration + " ms (which is " + timePerTest + " ms per user agent sniff, on average). Max duration was " + maxTestDuration + " ms, for user agent: \"" + maxTestDurationUA + "\".");
   }


   //-------------------------------------------------------------------------
   // Inner classes
   //-------------------------------------------------------------------------

   private static class TestData {

      //----------------------------------------------------------------------
      // Constructors
      //----------------------------------------------------------------------

      TestData(LineNumberReader reader)
      throws IllegalArgumentException, IOException {

         // Check preconditions
         MandatoryArgumentChecker.check("reader", reader);

         _entries = new ArrayList<Entry>();

         // Process each line
         String line, agentString = null;
         List<String> outputStrings = new ArrayList<String>();
         while ((line = reader.readLine()) != null) {

            // Remove whitespace on both ends
            line = line.trim();

            // Empty line means: next entry;
            // if there is some data, store it and then reset
            if ("".equals(line)) {
               if (agentString != null) {
                  _entries.add(new Entry(agentString, outputStrings));
                  agentString   = null;
                  outputStrings = new ArrayList<String>();
               }

            // Ignore comments
            } else if (line.startsWith("#")) {
               continue;

            // First line or first line after empty line is the agent string
            } else if (agentString == null) {
               agentString = line;

            // Otherwise this is an expected output string
            } else {
               outputStrings.add(line);
            }
         }

         // Add last entry, if any
         if (agentString != null) {
            _entries.add(new Entry(agentString, outputStrings));
         }
      }


      //----------------------------------------------------------------------
      // Fields
      //----------------------------------------------------------------------

      Collection<Entry> _entries;


      //----------------------------------------------------------------------
      // Methods
      //----------------------------------------------------------------------

      public Collection<Entry> getEntries() {
         return _entries;
      }


      //----------------------------------------------------------------------
      // Inner classes
      //----------------------------------------------------------------------

      static class Entry {

         //-------------------------------------------------------------------
         // Constructors
         //-------------------------------------------------------------------

         /**
          * Constructs a new <code>Entry</code>.
          *
          * @param agentString
          *    the user agent string, cannot be <code>null</code> nor empty.
          *
          * @param outputStrings
          *    the expected output strings, cannot be <code>null</code>,
          *    cannot be empty and cannot contain any <code>null</code>, empty
          *    or duplicate elements.
          *
          * @throws IllegalArgumentException
          *    if any of the preconditions failed.
          */
         Entry(String agentString, Collection<String> outputStrings)
         throws IllegalArgumentException {

            // Check preconditions
            if (TextUtils.isEmpty(agentString)) {
               throw new IllegalArgumentException("agentString (" + TextUtils.quote(agentString) + ") is null or empty.");
            } else if (outputStrings == null) {
               throw new IllegalArgumentException("outputStrings " + TextUtils.quote(outputStrings) + " == null (for agent string \"" + agentString + "\")");
            }

            // Copy all output strings
            _outputStrings = new ArrayList<String>();
            for (String s : outputStrings) {
               if (TextUtils.isEmpty(s)) {
                  throw new IllegalArgumentException("One of the output strings is null or empty (for agent string \"" + agentString + "\")");
               } else if (_outputStrings.contains(s)) {
                  throw new IllegalArgumentException("Found duplicate output string \"" + s + "\" (for agent string \"" + agentString + "\")");
               }
               _outputStrings.add(s);
            }

            // Store the agent string
            _agentString = agentString;
         }


         //-------------------------------------------------------------------
         // Fields
         //-------------------------------------------------------------------

         private final String _agentString;
         private final Collection<String> _outputStrings;


         //-------------------------------------------------------------------
         // Methods
         //-------------------------------------------------------------------

         String getAgentString() {
            return _agentString;
         }

         Collection<String> getOutputStrings() {
            return _outputStrings;
         }
      }
   }
}
