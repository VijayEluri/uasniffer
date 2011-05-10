// BSD-licensed, see COPYRIGHT file
// Copyright 2011, Ernst de Haan
package org.znerd.uasniffer;

/**
 * Class responsible for determining the user agent details.
 * 
 * @author <a href="mailto:anthony.goubard@japplis.com">Anthony Goubard</a>
 * @author <a href="mailto:mees@wittemansoftware.nl">Mees Witteman</a>
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 */
public final class Sniffer {

   /**
    * Analyzes the specified user agent string.
    *
    * @param agentString
    *    the user agent string, cannot be <code>null</code>.
    *
    * @return
    *    an {@link UserAgent} instance that describes the user agent,
    *    never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>agentString == null</code>.
    */
   public static final UserAgent analyze(String agentString)
   throws IllegalArgumentException {

      UserAgent ua = new UserAgent(agentString);
      analyze(ua);

      return ua;
   }

   private static final void analyze(final UserAgent ua) {

      String agentString = ua.getLowerCaseAgentString();

      // Detect specific devices
      boolean    android = agentString.contains("android");
      boolean appleTouch = agentString.contains("ipod") || agentString.contains("iphone");

      // Mobile devices
      boolean          matchFound = false;
      String               uaType = "desktop";
      boolean supportsTelProtocol = false;
      boolean     supportsScripts = true;
      boolean       supportsFlash = true;
      boolean           isBrowser = true;
      for (int i=0; i < UA_MOBILE_DEVICE_SNIPPETS.length; i++) {
         if (agentString.contains(UA_MOBILE_DEVICE_SNIPPETS[i])) {
            matchFound          = true;
            uaType              = "mobile";
            supportsTelProtocol = true;
            supportsScripts     = false;
            supportsFlash       = false;

            for (int j=0; j < UA_MOBILE_DEVICE_WITHOUT_TEL_SUPPORT.length; j++) {
               if (agentString.contains(UA_MOBILE_DEVICE_WITHOUT_TEL_SUPPORT[j])) {
                  supportsTelProtocol = false;
               }
            }
         }
      }

      // iPod
      if (!matchFound && agentString.contains("ipod")) {
         matchFound          = true;
         uaType              = "desktop";
         supportsTelProtocol = false;
         supportsScripts     = true;
         supportsFlash       = false;

      // iPhone
      } else if (!matchFound && agentString.contains("iphone")) {
         matchFound          = true;
         uaType              = "desktop";
         supportsTelProtocol = true;
         supportsScripts     = true;
         supportsFlash       = false;

      // Android
      } else if (!matchFound && agentString.contains("android")) {
         matchFound          = true;
         uaType              = "desktop";
         supportsTelProtocol = true;
         supportsScripts     = true;
         supportsFlash       = false;

      // Palm Pre
      } else if (!matchFound && agentString.contains("pre/")) {
         matchFound          = true;
         uaType              = "desktop";
         supportsTelProtocol = true;
         supportsScripts     = true;
         supportsFlash       = false;

      // Bots
      } else if (!matchFound) {
         for (int i=0; i < UA_BOT_SNIPPETS.length; i++) {
            if (agentString.contains(UA_BOT_SNIPPETS[i])) {
               matchFound          = true;
               uaType              = "bot";
               supportsTelProtocol = false;
               supportsScripts     = false;
               supportsFlash       = false;
               isBrowser           = false;
            }
         }
      }

      ua.setBrowser           (isBrowser);
      ua.setType              (uaType);
      ua.setTelProtocolSupport(supportsTelProtocol);
      ua.setJavaScriptSupport (supportsScripts);
      ua.setFlashSupport      (supportsFlash);

      // Categorize Device
      if (supportsTelProtocol) {
         ua.addName("Device-Phone");
      } else {
         ua.addName("Device-NoPhone");
      }
      if ("mobile".equals(uaType) || appleTouch || android || agentString.contains("webos/")) {
         ua.addName("Device-Mobile");
      } else if ("bot".equals(uaType)) {
         ua.addName("Device-Bot");
      } else {
         ua.addName("Device-Desktop");
      }
      if (appleTouch) {
         ua.addName("Device-AppleTouch");
         if (agentString.contains("ipod")) {
            ua.addName("Device-AppleTouch-iPod");
         } else {
            ua.addName("Device-AppleTouch-iPhone");
         }
      } else if (agentString.contains("blackberry")) {
         analyze(ua, agentString, "Device-Blackberry", "blackberry", 1, false);
      }

      // Detect OS, browser engine and browser
      if (! "bot".equals(uaType)) {
         detectOS           (ua);
         detectBrowserEngine(ua);
         detectBrowser      (ua);
      }
   }

