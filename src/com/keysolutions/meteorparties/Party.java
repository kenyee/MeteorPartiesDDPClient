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

import java.util.ArrayList;
import java.util.Map;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

/**
 * Party object that is core object for application
 * @author kenyee
 */
public class Party {
    /**
     * This is a reference to the hashmap object in our "data store"
     * so we can look up fields dynamically
     */
    private Map<String, Object> mFields;
    /** This is our object ID */
    private String mDocId;
    /** Whether current user is the owner of this party */
    private boolean mIsOwner;
    /**
     * This is the value for current user's RSVP since that would take
     * a while to find in the fields map
     */
    private String mMyRsvp;
    /** This is the number of people who RSVP'd Yes */
    private int mAttendees;
    /** Last state of current user ID so we can figure out if we need to refresh fields */
    private String mLastMyUserId;
    /** This is the Google Maps marker associated w/ this party */
    private Marker mMarker;
    /** This is a color map used to map the #attendees/10 to a color */
    private static final float colorMap[] = {
        BitmapDescriptorFactory.HUE_BLUE,  
        BitmapDescriptorFactory.HUE_AZURE,  
        BitmapDescriptorFactory.HUE_CYAN,   
        BitmapDescriptorFactory.HUE_GREEN,
        BitmapDescriptorFactory.HUE_YELLOW, 
        BitmapDescriptorFactory.HUE_ORANGE,
        BitmapDescriptorFactory.HUE_MAGENTA,     
        BitmapDescriptorFactory.HUE_ROSE,    
        BitmapDescriptorFactory.HUE_RED, 
        BitmapDescriptorFactory.HUE_VIOLET,  
    };
    
    /**
     * Gets Meteor object ID
     * @return object ID string
     */
    public String getId() {
        return mDocId;
    }
    /**
     * Gets title for party
     * @return title of party
     */
    public String getTitle() {
        return ((String) mFields.get("title"));
    }
    /**
     * Gets description for party
     * @return description of party
     */
    public String getDescription() {
        return ((String) mFields.get("description"));
    }
    /**
     * Gets #attendees (people who rsvp'd yes) for party
     * @return #attendees for party
     */
    public int getAttendees() {
        return mAttendees;
    }
    /**
     * RSVP array of <userID,rsvp>
     * @return array of RSVPs
     */
    @SuppressWarnings("unchecked")
    public ArrayList<Map<String, String>> getRsvps() {
        return ((ArrayList<Map<String, String>>) mFields.get("rsvps"));
    }
    /**
     * Gets value for MyRSVP
     * @return
     */
    public String getMyRsvp() {
        refreshFieldsIfIdChanged();
        return mMyRsvp;
    }
    /**
     * Gets GPS latitude for party
     * @return latitude
     */
    public double getLatitude() {
        return ((Double) mFields.get("lat"));
    }
    /**
     * Gets GPS longitude for party
     * @return longitude
     */
    public double getLongitude() {
        return ((Double) mFields.get("lon"));
    }
    /**
     * Whether party is public
     * @return true if public
     */
    public boolean isPublic() {
        return ((Boolean) mFields.get("public"));
    }
    /**
     * Whether current user is party's owner
     * @return true if current user created party
     */
    public boolean isOwner() {
        refreshFieldsIfIdChanged();
        return mIsOwner;
    }
    

    /**
     * Constructor for Party object
     * @param docId Meteor's object ID for this Party
     * @param fields field/value map reference
     */
    public Party(String docId, Map<String, Object> fields) {
        this.mFields = fields;
        this.mDocId = docId;
        mMarker = null;
        refreshFields();
    }
    
    /**
     * Refreshes any fields dependent on login
     */
    private void refreshFieldsIfIdChanged() {
        if (hasUserIdChanged()) {
            refreshFields();
        }
    }
    /**
     * used to figure out if user ID has changed so we can refresh user ID dependent fields
     * @return true if changed
     */
    private boolean hasUserIdChanged() {
        String myUserId = MyDDPState.getInstance().getUserId();
        if (((myUserId == null) && (mLastMyUserId != null))
                || ((myUserId != null) && (mLastMyUserId == null))
                || (!myUserId.equals(mLastMyUserId))) {
            return true;
        }
        return false;
    }
    
    /**
     * This recalculates any internal fields that would take a long time
     * to calculate/get if we had to reparse the the fields map.
     * Currently only gets the myRsvp and attendees and isOwner fields.
     * NOTE: This also needs to be called when the underlying data is changed by DDP.
     */
    public void refreshFields() {
        String myUserId = MyDDPState.getInstance().getUserId();
        mIsOwner = false;
        if (myUserId != null) {
            mIsOwner = ((String) mFields.get("owner"))
                    .equals(myUserId);
        }
        mLastMyUserId = myUserId;
        mMyRsvp = null;
        ArrayList<Map<String, String>> rsvps = getRsvps();
        int rsvpCount = 0;
        if (rsvps != null) {
            for (Map<String, String> rsvpFields : rsvps) {
                String userId = (String) rsvpFields.get("user");
                String userRsvp = (String) rsvpFields.get("rsvp");
                if (userRsvp.equals("yes")) {
                    rsvpCount++;
                }
                if ((myUserId != null) && myUserId.equals(userId)) {
                    mMyRsvp = userRsvp;
                }
            }
        }
        mAttendees = rsvpCount;
    }

    /**
     * Override so we can print party info easily
     */
    @Override
    public String toString() {
        return getTitle();
    }

    /**
     * Sets Google Maps marker for this party
     * @param marker Google Maps marker
     */
    public void setMarker(Marker marker) {
        if (this.mMarker != null) {
            this.mMarker.remove();
        }
        this.mMarker = marker;
    }
    /**
     * Gets Google Maps marker for this party
     * @return Google Maps marker or null
     */
    public Marker getMarker() {
        return this.mMarker;
    }
    
    /**
     * Calculates marker color based on #attendees
     * @return Google Maps marker color
     */
    public float getMarkerColor() {
        // map attendee count to color
        if (mAttendees > 100) {
            return BitmapDescriptorFactory.HUE_VIOLET;
        } else {
            return colorMap[(int)(mAttendees / 10)];
        }
    }
}


