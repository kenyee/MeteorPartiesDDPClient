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

import java.util.HashMap;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.keysolutions.ddpclient.android.DDPBroadcastReceiver;
import com.keysolutions.ddpclient.android.DDPStateSingleton;

/**
 * A Google map fragment showing parties on a map.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class PartyMapFragment extends SupportMapFragment {
    private final static String TAG = "PartyMapActivity";

    private static final float ZOOM_LEVEL = 15;

    /** reference to Google Maps object */
    private GoogleMap mMap;

    /** broadcast receiver */
    private BroadcastReceiver mReceiver;

    /** for storing selected party in bundle */
    private static final String STATE_SELECTED_PARTY = "selected_party";
    
    /** for looking up Party from marker */
    private HashMap<String, Party> mPartyMarkerMap;

    /**
     * The fragment's current callback object, which is notified of map party
     * clicks.
     */
    @SuppressWarnings("unused")
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onPartySelected(String id, boolean clickThrough);

        /**
         * Callback to find out if our parent activity is in two pane mode for
         * handling marker click behavior
         */
        public boolean isTwoPaneMode();

        /**
         * Callback to update login buttons in onResume because action bar isn't recreated
         */
        public void updateLoginButtons();
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onPartySelected(String id, boolean clickThrough) {
        }

        @Override
        public boolean isTwoPaneMode() {
            return false;
        }

        @Override
        public void updateLoginButtons() {
        }
    };

    /** called when this fragment is attached to an activity */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    /**
     * called after the activity has been created so we can initialize the map
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // restore selected party if any
        // NOTE: this doesn't work because if you use setRetainInstance(true) to let GoogleMaps
        // retain internal state so it doesn't have to re-init so much:
        // http://stackoverflow.com/questions/11353075/how-can-i-maintain-fragment-state-when-added-to-the-back-stack
        // http://stackoverflow.com/questions/15545214/android-using-savedinstancestate-with-fragments
        if (savedInstanceState != null) {
            MyApplication.setSelectedPartyId(savedInstanceState
                    .getString(STATE_SELECTED_PARTY));
        }

        int status = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(getActivity());
        if (status == ConnectionResult.SUCCESS) {
            GoogleMap mapFragment = getMap();
            if (mapFragment != null) {
                if (savedInstanceState != null) {
                    // restore selected party ID
                    MyApplication.setSelectedPartyId(savedInstanceState
                            .getString(STATE_SELECTED_PARTY));
                    // Reincarnated activity. The obtained map is the same map
                    // instance in the previous activity life cycle.
                    // There is no need to reinitialize it if setRetainInstance is set.
                    // However, you still have to add all your listeners to it later.
                    mMap = getMap();
                } else {
                    // First incarnation of this activity.
                    // set retaininstance to minimize rotation time w/ google maps
                    /*this.setRetainInstance(true);*/
                }
                initMap();
            } else {
                Log.e(TAG, "Couldn't find Google map fragment!");
            }
        } else {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status,
                    getActivity(), 42/*
                                      * some random requestCode...value isn't
                                      * important
                                      */);
            dialog.show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // persist the activated party item
        outState.putString(STATE_SELECTED_PARTY,
                MyApplication.getSelectedPartyId());
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mReceiver != null) {
            // unhook the receiver
            LocalBroadcastManager.getInstance(getActivity())
                    .unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        // unhook any map markers
        for (Party party : MyDDPState.getInstance().getParties().values()) {
            party.setMarker(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // needed to update login buttons if on smartphone because the action bar
        // isn't refreshed when you come back to this activity after login activity
        ((Callbacks) getActivity()).updateLoginButtons();
        
        // re-hook or init map
        initMap();

        // get ready to handle DDP events
        mReceiver = new DDPBroadcastReceiver(MyDDPState.getInstance(), getActivity()) {
            @Override
            protected void onDDPConnect(DDPStateSingleton ddp) {
                super.onDDPConnect(ddp);
                // add our subscriptions needed for the activity here
                ddp.subscribe("parties", new Object[] {});
                ddp.subscribe("directory", new Object[] {});
            }
            @Override
            protected void onSubscriptionUpdate(String changeType,
                    String subscriptionName, String docId) {
                if (subscriptionName.equals("parties")) {
                    // show any new parties
                    showVisibleParties();
                }
            }
            @Override
            protected void onLogin() {
                // update login/logout action button
                getActivity().invalidateOptionsMenu();
            }
            @Override
            protected void onLogout() {
                // update login/logout action button
                getActivity().invalidateOptionsMenu();
            }
        };
        MyDDPState.getInstance().connectIfNeeded();    // start connection process if we're not connected
    }

    /**
     * used to initialize the map
     */
    private void initMap() {
        if (mMap == null) {
            mMap = getMap();
            if (mMap != null) {
                initialMapSetup();
            }
        } else {
            hookMap();
        }
    }

    /**
     * initial setup of map
     */
    private void initialMapSetup() {
        hookMap();

        // zoom to current location if available, otherwise use camera
        // changelistener
        mMap.animateCamera(CameraUpdateFactory.zoomTo(ZOOM_LEVEL), 2000, null);

        // set map location to current location
        // Location curLoc = getCurrentLocation();
        // hardcode San Fran downtown to be compatible w/ Parties demo app
        Location curLoc = new Location("network");
        curLoc.setLatitude(37.78212);
        curLoc.setLongitude(-122.40146);
        if (curLoc != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(curLoc.getLatitude(), curLoc.getLongitude()),
                    ZOOM_LEVEL));
        }
    }

    /**
     * Hooks all listeners into the map (needed if you rotate screen
     * or onResume)
     */
    public void hookMap() {
        mMap.setMyLocationEnabled(true);
        // handle user drags of map
        mMap.setOnCameraChangeListener(new OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                showVisibleParties();
            }
        });
        // handle user clicks on markers
        mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Party party = mPartyMarkerMap.get(marker.getTitle()
                        + marker.getSnippet());
                if (party != null) {
                    if (((PartyMapActivity) getActivity()).isTwoPaneMode()) {
                        // refreshes detail fragment
                        ((PartyMapActivity) getActivity()).onPartySelected(
                                party.getId(), true);
                    }
                    MyApplication.setSelectedPartyId(party.getId());
                } else {
                    MyApplication.setSelectedPartyId(null);
                }
                
                return false; // false = move to map and show info
            }
        });
        // handle user clicks on infowindow (this is for phone screens to start
        // detail activity)
        mMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Party party = mPartyMarkerMap.get(marker.getTitle()
                        + marker.getSnippet());
                if (party != null) {
                    ((PartyMapActivity) getActivity()).onPartySelected(
                            party.getId(), true);
                    MyApplication.setSelectedPartyId(party.getId());
                }
            }
        });
    }

    /**
     * Displays all visible parties on map so we don't set up
     * excess markers and slow it down
     */
    private void showVisibleParties() {
        if (this.mMap == null) {
            // if Google Play Services is not installed, map will be null
            return;
        }
        if (MyApplication.getSelectedPartyId() == null) {
            // select the first party if nothing is selected yet
            if (MyDDPState.getInstance().getParties().size() > 0) {
                MyApplication
                        .setSelectedPartyId(MyDDPState.getInstance().getParties().keySet().iterator().next());
            }
        }
        String partyId = MyApplication.getSelectedPartyId();
        // we have to do this convoluted marker-party hashmap because
        // the Google Maps API doesn't let us tuck an ID into the marker object
        // Note: you can use http://code.google.com/p/android-maps-extensions/
        // but this wasn't included to minimize project references
        if (mPartyMarkerMap == null) {
            mPartyMarkerMap = new HashMap<String, Party>();
        }
        mPartyMarkerMap.clear();
        Marker selectedMarker = null;
        // add all party locations that are visible on the current map
        LatLngBounds visibleBounds = this.mMap.getProjection()
                .getVisibleRegion().latLngBounds;
        for (Party party : MyDDPState.getInstance().getParties().values()) {
            if (visibleBounds.contains(new LatLng(party.getLatitude(),
                    party.getLongitude()))) {
                if (party.getMarker() == null) {
                    // add party if it wasn't visible
                    Marker marker = mMap
                            .addMarker(new MarkerOptions()
                                    .position(
                                            new LatLng(party.getLatitude(),
                                                    party.getLongitude()))
                                    .title(party.getTitle())
                                    .snippet(party.getDescription())
                                    .icon(BitmapDescriptorFactory
                                            .defaultMarker(party
                                                    .getMarkerColor())));
                    party.setMarker(marker);
                }
                if (party.getId() == partyId) {
                    selectedMarker = party.getMarker();
                }
                mPartyMarkerMap.put(party.getMarker().getTitle()
                        + party.getMarker().getSnippet(), party);
            } else {
                // remove any party that is not visible
                party.setMarker(null);
            }
        }
        // show marker info for selected marker
        if (selectedMarker != null) {
            selectedMarker.showInfoWindow();
            // also send fragment notification
            Party party = mPartyMarkerMap.get(selectedMarker.getTitle()
                    + selectedMarker.getSnippet());
            if (party != null) {
                PartyMapActivity activity = ((PartyMapActivity) getActivity());
                // when rotating device, activity can be null for a small fraction of time
                if (activity != null) {
                    activity.onPartySelected(party.getId(), false);
                }
            }
        }
    }

    /**
     * Gets current location using Android's location provider
     * so you can zoom the map to it
     * @return last known Location
     */
    @SuppressWarnings("unused")
    private Location getCurrentLocation() {
        Criteria criteria = new Criteria();
        LocationManager locMan = (LocationManager) getActivity()
                .getSystemService(Context.LOCATION_SERVICE);
        String towers = locMan.getBestProvider(criteria, false);
        Location location = locMan.getLastKnownLocation(towers);
        return location;
    }
}