   private static final void detectOS(UserAgent ua) {

      String agentString = ua.getLowerCaseAgentString();

      // Linux
      if (agentString.contains("linux")) {
         ua.addName("BrowserOS-NIX");
         ua.addName("BrowserOS-Linux");
         if (agentString.contains("linux 2.")) {
            analyze(ua, agentString, "BrowserOS-Linux", "linux ");
         }

         // Android
         if (agentString.contains("android")) {
            analyze(ua, agentString, "BrowserOS-Linux-Android", "android ");
         }

      // Google Chrome OS
      } else if (agentString.contains("cros ")) {
         ua.addName("BrowserOS-CrOS");

      // webOS, by Palm
      } else if (agentString.contains("webos/")) {
         analyze(ua, agentString, "BrowserOS-WebOS", "webos/");

      // iPhone OS (detect before Mac OS)
      } else if (agentString.contains("iphone") || agentString.contains("ipod")) {
         analyze(ua, agentString.replace('_', '.'), "BrowserOS-iPhoneOS", "iPhone OS ");

      // Mac OS
      } else if (agentString.contains("mac os") || agentString.contains("mac_") || agentString.contains("macintosh")) {

         ua.addName("BrowserOS-MacOS");

         // Mac OS X
         if (agentString.contains("mac os x")) {
            ua.addName("BrowserOS-NIX");
            ua.addName("BrowserOS-MacOS-10");
            analyze(ua, agentString.replace('_', '.'), "BrowserOS-MacOS", "mac os x ",              0, false);
            analyze(ua, agentString.replace('_', '.'), "BrowserOS-MacOS", "mac os x tiger ",        0, false);
            analyze(ua, agentString.replace('_', '.'), "BrowserOS-MacOS", "mac os x leopard ",      0, false);
            analyze(ua, agentString.replace('_', '.'), "BrowserOS-MacOS", "mac os x snow leopard ", 0, false);
            analyze(ua, agentString.replace('_', '.'), "BrowserOS-MacOS", "mac os x lion ",         0, false);
         }

      // Windows
      } else if (agentString.contains("windows") || agentString.contains("win3.") || agentString.contains("win9") || agentString.contains("winnt") || agentString.contains("wince")) {
         ua.addName("BrowserOS-Windows");
         if (agentString.contains("windows nt")) {
            analyze(ua, agentString, "BrowserOS-Windows-NT", "windows nt ", 2, true);
         } else if (agentString.contains("windows 5.") || agentString.contains("windows 6.")) {
            analyze(ua, agentString, "BrowserOS-Windows-NT", "windows ", 2, false);
         } else if (agentString.contains("windows vista")) {
            analyze(ua, "nt/6.0", "BrowserOS-Windows-NT", "nt/", 2, false);
         } else if (agentString.contains("windows xp")) {
            analyze(ua, "nt/5.1", "BrowserOS-Windows-NT", "nt/", 2, false);
         } else if (agentString.contains("windows 2000")) {
            analyze(ua, "nt/5.0", "BrowserOS-Windows-NT", "nt/", 2, false);
         } else if (agentString.contains("winnt")) {
            analyze(ua, agentString, "BrowserOS-Windows-NT", "winnt", 2, true);

         // Windows ME (needs to be checked before Windows 98)
         } else if (agentString.contains("win 9x 4.90") || agentString.contains("windows me")) {
            ua.addName("BrowserOS-Windows-ME");

         // Windows 98
         } else if (agentString.contains("windows 98") || agentString.contains("win98")) {
            ua.addName("BrowserOS-Windows-98");

         // Windows 95
         } else if (agentString.contains("windows 95") || agentString.contains("win95")) {
            ua.addName("BrowserOS-Windows-95");

         // Windows Mobile
         } else if (agentString.contains("windows mobile") || agentString.contains("windows; ppc") || agentString.contains("windows ce") || agentString.contains("wince")) {
            analyze(ua, agentString, "BrowserOS-Windows-Mobile", "windows mobile ", 3, true);

         // Windows 3.x
         } else if (agentString.contains("windows 3.")) {
            analyze(ua, agentString, "BrowserOS-Windows", "windows ", 3, true);
         } else if (agentString.contains("win3.")) {
            int    indexWin3 = agentString.indexOf("win3.");
            int indexWindows = agentString.indexOf("windows");
            String         s = (indexWindows >= 0 && indexWindows < indexWin3) ? agentString.substring(indexWindows + 1) : agentString;

            analyze(ua, s, "BrowserOS-Windows", "win", 3, true);
         }

         // Add some marketing names for various Windows versions
         if (ua.hasName("BrowserOS-Windows-NT-5-0")) {
            ua.addName("BrowserOS-Windows-2000");
         } else if (ua.hasName("BrowserOS-Windows-NT-5")) {
            ua.addName("BrowserOS-Windows-XP");
         } else if (ua.hasName("BrowserOS-Windows-NT-6-0")) {
            ua.addName("BrowserOS-Windows-Vista");
         } else if (ua.hasName("BrowserOS-Windows-NT-6-1")) {
            ua.addName("BrowserOS-Windows-7");
         }

      // DragonFlyBSD, extra check
      } else if (agentString.contains("dragonfly")) {
         ua.addName("BrowserOS-NIX");
         ua.addName("BrowserOS-BSD");
         ua.addName("BrowserOS-BSD-DragonFlyBSD");

      // Other BSD variants
      } else if (agentString.contains("bsd")) {
         ua.addName("BrowserOS-NIX");
         ua.addName("BrowserOS-BSD");
         if (agentString.contains("netbsd")) {
            ua.addName("BrowserOS-BSD-NetBSD");
         } else if (agentString.contains("openbsd")) {
            ua.addName("BrowserOS-BSD-OpenBSD");
         } else if (agentString.contains("freebsd")) {
            ua.addName("BrowserOS-BSD-FreeBSD");
         }

      // AIX
      } else if (agentString.contains("aix")) {
         ua.addName("BrowserOS-NIX");
         ua.addName("BrowserOS-AIX");

      // IRIX
      } else if (agentString.contains("irix")) {
         ua.addName("BrowserOS-NIX");
         ua.addName("BrowserOS-IRIX");

      // HP-UX
      } else if (agentString.contains("hp-ux")) {
         ua.addName("BrowserOS-NIX");
         ua.addName("BrowserOS-HPUX");

      // Sun Solaris
      } else if (agentString.contains("sunos")) {
         ua.addName("BrowserOS-NIX");
         analyze(ua, agentString, "BrowserOS-Solaris", "sunos ", 1, false);

      // Sun Solaris
      } else if (agentString.contains("beos")) {
         ua.addName("BrowserOS-BeOS");

      // OS/2 (a.k.a. Ecomstation)
      } else if (agentString.contains("(os/2")) {
         analyze(ua, agentString, "BrowserOS-OS2", "warp ", 1, false);
      }
   }

