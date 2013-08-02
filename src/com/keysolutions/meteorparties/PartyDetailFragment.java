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

import com.keysolutions.ddpclient.android.DDPBroadcastReceiver;
import com.keysolutions.meteorparties.R;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * A fragment representing a single Party detail screen. This fragment is either
 * contained in a {@link PartyMapActivity} in two-pane mode (on tablets) or a
 * {@link PartyDetailActivity} on handsets.
 */
public class PartyDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /** The dummy content this fragment is presenting. */
    private Party mParty;

    /** Make receiver a member variable of a class to be able to unregister it later */
    private BroadcastReceiver mReceiver;
    
    DecelerateInterpolator sDecelerator = new DecelerateInterpolator();
    OvershootInterpolator sOverShooter = new OvershootInterpolator(10f);
    
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PartyDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            String docId = getArguments().getString(ARG_ITEM_ID);
            mParty = MyDDPState.getInstance().getParty(docId);
        }
    }

    /**
     * Initial display of view and fields is done here
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_party_detail,
                container, false);

        // Show the party details.
        if (mParty != null) {
            showPartyInfo(savedInstanceState, rootView);
            updateMyRsvpDisplay(rootView);
        }

        return rootView;
    }

    /**
     * Displays party info
     * @param savedInstanceState instance state
     * @param rootView view the party display elements are in
     */
    public void showPartyInfo(Bundle savedInstanceState, View rootView) {
        ((TextView) rootView.findViewById(R.id.party_title))
                .setText(mParty.getTitle());
        ((TextView) rootView.findViewById(R.id.party_description))
                .setText(mParty.getDescription());
        // get ready to show my RSVP info
        String myUserId = MyDDPState.getInstance().getUserId();
        // add RSVP list
        // NOTE: if you have a lot of RSVPs, you should be using a ListView
        // The TableLayout is used here to make it this demo simple to
        // understand
        if (mParty.getRsvps() != null) {
            TableLayout table = (TableLayout) rootView
                    .findViewById(R.id.rsvp_list);
            table.removeAllViews();
            // add header
            TableRow tr = (TableRow) getLayoutInflater(savedInstanceState)
                    .inflate(R.layout.header_party_rsvp, null);
            table.addView(tr);
            for (Map<String, String> rsvp : mParty.getRsvps()) {
                String rsvpValue = rsvp.get("rsvp");
                String rsvpUser = rsvp.get("user");
                if ((myUserId == null) || (!myUserId.equals(rsvpUser))) {
                    // don't add row for my RSVP because that's displayed later
                    tr = (TableRow) getLayoutInflater(savedInstanceState)
                            .inflate(R.layout.row_party_rsvp, null);
                    TextView tv;
                    tv = (TextView) tr.findViewById(R.id.user);
                    String email = MyDDPState.getInstance()
                            .getUserEmail(rsvpUser);
                    tv.setText(email);
                    tv = (TextView) tr.findViewById(R.id.rsvp);
                    String rsvpDisplayed = "";
                    int rsvpColor = 0;
                    if (rsvpValue.equals("yes")) {
                        rsvpDisplayed = "Going";
                        rsvpColor = 0xFF00FF00;
                    } else if (rsvpValue.equals("no")) {
                        rsvpDisplayed = "Declined";
                        rsvpColor = 0xFFFF0000;
                    } else if (rsvpValue.equals("maybe")) {
                        rsvpDisplayed = "Maybe";
                        rsvpColor = 0xFF0000FF;
                    }
                    tv.setText(rsvpDisplayed);
                    tv.setTextColor(rsvpColor);
                    table.addView(tr);
                }
            }
        }
    }

    /**
     * Looks up current user's RSVP
     * @return null, "yes", "no", or "maybe"
     */
    public String getMyRsvp() {
        String myRsvp = null;
        String myUserId = MyDDPState.getInstance().getUserId();
        for (Map<String, String> rsvp : mParty.getRsvps()) {
            String rsvpValue = rsvp.get("rsvp");
            String rsvpUser = rsvp.get("user");
            if ((myUserId != null) && (myUserId.equals(rsvpUser))) {
                myRsvp = rsvpValue;
            }
        }
        return myRsvp;
    }
    
    /**
     * Updates current RSVP display
     * @param rootView root view holding all the fields
     */
    @SuppressLint("NewApi")
    public void updateMyRsvpDisplay(View rootView) {
        // add info about my RSVP
        boolean isLoggedIn = MyDDPState.getInstance().isLoggedIn();
        ((View)(rootView.findViewById(R.id.login_for_rsvp))).setVisibility(isLoggedIn ? View.INVISIBLE : View.VISIBLE);             
        ((View)(rootView.findViewById(R.id.rsvp_buttons))).setVisibility(isLoggedIn ? View.VISIBLE : View.INVISIBLE);             
        ((View)(rootView.findViewById(R.id.label_my_rsvp))).setVisibility(isLoggedIn ? View.VISIBLE : View.INVISIBLE);
        
        if (isLoggedIn) {
            RadioGroup rsvpButtons = ((RadioGroup) rootView.findViewById(R.id.rsvp_buttons));
            rsvpButtons.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                    for (int j = 0; j < radioGroup.getChildCount(); j++) {
                        final ToggleButton view = (ToggleButton) radioGroup.getChildAt(j);
                        view.setChecked(view.getId() == checkedId);
                    }
                }
            });
            ToggleButton btnYes = ((ToggleButton) rootView.findViewById(R.id.rsvp_yes));
            ToggleButton btnNo = ((ToggleButton) rootView.findViewById(R.id.rsvp_no));
            ToggleButton btnMaybe = ((ToggleButton) rootView.findViewById(R.id.rsvp_maybe));
            OnClickListener toggleListener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onToggle(v);
                }
            };
            btnYes.setOnClickListener(toggleListener);
            btnNo.setOnClickListener(toggleListener);
            btnMaybe.setOnClickListener(toggleListener);
            String myRsvp = getMyRsvp();
            if (myRsvp != null) {
                if (myRsvp.equals("yes")) {
                    rsvpButtons.check(R.id.rsvp_yes);
                } else if (myRsvp.equals("no")) {
                    rsvpButtons.check(R.id.rsvp_no);
                } else if (myRsvp.equals("maybe")) {
                    rsvpButtons.check(R.id.rsvp_maybe);
                }
            }
            
            // if we can, add button animation on Yes because it's cool
            if (android.os.Build.VERSION.SDK_INT > 11) {
                btnYes.animate().setDuration(200);
                btnYes.setOnTouchListener(new OnTouchListener() {                
                    @Override
                    public boolean onTouch(View button, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            ((ToggleButton)button).animate().setInterpolator(sDecelerator).scaleX(.7f).scaleY(.7f);
                        } else {
                            ((ToggleButton)button).animate().setInterpolator(sOverShooter).scaleX(1f).scaleY(1f);
                        }
                        return false;
                    }
                });
            }
        }
    }

    /**
     * Used to make a group of ToggleButtons act like radiobuttons
     * @param view
     */
    public void onToggle(View view) {
        int id = view.getId();
        ((RadioGroup)view.getParent()).check(0);    // to make it behave not like a toggle
        ((RadioGroup)view.getParent()).check(id);
        MyDDPState ddp = MyDDPState.getInstance();
        switch (id) {
        case R.id.rsvp_yes:
            ddp.rsvp(mParty.getId(), "yes");
            break;
        case R.id.rsvp_no:
            ddp.rsvp(mParty.getId(), "no");
            break;
        case R.id.rsvp_maybe:
            ddp.rsvp(mParty.getId(), "maybe");
            break;
        }
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
    }

    @Override
    public void onResume() {
        super.onResume();

        // get ready to handle DDP events
        mReceiver = new DDPBroadcastReceiver(MyDDPState.getInstance(), getActivity()) {
            @Override
            protected void onSubscriptionUpdate(String changeType,
                    String subscriptionName, String docId) {
                if ((mParty != null) && (mParty.getId().equals(docId))) {
                    // redisplay party
                    showPartyInfo(null, getView());
                }
            }
            @Override
            protected void onLogin() {
                showPartyInfo(null, getView());
                updateMyRsvpDisplay(getView());
            }
            @Override
            protected void onLogout() {
                showPartyInfo(null, getView());
                updateMyRsvpDisplay(getView());
            }
        };
    }
    
}
