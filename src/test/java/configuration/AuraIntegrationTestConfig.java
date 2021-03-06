/*
 * Copyright (C) 2014 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package configuration;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.auraframework.test.util.PooledRemoteWebDriverFactory;
import org.auraframework.test.util.RemoteWebDriverFactory;
import org.auraframework.test.util.SauceUtil;
import org.auraframework.test.SeleniumServerLauncher;
import org.auraframework.test.util.WebDriverProvider;
import org.auraframework.test.configuration.JettyTestServletConfig;
import org.auraframework.util.ServiceLoaderImpl.AuraConfiguration;
import org.auraframework.util.ServiceLoaderImpl.Impl;
import org.auraframework.util.test.configuration.TestServletConfig;

import org.openqa.selenium.net.PortProber;

@AuraConfiguration
public class AuraIntegrationTestConfig {
    @Impl
    public static TestServletConfig auraJettyServletTestInfo() throws Exception {
        return new JettyTestServletConfig();
    }

    /**
     * Returns a factory to build the appropriate WebDriver that connects to a given URL. This URL can be one of the
     * following: 1. Passed in as a property by the user 2. A Saucelabs address containing a specified Sauce
     * username/access key 3. The localhost with a generated open port number
     */
    @Impl
    public static WebDriverProvider auraWebDriverProvider() throws Exception {
        URL serverUrl;
        boolean runningOnSauceLabs = SauceUtil.areTestsRunningOnSauce();
        try {
            String hubUrlString = System.getProperty(WebDriverProvider.WEBDRIVER_SERVER_PROPERTY);
            if ((hubUrlString != null) && !hubUrlString.equals("")) {
                if (runningOnSauceLabs)
                    serverUrl = SauceUtil.getSauceServerUrl();
                else
                    serverUrl = new URL(hubUrlString);
            } else {
                int serverPort = PortProber.findFreePort();

                // quiet the verbose grid logging
                Logger selLog = Logger.getLogger("org.openqa");
                selLog.setLevel(Level.SEVERE);

                SeleniumServerLauncher.start("localhost", serverPort);
                serverUrl = new URL(String.format("http://localhost:%s/wd/hub", serverPort));
                System.setProperty(WebDriverProvider.WEBDRIVER_SERVER_PROPERTY, serverUrl.toString());
            }
            Logger.getLogger(AuraIntegrationTestConfig.class.getName()).info("Selenium server url: " + serverUrl);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error(e);
        }

        // Check if we should reuse the same browser during tests.
        // This property can be set by sending in -Dwebdriver.reusebrowser
        if (!runningOnSauceLabs && Boolean.parseBoolean(System.getProperty(WebDriverProvider.REUSE_BROWSER_PROPERTY))) {
            return new PooledRemoteWebDriverFactory(serverUrl);
        } else {
            return new RemoteWebDriverFactory(serverUrl);
        }
    }
}
