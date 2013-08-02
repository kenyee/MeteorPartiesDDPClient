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

import com.keysolutions.meteorparties.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

/**
 * Handles the main Party map activity.
 * This is mostly a wrapper around {@link PartyMapFragment} with a
 * bet of special handling for two-pane mode
 * @author kenyee
 */
public class PartyMapActivity extends FragmentActivity implements PartyMapFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    
    /** handle to Login menu item */
    private MenuItem mMenuLogin;
    
    /** handle to Logout menu item */
    private MenuItem mMenuLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_map);
        //NOTE: activity_party_map layout is aliased in res/values-large/refs.xml
        // and res/values-sw600dp/refs.xml to switch to activity_party_twopane.xml
        
        // workaround for black map...see http://code.google.com/p/gmaps-api-issues/issues/detail?id=4639
        ViewGroup topLayout = (ViewGroup) findViewById(R.id.party_map);
        topLayout.requestTransparentRegion(topLayout);

        if (findViewById(R.id.party_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    /**
     * Callback method from {@link PartyMapFragment.Callbacks} indicating that
     * the item with the given ID was selected.
    */    
    @Override
    public void onPartySelected(String id, boolean clickThrough) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(PartyDetailFragment.ARG_ITEM_ID, id);
            PartyDetailFragment fragment = new PartyDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.party_detail_container, fragment).commit();

        } else if (clickThrough) {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            // Only if use clicked on it, not if it's displayed as last selected
            Intent detailIntent = new Intent(this, PartyDetailActivity.class);
            detailIntent.putExtra(PartyDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }

    /** for fragment to find out if activity is in two-pane mode */
    @Override
    public boolean isTwoPaneMode() {
        return mTwoPane;
    }

    /** used by fragment to force update of login buttons */
    @Override
    public void updateLoginButtons() {
        if (mMenuLogin != null) {
            MyDDPState ddp = MyDDPState.getInstance();
            mMenuLogin.setVisible(!ddp.isLoggedIn());
            mMenuLogout.setVisible(ddp.isLoggedIn());
        }
    }
    
    /**
     * Updates login action bar menu buttons
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.menu_main, menu);
      mMenuLogin = menu.findItem(R.id.action_login);
      mMenuLogout = menu.findItem(R.id.action_logout);
      updateLoginButtons();
      return true;
    }
    
    /**
     * Handles login/logout button presses
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      MyDDPState ddp = MyDDPState.getInstance();
      switch (item.getItemId()) {
      case R.id.action_login:
          String resumeToken = ddp.getResumeToken();
          if (resumeToken != null) {
              // start DDP login process
              ddp.login(resumeToken);
          } else {
              // fire up login window
              Intent intent = new Intent(this, LoginActivity.class);
              this.startActivity(intent);
          }
        break;
      case R.id.action_logout:
        ddp.logout();
        break;
      default:
        break;
      }
      return true;
    }
}
