/*
* (c)Copyright 2013 Ken Yee, KEY Enterprise Solutions 
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.keysolutions.meteorparties;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

/**
 * Holds global application state using Android's Application object
 * that is specified in the AndroidManifest.xml
 * @author kenyee
 */
public class MyApplication extends Application {
    /** Android application context */
    private static Context sContext = null;
    
    /** Saves current party ID to share between fragments/activities **/
    private static String sSelectedPartyId = null;
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MyApplication.sContext = getApplicationContext();
        // Initialize the singletons so their instances
        // are bound to the application process.
        initSingletons();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    /**
     * Initializes any singleton classes
     */
    protected void initSingletons() {
        // Initialize App DDP State Singleton
        MyDDPState.initInstance(MyApplication.sContext);
    }
    
    /**
     * Gets application context
     * @return Android application context
     */
    public static Context getAppContext() {
        return MyApplication.sContext;
    }
    
    /**
     * Gets currently selected Party ID
     * @return current party ID
     */
    public static String getSelectedPartyId() {
       return sSelectedPartyId;
    }
    
    /**
     * Sets currently selected Party ID
     * @param id current party ID
     */
    public static void setSelectedPartyId(String id) {
        sSelectedPartyId = id;
    }
}
