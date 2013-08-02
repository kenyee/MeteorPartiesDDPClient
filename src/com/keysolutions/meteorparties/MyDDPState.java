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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.keysolutions.ddpclient.DDPListener;
import com.keysolutions.ddpclient.DDPClient.DdpMessageField;
import com.keysolutions.ddpclient.DDPClient.DdpMessageType;
import com.keysolutions.ddpclient.android.DDPStateSingleton;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Implements specific DDP state/commands for this application
 * @author kenyee
 *
 * This is a singleton class that should be initialized in your MyApplication singleton.
 */
public class MyDDPState extends DDPStateSingleton {
    /** collection of Parties */
    private Map<String, Party> mParties;
    
    /**
     * Constructor for this singleton (private because it's a singleton)
     * @param context Android application context
     */
    private MyDDPState(Context context) {
        // Constructor hidden because this is a singleton
        super(context);
        mParties = new ConcurrentHashMap<String, Party>();
    }

    /**
     * Used by MyApplication singleton to initialize this singleton
     * @param context Android application context
     */
    public static void initInstance(Context context) {
        // only called by MyApplication
        if (mInstance == null) {
            // Create the instance
            mInstance = new MyDDPState(context);
        }
    }
    
    /**
     * Gets only instance of this singleton
     * @return this singleton object
     */
    public static MyDDPState getInstance() {
        // Return the instance
        return (MyDDPState) mInstance;
    }
    
    /**
     * Gets current collection of Parties
     * @return parties collection
     */
    public Map<String, Party> getParties() {
        return mParties;
    }
    
    /**
     * Gets specified Party object
     * @param partyId Meteor ID of party
     * @return Party object if found
     */
    public Party getParty(String partyId) {
        if (!mParties.containsKey(partyId)) {
            return null;
        }
        return mParties.get(partyId);
    }
    
    /**
     * Lets us lightly wrapper default implementation's objects
     * instead of using our own DB if we had to override 
     * the add/remove/updateDoc methods
     */
    @Override
    public void broadcastSubscriptionChanged(String collectionName,
            String changetype, String docId) {
        if (collectionName.equals("parties")) {
            if (changetype.equals(DdpMessageType.ADDED)) {
                mParties.put(docId, new Party(docId, (Map<String, Object>) getCollection(collectionName).get(docId)));
            } else if (changetype.equals(DdpMessageType.REMOVED)) {
                mParties.remove(docId);
            } else if (changetype.equals(DdpMessageType.UPDATED)) {
                mParties.get(docId).refreshFields();
            }
        }
        // do the broadcast after we've taken care of our parties wrapper
        super.broadcastSubscriptionChanged(collectionName, changetype, docId);
    }
    
    ////// Meteor methods on the server for this application
    /**
     * Handles method to create a new party on the server
     * @param title Title of party
     * @param description Description of party
     * @param lat GPS latitude of party
     * @param lon GPS longitude of party
     * @param isPublic whether party is public
     */
    public void createParty(String title, String description, double lat,
            double lon, boolean isPublic) {
    }

    /**
     * Used to invite a user to the party if it's not public
     * @param partyId Meteor object ID of party
     * @param userId Meteor object ID of user to invite
     */
    public void invite(String partyId, String userId) {
    }

    /**
     * Handles RSVP method call
     * @param partyId Meteor object ID of party
     * @param yesNoMaybe "yes", "no" or "maybe" RSVP response for current user
     */
    public void rsvp(String partyId, String yesNoMaybe) {
        Object[] methodArgs = new Object[2];
        methodArgs[0] = partyId;
        methodArgs[1] = yesNoMaybe;
        mDDP.call("rsvp", methodArgs, new DDPListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void onResult(Map<String, Object> jsonFields) {
                if (jsonFields.containsKey("result")) {
                    Map<String, Object> result = (Map<String, Object>) jsonFields
                            .get(DdpMessageField.RESULT);
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(MESSAGE_METHODRESUlT);
                    broadcastIntent.putExtra(MESSAGE_EXTRA_RESULT,
                            mGSON.toJson(result));
                    LocalBroadcastManager.getInstance(
                            MyApplication.getAppContext()).sendBroadcast(
                            broadcastIntent);
                    // broadcast a msg that RSVP was successful so UI can
                    // update?
                }
                if (jsonFields.containsKey("error")) {
                    Map<String, Object> error = (Map<String, Object>) jsonFields
                            .get(DdpMessageField.ERROR);
                    broadcastDDPError((String) error.get("message"));
                    // should this be an RSVP error result instead?
                }
            }
        });
    }
}