   private static final void detectBrowserEngine(UserAgent ua) {

      String agentString = ua.getLowerCaseAgentString();

      // Apple WebKit
      if (agentString.contains("applewebkit/")) {
         analyze(ua, agentString, "BrowserEngine-WebKit", "applewebkit/", 4, false);

      // Mozilla Gecko
      } else if (agentString.contains("gecko/")) {
         analyze(ua, agentString, "BrowserEngine-Gecko", "rv:", 4, false);

      // Opera Presto
      } else if (agentString.contains("presto/")) {
         analyze(ua, agentString, "BrowserEngine-Presto", "presto/", 3, false);
      } else if (agentString.contains("presto")) {
         analyze(ua, agentString, "BrowserEngine-Presto", "presto ", 3, false);

      // Microsoft Trident
      } else if (agentString.contains("trident/")) {
         analyze(ua, agentString, "BrowserEngine-Trident", "trident/", 3, false);
      } else if (agentString.contains("trident")) {
         analyze(ua, agentString, "BrowserEngine-Trident", "trident ", 3, false);

      // KDE KHTML
      } else if (agentString.contains("khtml/")) {
         analyze(ua, agentString, "BrowserEngine-KHTML", "khtml/", 3, false);
      }
   }

   private static final void detectBrowser(UserAgent ua) {

      String agentString = ua.getLowerCaseAgentString();

      // Lunascape, can use different rendering engines
      // E.g.: Lunascape5 (Webkit) - Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/528+ (KHTML, like Gecko, Safari/528.0) Lunascape/5.0.3.0
      if (agentString.contains("lunascape")) {
         analyze(ua, agentString, "Browser-Lunascape", "lunascape ", 4, false);
         analyze(ua, agentString, "Browser-Lunascape", "lunascape/", 4, false);

      // Maxthon
      } else if (agentString.contains("maxthon")) {
         analyze(ua, agentString, "Browser-Maxthon", "maxthon ");

      // Konqueror (needs to be detected before Gecko-based browsers)
      // E.g.: Mozilla/5.0 (compatible; Konqueror/4.1; Linux) KHTML/4.1.2 (like Gecko)
      } else if (agentString.contains("konqueror")) {
         analyze(ua, agentString, "Browser-Konqueror", "konqueror/", 2, false);
         ua.addName("BrowserEngine-KHTML");

      // Fennec
      // E.g.: Mozilla/5.0 (Macintosh; U; Intel Mac OS X; en-US; rv:1.9.2a1pre) Gecko/20090317 Fennec/1.0b1
      //       Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1b2pre) Gecko/20081015 Fennec/1.0a1
      //       Mozilla/5.0 (X11; U; Linux armv7l; en-US; rv:1.9.2a1pre) Gecko/20090322 Fennec/1.0b2pre
      } else if (agentString.contains("fennec")) {
         analyze(ua, agentString, "Browser-Fennec", "fennec/");

      // Epiphany
      // E.g.: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.7.3) Gecko/20041007 Epiphany/1.4.7
      } else if (agentString.contains("epiphany")) {
         analyze(ua, agentString, "Browser-Epiphany", "epiphany/");

      // Flock (needs to be detected before Firefox)
      // E.g.: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.18) Gecko/20081107 Firefox/2.0.0.18 Flock/1.2.7
      } else if (agentString.contains("flock")) {
         analyze(ua, agentString, "Browser-Flock", "flock/");

      // Camino (needs to be detected before Firefox)
      // E.g.: Mozilla/5.0 (Macintosh; U; Intel Mac OS X; nl; rv:1.8.1.14) Gecko/20080512 Camino/1.6.1 (MultiLang) (like Firefox/2.0.0.14)
      } else if (agentString.contains("camino")) {
         analyze(ua, agentString, "Browser-Camino", "camino/");

      // SeaMonkey
      // E.g.: Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.1b3pre) Gecko/20090302 SeaMonkey/2.0b1pre
      } else if (agentString.contains("seamonkey/")) {
         analyze(ua, agentString, "Browser-SeaMonkey", "seamonkey/");

      // SeaMonkey (again)
      // E.g.: Seamonkey-1.1.13-1(X11; U; GNU Fedora fc 10) Gecko/20081112
      } else if (agentString.contains("seamonkey-")) {
         analyze(ua, agentString, "Browser-SeaMonkey", "seamonkey-");
         ua.addName("BrowserEngine-Gecko");

      // Netscape Navigator (needs to be detected before Firefox)
      // E.g.: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.5pre) Gecko/20070712 Firefox/2.0.0.4 Navigator/9.0b2
      } else if (agentString.contains("navigator/")) {
         analyze(ua, agentString, "Browser-Netscape", "navigator/");
         ua.addName("BrowserEngine-Gecko");

      // Firefox
      } else if (agentString.contains("firefox")) {
         analyze(ua, agentString, "Browser-Firefox", "firefox/");
      } else if (agentString.contains("minefield/")) {
         analyze(ua, agentString, "Browser-Firefox", "minefield/");
      } else if (agentString.contains("namoroka/")) {
         analyze(ua, agentString, "Browser-Firefox", "namoroka/"); // Firefox 3.6 pre-releases
      } else if (agentString.contains("shiretoko/")) {
         analyze(ua, agentString, "Browser-Firefox", "shiretoko/"); // Firefox 3.5 pre-releases
      } else if (agentString.contains("firebird/")) {
         analyze(ua, agentString, "Browser-Firefox", "firebird/"); // Before 1.0
      } else if (agentString.contains("phoenix/")) {
         analyze(ua, agentString, "Browser-Firefox", "phoenix/"); // Before 1.0 (and before Firebird code-name)

      // Opera
      } else if (agentString.startsWith("opera/")) {

         ua.addName("BrowserEngine-Presto");
         ua.addName("Browser-Opera");

         // Opera Mobile
         if (agentString.contains("mobi/")) {
            analyze(ua, agentString, "Browser-OperaMobile", agentString.contains("version/") ? "version/" : "opera/", 3, true);

         // Opera Mini
         } else if (agentString.contains("mini/")) {
            analyze(ua, agentString, "Browser-OperaMini", "mini/", 3, true);

         // Opera Desktop
         } else {
            analyze(ua, agentString, "Browser-OperaDesktop", agentString.contains("version/") ? "version/" : "opera/", 3, true);
         }

      // Opera (older releases)
      } else if (agentString.contains("opera")) {
         ua.addName("Browser-Opera");
         analyze(ua, agentString, "Browser-OperaDesktop", "opera ");
         ua.addName("BrowserEngine-Presto");

      // Palm Pre browser - this one needs to be checked before Safari
      } else if (agentString.contains("pre/")) {
         analyze(ua, agentString, "Browser-PalmPreBrowser", "version/");

      // OmniWeb - this one needs to be checked before Safari
      } else if (agentString.contains("omniweb")) {
         ua.addName("Browser-OmniWeb");

      // RockMelt - this one needs to be checked before Google Chrome
      // e.g.: Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/534.13 (KHTML, like Gecko) RockMelt/0.9.48.51 Chrome/9.0.597.107 Safari/534.13
      } else if (agentString.contains("rockmelt")) {
         analyze(ua, agentString, "Browser-RockMelt", "rockmelt/", 4, false);

      // Google Chrome - this one needs to be checked before Safari
      // e.g.: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Chrome/0.X.Y.Z Safari/525.13.
      } else if (agentString.contains("chrome")) {
         analyze(ua, agentString, "Browser-Chrome", "chrome/", 4, false);

      // Apple Safari
      } else if (agentString.contains("safari")) {
         ua.addName("BrowserEngine-WebKit");
         ua.addName("Browser-Safari"      );

         if (agentString.contains("mobile/") || agentString.contains("android")) {
            analyze(ua, agentString, "Browser-MobileSafari", "version/");
         } else {
            analyze(ua, agentString, "Browser-DesktopSafari", "version/");
         }

      // Netscape (again)
      } else if (agentString.contains("netscape6")) {
         analyze(ua, agentString, "Browser-Netscape", "netscape6/");
         ua.addName("Browser-Netscape");
         ua.addName("Browser-Netscape-6");
         ua.addName("BrowserEngine-Gecko");
      } else if (agentString.contains("netscape")) {
         analyze(ua, agentString, "Browser-Netscape", "netscape/", 3, true);
         ua.addName("BrowserEngine-Gecko");

      // iCab
      // E.g.: iCab/4.5 (Macintosh; U; Mac OS X Leopard 10.5.7)
      } else if (agentString.contains("icab")) {
         analyze(ua, agentString, "Browser-iCab", "icab/");
         analyze(ua, agentString, "Browser-iCab", "icab ");

         // iCab 4 uses the WebKit rendering engine, although the user agent
         // string does not advertise that
         if (ua.hasName("Browser-iCab-4")) {
            ua.addName("BrowserEngine-WebKit");
         }

      // Internet Explorer
      } else if (agentString.contains("msie")) {
         ua.addName("BrowserEngine-Trident");
         ua.addName("Browser-MSIE"         );

         // Mobile IE
         if (agentString.contains("iemobile")) {
            analyze(ua, agentString, "Browser-MobileMSIE", "iemobile ", 3, true);
         } else if (ua.hasName("BrowserOS-Windows-Mobile")) {
            ua.addName("Browser-MobileMSIE");
         } else {
            analyze(ua, agentString, "Browser-DesktopMSIE", "msie ", 3, true);
         }

      // Netscape 4
      } else if (! agentString.contains("(compatible") && (agentString.startsWith("mozilla/4.") || agentString.startsWith("mozilla/3."))) {
         analyze(ua, agentString, "Browser-Netscape", "mozilla/", 3, true);
      }
   }

