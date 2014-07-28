/*
 * Copyright (c) 2014 SBG Apps
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sbgapps.scoreit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.melnykov.fab.FloatingActionButton;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.sbgapps.scoreit.games.Game;
import com.sbgapps.scoreit.games.GameHelper;
import com.sbgapps.scoreit.games.Lap;
import com.sbgapps.scoreit.games.LapFragment;
import com.sbgapps.scoreit.games.Player;
import com.sbgapps.scoreit.games.belote.BeloteLap;
import com.sbgapps.scoreit.games.belote.BeloteLapFragment;
import com.sbgapps.scoreit.games.coinche.CoincheLap;
import com.sbgapps.scoreit.games.coinche.CoincheLapFragment;
import com.sbgapps.scoreit.games.tarot.TarotFiveLap;
import com.sbgapps.scoreit.games.tarot.TarotFourLap;
import com.sbgapps.scoreit.games.tarot.TarotLapFragment;
import com.sbgapps.scoreit.games.tarot.TarotThreeLap;
import com.sbgapps.scoreit.games.universal.UniversalLap;
import com.sbgapps.scoreit.games.universal.UniversalLapFragment;
import com.sbgapps.scoreit.navigationdrawer.NavigationDrawerItem;
import com.sbgapps.scoreit.navigationdrawer.NavigationDrawerView;
import com.sbgapps.scoreit.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;

public class ScoreItActivity extends ActionBarActivity
        implements FragmentManager.OnBackStackChangedListener {

    private static final int REQ_PICK_CONTACT = 1;

    @InjectView(R.id.navigation_drawer)
    NavigationDrawerView mNavigationDrawer;
    @InjectView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @InjectView(R.id.drawer_list_view)
    ListView mDrawerListView;
    @InjectView(R.id.fab)
    FloatingActionButton mActionButton;

    private List<NavigationDrawerItem> mNavigationItems;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle;
    private int mSelectedPosition = 0;
    private boolean mIsTablet;
    private ScoreFragment mScoreFragment;
    private LapFragment mLapFragment;
    private GameHelper mGameHelper;
    private Player mEditedPlayer;
    private Lap mLap;
    private boolean mIsEdited = false;

    public GameHelper getGameHelper() {
        return mGameHelper;
    }

    public Lap getLap() {
        return mLap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGameHelper = new GameHelper(this);
        mGameHelper.loadLaps();

        setContentView(R.layout.activity_scoreit);
        ButterKnife.inject(this);
        //mIsTablet = (null != findViewById(R.id.fragment_container_large));

        final FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        // Init fragments
        if (null == savedInstanceState) {
            mScoreFragment = new ScoreFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.main_container, mScoreFragment, ScoreFragment.TAG)
                    .commit();
        } else {
            mIsEdited = savedInstanceState.getBoolean("edited");
            mScoreFragment = (ScoreFragment) fragmentManager.findFragmentByTag(ScoreFragment.TAG);
            mLapFragment = (LapFragment) fragmentManager.findFragmentByTag(LapFragment.TAG);
            if (mIsEdited) {
                int position = savedInstanceState.getInt("lap");
                mLap = mGameHelper.getLaps().get(position);
                mActionButton.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_content_edit_fab));
            }
        }

        // Init drawer
        mTitle = getTitle();
        mNavigationItems = new ArrayList<>();
        mNavigationItems.add(new NavigationDrawerItem(getString(R.string.universal), true));
        mNavigationItems.add(new NavigationDrawerItem(getString(R.string.tarot), true));
        mNavigationItems.add(new NavigationDrawerItem(getString(R.string.belote), true));
        mNavigationItems.add(new NavigationDrawerItem(getString(R.string.coinche), true));
        mNavigationItems.add(new NavigationDrawerItem(getString(R.string.about),
                R.drawable.ic_action_about, false));
        mNavigationDrawer.replaceWith(mNavigationItems);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_navigation_drawer, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar()
                        .setTitle(mNavigationItems.get(mSelectedPosition).getItemName());
                supportInvalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(getTitle());
                supportInvalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mSelectedPosition = mGameHelper.getPlayedGame();
        selectItem(mSelectedPosition);

        // Floating Action Button
        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null == mLap) {
                    mActionButton.setImageDrawable(
                            getResources().getDrawable(R.drawable.ic_action_accept_fab));
                    addLap();
                } else {
                    mActionButton.setImageDrawable(
                            getResources().getDrawable(R.drawable.ic_action_new_fab));
                    mLap.computeScores();
                    if (mIsEdited) {
                        mIsEdited = false;
                    } else {
                        mGameHelper.addLap(mLap);
                    }
                    getSupportFragmentManager()
                            .popBackStack(LapFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    mScoreFragment.update();
                    mLap = null;
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGameHelper.saveGame();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mLap) {
            outState.putInt("lap", mGameHelper.getLaps().indexOf(mLap));
            outState.putBoolean("edited", mIsEdited);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mDrawerLayout.isDrawerOpen(mNavigationDrawer) ||
                (null != mScoreFragment && !mScoreFragment.isVisible())) {
            return false;
        }
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mDrawerLayout.isDrawerOpen(mNavigationDrawer)) {
            return false;
        }
        MenuItem item;
        if (0 == mGameHelper.getLaps().size()) {
            if (!mIsTablet) {
                item = menu.findItem(R.id.menu_view);
                item.setVisible(false);
            }
            item = menu.findItem(R.id.menu_clear);
            item.setVisible(false);
        }
        item = menu.findItem(R.id.menu_count);
        item.setVisible(Game.UNIVERSAL == mGameHelper.getPlayedGame() ||
                Game.TAROT == mGameHelper.getPlayedGame());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.menu_clear:
                showClearDialog();
                return true;

            case R.id.menu_view:
                mScoreFragment.switchScoreViews();
                return true;

            case R.id.menu_count:
                showPlayerCountDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnItemClick(R.id.drawer_list_view)
    public void onItemClick(int position, long id) {
        if (mDrawerLayout.isDrawerOpen(mNavigationDrawer)) {
            mDrawerLayout.closeDrawer(mNavigationDrawer);
            if (mSelectedPosition == position) {
                return;
            }
            onNavigationDrawerItemSelected(position);
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);

            mNavigationItems.get(mSelectedPosition).setSelected(false);
            mNavigationItems.get(position).setSelected(true);

            mSelectedPosition = position;
            getSupportActionBar()
                    .setTitle(mNavigationItems.get(mSelectedPosition).getItemName());
        }
        mDrawerLayout.closeDrawer(mNavigationDrawer);
    }

    private void onNavigationDrawerItemSelected(int position) {
        switch (position) {
            case 0:
                mGameHelper.setPlayedGame(Game.UNIVERSAL);
                break;
            case 1:
                mGameHelper.setPlayedGame(Game.TAROT);
                break;
            case 2:
                mGameHelper.setPlayedGame(Game.BELOTE);
                break;
            case 3:
                mGameHelper.setPlayedGame(Game.COINCHE);
                break;
            case 4:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return;
        }
        getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        supportInvalidateOptionsMenu();
        mScoreFragment.loadFragments();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String name;
        if (RESULT_OK != resultCode) return;

        switch (requestCode) {
            case REQ_PICK_CONTACT:
                Cursor cursor = getContentResolver().query(data.getData(),
                        new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    name = cursor.getString(columnIndex);
                    mEditedPlayer.setName(name);
                    mScoreFragment.getHeaderFragment().update();
                }
                break;
        }
    }

    public void addLap() {
        switch (mGameHelper.getPlayedGame()) {
            default:
            case Game.UNIVERSAL:
                mLap = new UniversalLap(mGameHelper.getPlayerCount());
                break;
            case Game.BELOTE:
                mLap = new BeloteLap();
                break;
            case Game.COINCHE:
                mLap = new CoincheLap();
                break;
            case Game.TAROT:
                switch (mGameHelper.getPlayerCount()) {
                    case 3:
                        mLap = new TarotThreeLap();
                        break;
                    case 4:
                        mLap = new TarotFourLap();
                        break;
                    case 5:
                        mLap = new TarotFiveLap();
                        break;
                }
                break;
        }
        showLapFragment();
    }

    public void editLap(Lap lap) {
        mIsEdited = true;
        mLap = lap;
        mScoreFragment.closeOpenedItems();
        mActionButton.setImageDrawable(
                getResources().getDrawable(R.drawable.ic_content_edit_fab));
        showLapFragment();
    }

    public void removeLap(Lap lap) {
        mGameHelper.removeLap(lap);
        mScoreFragment.closeOpenedItems();
        mScoreFragment.update();
    }

    public void editName(Player player) {
        mEditedPlayer = player;
        showEditNameActionChoices();
    }

    public void editColor(Player player) {
        mEditedPlayer = player;
        showColorPickerDialog();
    }

    private void showLapFragment() {
        switch (mGameHelper.getPlayedGame()) {
            default:
            case Game.UNIVERSAL:
                mLapFragment = new UniversalLapFragment();
                break;
            case Game.BELOTE:
                mLapFragment = new BeloteLapFragment();
                break;
            case Game.COINCHE:
                mLapFragment = new CoincheLapFragment();
                break;
            case Game.TAROT:
                mLapFragment = new TarotLapFragment();
                break;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.scale_in,
                        android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left,
                        R.anim.scale_out)
                .replace(R.id.main_container, mLapFragment)
                .addToBackStack(LapFragment.TAG)
                .commit();
    }

    private void setAccentDecor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window win = getWindow();
            WindowManager.LayoutParams winParams = win.getAttributes();
            final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            winParams.flags |= bits;
            win.setAttributes(winParams);
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintResource(R.drawable.background_status_bar);
        }
    }

    private void showClearDialog() {
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.clear_game)
                .setMessage(R.string.sure)
                .setPositiveButton(
                        R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mGameHelper.deleteAll();
                                mScoreFragment.getHeaderFragment().update();
                                ScoreListFragment list = mScoreFragment.getScoreListFragment();
                                if (null != list && list.isVisible())
                                    list.getListAdapter().removeAll();
                                ScoreGraphFragment graph = mScoreFragment.getScoreGraphFragment();
                                if (null != graph && graph.isVisible()) {
                                    graph.update();
                                    if (!mIsTablet) getSupportFragmentManager().popBackStack();
                                }
                                supportInvalidateOptionsMenu();
                            }
                        }
                )
                .setNegativeButton(
                        R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Nothing to do!
                            }
                        }
                )
                .create();
        dialog.show();
        Utils.colorizeDialog(dialog);
    }

    private void showEditNameActionChoices() {
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_name)
                .setItems(R.array.edit_name_action,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent;
                                switch (which) {
                                    default:
                                    case 0:
                                        intent = new Intent(Intent.ACTION_PICK,
                                                ContactsContract.Contacts.CONTENT_URI);
                                        startActivityForResult(intent, REQ_PICK_CONTACT);
                                        break;
                                    case 1:
                                        showEditNameDialog();
                                        break;
                                }
                            }
                        }
                )
                .create();
        dialog.show();
        Utils.colorizeDialog(dialog);
    }

    public void showPlayerCountDialog() {
        String[] players;
        switch (mGameHelper.getPlayedGame()) {
            default:
                players = new String[]{"2", "3", "4", "5"};
                break;
            case Game.TAROT:
                players = new String[]{"3", "4", "5"};
                break;
        }
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.player_number)
                .setItems(players,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mGameHelper.setPlayerCount(which);
                                mScoreFragment.loadFragments();
                            }
                        }
                )
                .create();
        dialog.show();
        Utils.colorizeDialog(dialog);
    }

    private void showEditNameDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_name, null);
        final EditText editText = (EditText) view.findViewById(R.id.edit_name);
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_name)
                .setView(view)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String name = editText.getText().toString();
                                mEditedPlayer.setName(name);
                                mScoreFragment.getHeaderFragment().update();
                            }
                        }
                )
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Nothing to do!
                            }
                        }
                )
                .create();
        dialog.show();
        Utils.colorizeDialog(dialog);
    }

    private void showColorPickerDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_color_picker, null);
        final ColorPicker picker = (ColorPicker) view.findViewById(R.id.picker);
        picker.setColor(mEditedPlayer.getColor());
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_color)
                .setView(view)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int color = picker.getColor();
                                mEditedPlayer.setColor(color);
                                mScoreFragment.update();
                            }
                        }
                )
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Nothing to do!
                            }
                        }
                )
                .create();
        dialog.show();
        Utils.colorizeDialog(dialog);
    }

    @Override
    public void onBackStackChanged() {
        if (null != mLapFragment && mLapFragment.isVisible()) {
            return;
        }
        mLap = null;
        mIsEdited = false;
        mActionButton.setImageDrawable(
                getResources().getDrawable(R.drawable.ic_action_new_fab));
        supportInvalidateOptionsMenu();
    }
}