   private static final void analyze(UserAgent ua, String agentString, String basicName, String versionPrefix) {
      analyze(ua, agentString, basicName, versionPrefix, 3, false);
   }

   private static final void analyze(UserAgent ua, String agentString, String basicName, String versionPrefix, int minVersionParts, boolean splitSecondVersionPart) {

      versionPrefix = versionPrefix.toLowerCase();

      // First add the basic name
      ua.addName(basicName);

      // Find the location of the version number after the prefix
      int index = agentString.indexOf(versionPrefix);
      if (index >= 0) {

         // Get the version number in a string
         String version = cutVersionEnd(agentString.substring(index + versionPrefix.length()).trim());
         // XXX: System.err.println("User agent \"" + ua + "\": Found version number \"" + version + "\".");

         if (version.length() > 0) {

            // Split the version number in pieces
            String[] versionParts = version.split("\\.");

            // First version part can always be done immediately
            String specificName = basicName + '-' + versionParts[0];
            ua.addName(specificName);

            int versionPartsFound;
            if (splitSecondVersionPart && versionParts.length == 2) {
               versionPartsFound = 1;

               String secondVersionPart = versionParts[1];
               for (int i = 0; i < secondVersionPart.length(); i++) {
                  specificName += "-" + secondVersionPart.charAt(i);
                  ua.addName(specificName);
                  versionPartsFound++;
               }
            } else {
               for (int i = 1; i < versionParts.length; i++) {
                  specificName += '-' + versionParts[i];
                  ua.addName(specificName);
               }
               versionPartsFound = versionParts.length;
            }

            for (int i = versionPartsFound; i < minVersionParts; i++) {
               specificName += "-0";
               ua.addName(specificName);
            }
         }
      }
   }

   private static final String cutVersionEnd(String s) {
      String result = "";
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (Character.isDigit(c) || c == '.') {
            result += c;
         } else {
            break;
         }
      }

      return result;
   }


   private static final String[] UA_MOBILE_DEVICE_SNIPPETS = new String[] {
      "windows ce", "windowsce", "symbian", "nokia", "opera mini", "wget", "fennec", "opera mobi", "windows; ppc", "blackberry"
   };

   private static final String[] UA_MOBILE_DEVICE_WITHOUT_TEL_SUPPORT = new String[] {
      "opera/8.", "opera/7.", "opera/6.", "opera/5.", "opera/4.", "opera/3.", "ipod"
   };

   private static final String[] UA_BOT_SNIPPETS = new String[] {
      "spider", "bot", "crawl", "miner", "checker", "java", "pingdom"
   };

   private Sniffer() {
      // empty
   }
}
