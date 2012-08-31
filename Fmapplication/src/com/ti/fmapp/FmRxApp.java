/*
 * TI's FM
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 * Copyright (C) 2010 Sony Ericsson Mobile Communications AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*******************************************************************************\
 *
 *   FILE NAME:      FmRxApp.java
 *
 *   BRIEF:          This file defines the API of the FM Rx stack.
 *
 *   DESCRIPTION:    General
 *
 *
 *
 *   AUTHOR:
 *
 \*******************************************************************************/
package com.ti.fmapp;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.ti.fm.FmReceiver;
import com.ti.fm.FmReceiverIntent;
import com.ti.fm.IFmConstants;
import com.ti.fmapp.adapters.PreSetsAdapter;
import com.ti.fmapp.database.PreSetsDB;
import com.ti.fmapp.logic.PreSetRadio;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

/*
 FM Boot up Sequence:

 FM APIS NON Blocking:

 sFmReceiver.rxEnable() is called from FM application, when the user selects FM radio icon from main menu.

 Once  the callback for sFmReceiver.rxEnable() is received (EVENT_FM_ENABLED),
 the default configurations which have been saved in the preference will be loaded using loadDefaultConfiguration() function .
 After this  sFmReceiver.rxSetVolume() with default volume will be called.

 Once the callback for  sFmReceiver.rxSetVolume() is received(EVENT_VOLUME_CHANGE),
 sFmReceiver.rxSetBand() with default band will be called.

 Once the callback for sFmReceiver.rxSetBand() is received(EVENT_BAND_CHANGE), sFmReceiver.rxTune_nb()
 with default frequency will be called.

 Once the callback for sFmReceiver.rxTune_nb()is received (EVENT_TUNE_COMPLETE)
 sFmReceiver.rxEnableAudioRouting()  will be called to enable audio.

 After these sequences user can hear the FM audio.


 The rest of the APIS will be called based on the user actions like when user presses seek up or down
 sFmReceiver.rxSeek_nb() will be called and the callback for the same will be EVENT_SEEK_STARTED.

 To increase decrease the volume  sFmReceiver.rxSetVolume() will be called and the callback for
 the same will be EVENT_VOLUME_CHANGE.

 To mute /unmute, sFmReceiver.rxSetMuteMode() will be called and the callback
 for the same will be EVENT_MUTE_CHANGE.


  FM APIS  Blocking:

In case of blocking FM APIS, the above sequence holds good. The difference will be the FM Events will not posted
as intents and the usage of FM APIS will be sequential.
 */

public class FmRxApp extends Activity implements View.OnClickListener,
        IFmConstants, FmRxAppConstants, FmReceiver.ServiceListener,
        ViewSwitcher.ViewFactory, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    public static final String TAG = "FmRxApp";
    private static final boolean DBG = false;

    private ArrayList<PreSetRadio> preSetRadios = null;

    private static final boolean MAKE_FM_APIS_BLOCKING = true;

    // Notification stuff
    private NotificationManager mNotificationManager;
    private Notification mNotification;

    private boolean isFirstPlay = true;
    private boolean hidNotification = false;

    /**
     * *****************************************
     * Widgets
     * ******************************************
     */

    private ImageView imgFmMode, imgFmVolume;
    private TextView txtStatusMsg, txtRadioText;
    private TextView txtPsText;
    private ProgressDialog pd = null, configPd;

    /**
     * *****************************************
     * Menu Constants
     * ******************************************
     */
    public static final int MENU_CONFIGURE = Menu.FIRST + 1;
    public static final int MENU_EXIT = Menu.FIRST + 2;
    public static final int MENU_ABOUT = Menu.FIRST + 3;

    /**
     * *****************************************
     * private variables
     * ******************************************
     */
    private int mToggleMode = 0; // To toggle between the mono/stereo
    private int mToggleAudio = 1; // To toggle between the speaker/headset
    private boolean mToggleMute = false; // To toggle between the mute/unmute

    private boolean mRdsState = false;
    /* Default values */
    private int mVolume = DEF_VOLUME;
    private int mMode = DEFAULT_MODE;
    private boolean mRds = DEFAULT_RDS;
    private String mPS = "";
    private boolean mRdsAf = DEFAULT_RDS_AF;
    private int mRdsSystem = INITIAL_VAL;
    private int mDeEmpFilter = INITIAL_VAL;
    private int mRssi = INITIAL_RSSI;
    // Seek up/down direction
    private int mDirection = FM_SEEK_UP;

    /* State values */

    // variable to make sure that the next configuration change happens after
    // the current configuration request has been completed.
    private int configurationState = CONFIGURATION_STATE_IDLE;

    // variable to make sure that the next volume change happens after the
    // current volume request has been completed.

    private boolean mVolState = VOL_REQ_STATE_IDLE;
    // variable to make sure that the next seek happens after the current seek
    // request has been completed.
    private boolean mSeekState = SEEK_REQ_STATE_IDLE;

    private boolean mStatus;
    private int mIndex = 0;
    private int mStationIndex;

    //to use with frequency display on main screen
    private static final int[] NUMBER_IMAGES = new int[]{
            R.drawable.fm_number_0, R.drawable.fm_number_1, R.drawable.fm_number_2,
            R.drawable.fm_number_3, R.drawable.fm_number_4, R.drawable.fm_number_5,
            R.drawable.fm_number_6, R.drawable.fm_number_7, R.drawable.fm_number_8,
            R.drawable.fm_number_9
    };

    private ImageSwitcher[] mFreqDigits;

    /*
     * Variable to identify whether we need to do the default setting when
     * entering the FM application. Based on this variable,the default
     * configurations for the FM will be done for the first time
     */

    private static boolean sdefaultSettingOn = false;

    static final String FM_INTERRUPTED_KEY = "fm_interrupted";
    static final String FM_STATE_KEY = "fm_state";
    /* Flag to know whether FM App was interrupted due to orientation change */
    boolean mFmInterrupted = false;

    /*Flag to check if service is connected*/
    boolean mFmServiceConnected = false;

    /**
     * *****************************************
     * public variables
     * ******************************************
     */
    public static int sBand = DEFAULT_BAND;
    public static int sChannelSpace = DEFAULT_CHANNELSPACE;

    public static Float lastTunedFrequency = (float) DEFAULT_FREQ_EUROPE;
    public static FmReceiver sFmReceiver;

    private OrientationListener mOrientationListener;

    Context mContext;

    /**
     * Called when the activity is first created.
     */

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mContext = this;
        /* Retrieve the fm_state and find out whether FM App was interrupted */

        if (savedInstanceState != null) {
            Bundle fmState = savedInstanceState.getBundle(FM_STATE_KEY);
            if (fmState != null) {
                mFmInterrupted = fmState.getBoolean(FM_INTERRUPTED_KEY, false);

            }
        }


        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Register for FM intent broadcasts.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FmReceiverIntent.FM_ENABLED_ACTION);
        intentFilter.addAction(FmReceiverIntent.FM_DISABLED_ACTION);
        intentFilter.addAction(FmReceiverIntent.GET_FREQUENCY_ACTION);
        intentFilter.addAction(FmReceiverIntent.SEEK_ACTION);
        intentFilter.addAction(FmReceiverIntent.BAND_CHANGE_ACTION);
        intentFilter.addAction(FmReceiverIntent.GET_CHANNEL_SPACE_ACTION);
        intentFilter.addAction(FmReceiverIntent.SET_MODE_MONO_STEREO_ACTION);
        intentFilter.addAction(FmReceiverIntent.VOLUME_CHANGED_ACTION);
        intentFilter.addAction(FmReceiverIntent.RDS_TEXT_CHANGED_ACTION);
        intentFilter.addAction(FmReceiverIntent.PS_CHANGED_ACTION);
        intentFilter.addAction(FmReceiverIntent.AUDIO_PATH_CHANGED_ACTION);
        intentFilter.addAction(FmReceiverIntent.TUNE_COMPLETE_ACTION);
        intentFilter.addAction(FmReceiverIntent.SEEK_STOP_ACTION);
        intentFilter.addAction(FmReceiverIntent.MUTE_CHANGE_ACTION);
        intentFilter.addAction(FmReceiverIntent.DISPLAY_MODE_MONO_STEREO_ACTION);
        intentFilter.addAction(FmReceiverIntent.ENABLE_RDS_ACTION);
        intentFilter.addAction(FmReceiverIntent.DISABLE_RDS_ACTION);
        intentFilter.addAction(FmReceiverIntent.SET_RDS_AF_ACTION);
        intentFilter.addAction(FmReceiverIntent.SET_RDS_SYSTEM_ACTION);
        intentFilter.addAction(FmReceiverIntent.SET_DEEMP_FILTER_ACTION);
        intentFilter.addAction(FmReceiverIntent.SET_RSSI_THRESHHOLD_ACTION);
        intentFilter.addAction(FmReceiverIntent.SET_RF_DEPENDENT_MUTE_ACTION);
        intentFilter.addAction(FmReceiverIntent.PI_CODE_CHANGED_ACTION);
        intentFilter.addAction(FmReceiverIntent.MASTER_VOLUME_CHANGED_ACTION);
        intentFilter.addAction(FmReceiverIntent.CHANNEL_SPACING_CHANGED_ACTION);
        intentFilter.addAction(FmReceiverIntent.COMPLETE_SCAN_DONE_ACTION);
        intentFilter.addAction(FmReceiverIntent.COMPLETE_SCAN_STOP_ACTION);
        intentFilter.addAction(FmReceiverIntent.GET_BAND_ACTION);
        intentFilter.addAction(FmReceiverIntent.GET_MONO_STEREO_MODE_ACTION);
        intentFilter.addAction(FmReceiverIntent.GET_MUTE_MODE_ACTION);
        intentFilter.addAction(FmReceiverIntent.GET_RF_MUTE_MODE_ACTION);
        intentFilter.addAction(FmReceiverIntent.GET_RSSI_THRESHHOLD_ACTION);
        intentFilter.addAction(FmReceiverIntent.GET_DEEMPHASIS_FILTER_ACTION);
        intentFilter.addAction(FmReceiverIntent.GET_VOLUME_ACTION);
        intentFilter.addAction(FmReceiverIntent.GET_RDS_SYSTEM_ACTION);
        intentFilter.addAction(FmReceiverIntent.GET_RDS_GROUPMASK_ACTION);
        intentFilter.addAction(FmReceiverIntent.GET_RDS_AF_SWITCH_MODE_ACTION);
        intentFilter.addAction(FmReceiverIntent.GET_RSSI_ACTION);
        intentFilter.addAction(FmReceiverIntent.COMPLETE_SCAN_PROGRESS_ACTION);
        intentFilter.addAction(FmReceiverIntent.SET_RDS_AF_ACTION);
        intentFilter.addAction(FmReceiverIntent.SET_RF_DEPENDENT_MUTE_ACTION);
        intentFilter.addAction(FmReceiverIntent.SET_CHANNEL_SPACE_ACTION);
        // intentFilter.addAction(FmReceiverIntent.FM_ERROR_ACTION);

        registerReceiver(mReceiver, intentFilter);

        /*
         * Need to enable the FM if it was not enabled earlier
         */

        sFmReceiver = new FmReceiver(this, this);
    }


    /**
     * @param command what command to pass
     * @return the pending intent that contains the provided command to deliver to the appropriate service
     */
    private PendingIntent buildServiceIntent(String command) {
        Intent intent = new Intent(this, FmRxApp.class);
        intent.putExtra(EXTRA_COMMAND, command);

        //TODO: Now we use this Activity, but in the future we should really use a service for dealing with all FM comms
        return PendingIntent.getActivity(getApplicationContext(),
                command.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Initialize ImageSwitcher for the top frequency numbers
     */
    private void initImageSwitcher() {
        mFreqDigits = new ImageSwitcher[5];
        mFreqDigits[0] = (ImageSwitcher) findViewById(R.id.is_1);
        mFreqDigits[1] = (ImageSwitcher) findViewById(R.id.is_2);
        mFreqDigits[2] = (ImageSwitcher) findViewById(R.id.is_3);
        mFreqDigits[3] = (ImageSwitcher) findViewById(R.id.is_4);
        mFreqDigits[4] = (ImageSwitcher) findViewById(R.id.is_5);
        for (ImageSwitcher switcher : mFreqDigits) {
            switcher.setFactory(FmRxApp.this);
        }

        //set last tunned frequency
        updateFrequencyDisplay(lastTunedFrequency);
    }


    private void startup() {

        switch (sFmReceiver.getFMState()) {

            /* FM is already enabled. Just update the UI. */
            case FmReceiver.STATE_ENABLED:
                // Fm app is on and we are entering from the other screens
                loadDefaultConfiguration();
                setContentView(R.layout.fmrxmain);
                initControls();

                // Clear the notification which was displayed.
                // this.mNotificationManager.cancel(FM_NOTIFICATION_ID);
                break;

            case FmReceiver.STATE_DISABLED:
                Log.i(TAG, "FmReceiver.STATE_DISABLED ");
                //TODO: should this be set to false?
                sdefaultSettingOn = false;

                mStatus = sFmReceiver.enable();
                if (!mStatus) {
                    showAlert(this, "FmReceiver", getString(R.string.cannot_enable_radio));

                } else { /* Display the dialog till FM is enabled */
                    pd = ProgressDialog.show(this, getString(R.string.please_wait),
                            getString(R.string.powering_radio), true, false);
                }

                break;

            /* FM has not been started. Start the FM */
            case FmReceiver.STATE_DEFAULT:
                sdefaultSettingOn = false;
                /*
                * Make sure not to start the FM_Enable() again, if it has been
                * already called before orientation change
                */
                if (!mFmInterrupted) {
                    mStatus = sFmReceiver.create();
                    if (!mStatus) {
                        showAlert(this, "FmRadio", getString(R.string.cannot_enable_radio));

                    }
                    mStatus = sFmReceiver.enable();
                    if (!mStatus) {
                        showAlert(this, "FmRadio", getString(R.string.cannot_enable_radio));

                    } else { /* Display the dialog till FM is enabled */
                        pd = ProgressDialog.show(this, getString(R.string.please_wait),
                                getString(R.string.powering_radio), true, false);
                    }
                } else {
                    Log.i(TAG, "mFmInterrupted is true dont call enable");

                }
                break;
        }
    }

    public void onServiceConnected() {
        Log.i(TAG, "onServiceConnected");
        mFmServiceConnected = true;
        startup();
    }

    public void onServiceDisconnected() {
        Log.d(TAG, "Lost connection to service");
        mFmServiceConnected = false;
        sFmReceiver = null;
    }

    /*
     * Handler for all the FM related events. The events will be handled only
     * when we are in the FM application main menu
     */

    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {

            switch (msg.what) {

                /*
                * After FM is enabled dismiss the progress dialog and display the
                * FM main screen. Set the default volume.
                */
                case EVENT_FM_ENABLED:

                    Log.i(TAG, "enter handleMessage ----EVENT_FM_ENABLED");
                    if (pd != null) {
                        pd.dismiss();
                    }

                    // load RDS configs
                    setRdsConfig();

                    loadDefaultConfiguration();
                    setContentView(R.layout.fmrxmain);
                    // At Power up, FM should be always unmuted
                    mToggleMute = false;
                    initControls();
                    break;

                /*
                * Update the icon on main screen with appropriate mono/stereo
                * icon
                */
                case EVENT_MONO_STEREO_CHANGE:

                    Log.i(TAG, "enter handleMessage ---EVENT_MONO_STEREO_CHANGE");
                    if (mMode == 0) {
                        imgFmMode.setImageResource(R.drawable.fm_stereo);
                    } else {
                        imgFmMode.setImageResource(R.drawable.fm_mono);
                    }
                    break;

                /*
                * Update the icon on main screen with appropriate mono/stereo
                * icon
                */
                case EVENT_MONO_STEREO_DISPLAY:

                    Log.i(TAG, "enter handleMessage ---EVENT_MONO_STEREO_DISPLAY");

                    Integer mode = (Integer) msg.obj;
                    //Log.i(TAG, "enter handleMessage ---mode" + mode.intValue());
                    if (mode == 0) {
                        imgFmMode.setImageResource(R.drawable.fm_stereo);
                    } else {
                        imgFmMode.setImageResource(R.drawable.fm_mono);
                    }
                    break;

                /*
                * Update the icon on main screen with appropriate mute/unmute icon
                */
                case EVENT_MUTE_CHANGE:
                    Log.i(TAG, "enter handleMessage ---EVENT_MUTE_CHANGE  ");

                    break;

                case EVENT_SEEK_STOPPED:
                    Integer seekFreq = (Integer) msg.obj;
                    Log.d(TAG, "enter handleMessage ----EVENT_SEEK_STOPPED seekFreq: " + seekFreq);
                    lastTunedFrequency = (float) seekFreq / 1000;
                    txtStatusMsg.setText(R.string.playing);

                    //update panel frequency display
                    updateFrequencyDisplay(lastTunedFrequency);
                    break;

                case EVENT_FM_DISABLED:

                    Log.i(TAG, "enter handleMessage ----EVENT_FM_DISABLED");

                    /*
                    * we have exited the FM App. Set the sdefaultSettingOn flag to
                    * false Save the default configuration in the preference
                    */
                    sdefaultSettingOn = false;
                    saveDefaultConfiguration();
                    finish();
                    break;

                case EVENT_SEEK_STARTED:
                    Integer freq = (Integer) msg.obj;
                    Log.i(TAG, "enter handleMessage ----EVENT_SEEK_STARTED freq" + freq);
                    lastTunedFrequency = (float) freq / 1000;
                    txtStatusMsg.setText(R.string.playing);
                    updateFrequencyDisplay(lastTunedFrequency);
                    if (isFirstPlay) {
                        isFirstPlay = false;
                        initNotifications();
                    }
                    //update notification display
                    if (!hidNotification) {
                        updateNotification(lastTunedFrequency, "");
                    }

                    // clear the RDS text
                    txtRadioText.setText(null);

                    // clear the PS text
                    txtPsText.setText(null);

                    /*
                    * Seek up/down will be completed here. So set the state to
                    * idle, so that user can seek to other frequency.
                    */

                    mSeekState = SEEK_REQ_STATE_IDLE;

                    break;

                /*
                * Set the default band , if the fm app is getting started first
                * time
                */
                case EVENT_VOLUME_CHANGE:
                    Log.i(TAG, "enter handleMessage ----EVENT_VOLUME_CHANGE");

                    /*
                    * volume change will be completed here. So set the state to
                    * idle, so that user can set other volume.
                    */
                    mVolState = VOL_REQ_STATE_IDLE;
                    /*
                    * Setting the default band after the volume change when FM app
                    * is started for the first time
                    */
                    if (!sdefaultSettingOn) {
                        /* Set the default band */
                        if (MAKE_FM_APIS_BLOCKING) {
                            // Code for blocking call
                            mStatus = sFmReceiver.setBand(sBand);
                            if (!mStatus) {
                                showAlert(getParent(), "FmReceiver", getString(R.string.not_able_to_setband));
                            } else {
                                mStatus = sFmReceiver.tune((int) (lastTunedFrequency * 1000));
                                if (!mStatus) {
                                    showAlert(getParent(), "FmReceiver", getString(R.string.not_able_to_tune));
                                }
                            }

                        } else {
                            // Code for non blocking call
                            //  mStatus = sFmReceiver.rxSetBand_nb(sBand);
                            if (!mStatus) {
                                showAlert(getParent(), "FmReceiver", getString(R.string.not_able_to_setband));
                            }
                        }


                    }
                    break;

                case EVENT_COMPLETE_SCAN_PROGRESS:
                    Log.i(TAG, "enter handleMessage ----EVENT_COMPLETE_SCAN_PROGRESS");

                    Integer progress = (Integer) msg.obj;
                    Log.i(TAG, "enter handleMessage ----EVENT_COMPLETE_SCAN_PROGRESS progress" + progress);
                    break;

                /*
                * enable audio routing , if the fm app is getting started first
                * time
                */
                case EVENT_TUNE_COMPLETE:
                    Integer tuneFreq = (Integer) msg.obj;
                    Log.i(TAG, "enter handleMessage ----EVENT_TUNE_COMPLETE tuneFreq"
                            + tuneFreq);
                    lastTunedFrequency = (float) tuneFreq / 1000;
                    txtStatusMsg.setText(R.string.playing);
                    updateFrequencyDisplay(lastTunedFrequency);
                    // clear the RDS text
                    txtRadioText.setText(null);
                    // clear the PS text
                    txtPsText.setText(null);

                    Log.d(TAG, "sdefaultSettingOn: " + sdefaultSettingOn);

                    /*
                    * Enable the Audio routing after the tune complete , when FM
                    * app is started for the first time or after reentering the Fm
                    * app
                    */
                    if (!sdefaultSettingOn) {
/*
                mStatus = sFmReceiver.rxEnableAudioRouting();
                    if (mStatus == false) {
                        showAlert(getParent(), "FmReceiver",
                                "Not able to enable audio!!!!");
                    }
*/
                        /*
                        * The default setting for the FMApp are completed here. If
                        * the user selects back and goes out of the FM App, when he
                        * reenters we dont have to do the default configurations
                        */
                        sdefaultSettingOn = true;
                    }

                    // clear the RDS text
                    txtRadioText.setText(null);

                    // clear the PS text
                    txtPsText.setText(null);

                    break;

                /* Display the RDS text on UI */
                case EVENT_RDS_TEXT:
                    //Log.i(TAG, "enter handleMessage ----EVENT_RDS_TEXT");
                    if (FM_SEND_RDS_IN_BYTEARRAY) {
                        byte[] rdsText = (byte[]) msg.obj;

                        for (int i = 0; i < 4; i++) {
                            Log.i(TAG, "rdsText" + rdsText[i]);
                        }
                    } else {
                        String rds = (String) msg.obj;
                        //Log.i(TAG, "enter handleMessage ----EVENT_RDS_TEXT RDS:" + rds);
                        //only change if new text. avoids RDS text flickering on radio interferences
                        if (rds.length() > 0) {
                            txtRadioText.setText(" - " + rds);
                        }
                    }
                    break;

                /* Display the RDS text on UI */
                case EVENT_PI_CODE:
                    String pi = (String) msg.obj;
                    Log.i(TAG, "enter handleMessage ----EVENT_PI_CODE rds" + pi);

                    break;

                case EVENT_SET_CHANNELSPACE:
                    Log.i(TAG, "enter handleMessage ----EVENT_SET_CHANNELSPACE");
                    break;


                case EVENT_GET_CHANNEL_SPACE_CHANGE:
                    Log.i(TAG, "enter handleMessage ----EVENT_GET_CHANNEL_SPACE_CHANGE");
                    Long gChSpace = (Long) msg.obj;
                    Log.d(TAG, "enter handleMessage ----gChSpace" + gChSpace);
                    break;

                /* tune to default frequency after the band change callback . */

                case EVENT_BAND_CHANGE:
                    Log.i(TAG, "enter handleMessage ----EVENT_BAND_CHANGE");
                    /*
                    * Tune to the last stored frequency at the
                    * enable/re-enable,else tune to the default frequency when band
                    * changes
                    */

                    if (sdefaultSettingOn) {
                        /* Set the default frequency */
                        if (sBand == FM_BAND_EUROPE_US) {
                            lastTunedFrequency = DEFAULT_FREQ_EUROPE;
                        } else {
                            lastTunedFrequency = DEFAULT_FREQ_JAPAN;
                        }
                    }

                    mStatus = sFmReceiver.tune((int) (lastTunedFrequency * 1000));
                    if (!mStatus) {
                        showAlert(getParent(), "FmReceiver", getString(R.string.not_able_to_tune));
                    }

                    break;

                /* Enable RDS system after enable RDS callback . */

                case EVENT_ENABLE_RDS:
                    Log.i(TAG, "enter handleMessage ----EVENT_ENABLE_RDS");
                    break;

                /* Set RSSI after SET_RDS_AF callback */
                case EVENT_SET_RDS_AF:
                    Log.i(TAG, "enter handleMessage ----EVENT_SET_RDS_AF");
                    break;
                /* Set RDS AF after SET_RDS_SYSTEM callback */
                case EVENT_SET_RDS_SYSTEM:
                    Log.i(TAG, "enter handleMessage ----EVENT_SET_RDS_SYSTEM");
                    break;
                /* Set RSSI after disable RDS callback */
                case EVENT_DISABLE_RDS:
                    Log.i(TAG, "enter handleMessage ----EVENT_DISABLE_RDS");
                    txtPsText.setText(null);
                    txtRadioText.setText(null);
                    break;

                case EVENT_SET_DEEMP_FILTER:
                    Log.i(TAG, "enter handleMessage ----EVENT_SET_DEEMP_FILTER");
                    break;

                /* Display the PS text on UI */
                case EVENT_PS_CHANGED:
                    //Log.i(TAG, "enter handleMessage ----EVENT_PS_CHANGED");

                    if (FM_SEND_RDS_IN_BYTEARRAY) {
                        byte[] psName = (byte[]) msg.obj;

                        for (int i = 0; i < 4; i++) {
                            //Log.i(TAG, "psName" + psName[i]);
                        }
                    } else {
                        mPS = (String) msg.obj;
                        //Log.i(TAG, "enter handleMessage ----EVENT_PS_CHANGED PS:" + mPS);
                        txtPsText.setText(mPS);
                    }

                    break;

                case EVENT_SET_RSSI_THRESHHOLD:
                    Log.i(TAG, "enter handleMessage ----EVENT_SET_RSSI_THRESHHOLD");
                    /*
                    * All the configurations will be completed here. So set the
                    * state to idle, so that user can configure again
                    */
                    configurationState = CONFIGURATION_STATE_IDLE;
                    break;
                case EVENT_SET_RF_DEPENDENT_MUTE:
                    Log.i(TAG, "enter handleMessage ----EVENT_SET_RF_DEPENDENT_MUTE");
                    break;

                case EVENT_COMPLETE_SCAN_STOP:
                    Log.i(TAG, "enter handleMessage ----EVENT_COMPLETE_SCAN_STOP");
                    break;

                case EVENT_COMPLETE_SCAN_DONE:
                    Log.i(TAG, "enter handleMessage ----EVENT_COMPLETE_SCAN_DONE");

                    int[] channelList = (int[]) msg.obj;
                    int noOfChannels = msg.arg2;

                    Log.i(TAG, "noOfChannels" + noOfChannels);

                    for (int i = 0; i < noOfChannels; i++) {
                        Log.i(TAG, "channelList" + channelList[i]);
                    }

                    break;

                case EVENT_GET_BAND:

                    Long gBand = (Long) msg.obj;
                    Log.d(TAG, "enter handleMessage ----gBand" + gBand);
                    break;

                case EVENT_GET_FREQUENCY:

                    Integer gFreq = (Integer) msg.obj;
                    Log.d(TAG, "enter handleMessage ----gFreq" + gFreq);
                    break;
                case EVENT_GET_VOLUME:
                    Long gVol = (Long) msg.obj;
                    Log.d(TAG, "enter handleMessage ----gVol" + gVol);
                    break;
                case EVENT_GET_MODE:
                    Long gMode = (Long) msg.obj;
                    Log.d(TAG, "enter handleMessage ----gMode" + gMode);
                    break;
                case EVENT_GET_MUTE_MODE:

                    Long gMuteMode = (Long) msg.obj;

                    if (gMuteMode == (long) FM_UNMUTE) {
                        imgFmVolume.setImageResource(R.drawable.fm_volume);
                    } else if (gMuteMode == (long) FM_MUTE) {
                        imgFmVolume.setImageResource(R.drawable.fm_volume_mute);
                    }

                    Log.d(TAG, "enter handleMessage ----gMuteMode" + gMuteMode);
                    break;
                case EVENT_GET_RF_MUTE_MODE:

                    Long gRfMuteMode = (Long) msg.obj;
                    Log.d(TAG, "enter handleMessage ----gRfMuteMode" + gRfMuteMode);
                    break;
                case EVENT_GET_RSSI_THRESHHOLD:
                    Long gRssi = (Long) msg.obj;
                    Log.d(TAG, "enter handleMessage ----gRssi" + gRssi);
                    break;

                case EVENT_GET_RSSI:
                    Integer rssi = (Integer) msg.obj;
                    Log.d(TAG, "enter handleMessage ----rssi" + rssi);
                    break;
                case EVENT_GET_DEEMPHASIS_FILTER:
                    Long gFilter = (Long) msg.obj;
                    Log.d(TAG, "enter handleMessage ----gFilter" + gFilter);
                    break;

                case EVENT_GET_RDS_SYSTEM:
                    Long gRdsSys = (Long) msg.obj;
                    Log.d(TAG, "enter handleMessage ----gRdsSys" + gRdsSys);
                    break;
                case EVENT_GET_RDS_GROUPMASK:
                    Long gRdsMask = (Long) msg.obj;
                    Log.d(TAG, "enter handleMessage ----gRdsMask" + gRdsMask);
                    break;

                case EVENT_MASTER_VOLUME_CHANGED:
                    Integer vol = (Integer) msg.obj;
                    mVolume = vol;
                    Log.d(TAG, "enter handleMessage ----mVolume" + vol);

                    break;

                case EVENT_FM_ERROR:

                    Log.i(TAG, "enter handleMessage ----EVENT_FM_ERROR");
                    // showAlert(getParent(), "FmRadio", "Error!!!!");

                    LayoutInflater inflater = getLayoutInflater();
                    View layout = inflater.inflate(R.layout.toast,
                            (ViewGroup) findViewById(R.id.toast_layout));
                    TextView text = (TextView) layout.findViewById(R.id.text);
                    text.setText(R.string.error_in_fm_app);

                    Toast toast = new Toast(getApplicationContext());
                    toast
                            .setGravity(Gravity.BOTTOM | Gravity.CENTER_VERTICAL,
                                    0, 0);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.setView(layout);
                    toast.show();
                    break;

            }
        }
    };

    /* Display alert dialog */
    public void showAlert(Context context, String title, String msg) {

        new AlertDialog.Builder(context).setTitle(title).setIcon(
                android.R.drawable.ic_dialog_alert).setMessage(msg)
                .setNegativeButton(android.R.string.cancel, null).show();

    }


    private void setRdsConfig() {
        Log.i(TAG, "setRdsConfig()-entered");
        configurationState = CONFIGURATION_STATE_PENDING;
        SharedPreferences fmConfigPreferences = getSharedPreferences(
                "fmConfigPreferences", MODE_PRIVATE);

        // Set Band
        int band = fmConfigPreferences.getInt(BAND, DEFAULT_BAND);
        Log.i(TAG, "setRdsConfig()--- band= " + band);
        if (band != sBand) // If Band is same as the one set already do not set
        // it again
        {

            if (MAKE_FM_APIS_BLOCKING) {
                // Code for blocking call
                mStatus = sFmReceiver.setBand(band);
                if (!mStatus) {
                    Log.e(TAG, "setRdsConfig()-- setBand ->Error");
                    showAlert(this, "FmReceiver",
                            getString(R.string.not_able_to_setband_to_value));
                } else {
                    sBand = band;
                    if (sdefaultSettingOn) {
                        /* Set the default frequency */
                        if (sBand == FM_BAND_EUROPE_US) {
                            lastTunedFrequency = DEFAULT_FREQ_EUROPE;
                        } else {
                            lastTunedFrequency = DEFAULT_FREQ_JAPAN;
                        }
                    }

                    mStatus = sFmReceiver.tune((int) (lastTunedFrequency * 1000));
                    if (!mStatus) {
                        showAlert(getParent(), "FmReceiver", getString(R.string.not_able_to_tune));
                    }

                }

            } else {

                mStatus = sFmReceiver.setBand(band);
                if (!mStatus) {
                    Log.e(TAG, "setRdsConfig()-- setBand ->Error");
                    showAlert(this, "FmReceiver",
                            getString(R.string.not_able_to_setband_to_value));
                } else {
                    sBand = band;
                    if (sdefaultSettingOn) {
                        /* Set the default frequency */
                        if (sBand == FM_BAND_EUROPE_US) {
                            lastTunedFrequency = DEFAULT_FREQ_EUROPE;
                        } else {
                            lastTunedFrequency = DEFAULT_FREQ_JAPAN;
                        }
                    }

                }
            }
        }


        // Set De-emp Filter
        int deEmp = fmConfigPreferences.getInt(DEEMP, DEFAULT_DEEMP);
        if (mDeEmpFilter != deEmp)// If De-Emp filter is same as the one set
        // already do not set it again
        {
            if (MAKE_FM_APIS_BLOCKING) {
                mStatus = sFmReceiver.setDeEmphasisFilter(deEmp);
            }
            //else
            // mStatus = sFmReceiver.rxSetDeEmphasisFilter_nb(deEmp);

            Log.i(TAG, "setRdsConfig()--- DeEmp= " + deEmp);

            if (!mStatus) {
                Log.e(TAG, "setRdsConfig()-- setDeEmphasisFilter ->Error");
                showAlert(this, "FmReceiver",
                        getString(R.string.not_able_to_set_deemp_filter_to_value));

            }
            mDeEmpFilter = deEmp;

        }


        // Set Mode
        int mode = fmConfigPreferences.getInt(MODE, DEFAULT_MODE);
        if (mMode != mode)// If Mode is same as the one set already do not set it
        // again
        {

            if (MAKE_FM_APIS_BLOCKING) {
                mStatus = sFmReceiver.setMonoStereoMode(mode);
            }
            //else
            //mStatus = sFmReceiver.rxSetMonoStereoMode_nb(mode);
            if (!mStatus) {
                showAlert(this, "FmReceiver", getString(R.string.not_able_to_set_mode));
            } else {
                mMode = mode;
                if (mMode == 0) {
                    imgFmMode.setImageResource(R.drawable.fm_stereo);
                } else {
                    imgFmMode.setImageResource(R.drawable.fm_mono);
                }
            }

        }


        /** Set channel spacing to the one selected by the user */
        int channelSpace = fmConfigPreferences.getInt(CHANNELSPACE,
                DEFAULT_CHANNELSPACE);
        Log.i(TAG, "setChannelSpacing()--- channelSpace= " + channelSpace);
        if (channelSpace != sChannelSpace) // If channelSpace is same as the one
        // set already do not set
        // it again
        {
            if (MAKE_FM_APIS_BLOCKING) {
                mStatus = sFmReceiver.setChannelSpacing(channelSpace);
            }
            // else
            //     mStatus = sFmReceiver.rxSetChannelSpacing_nb(channelSpace);

            if (!mStatus) {
                Log.e(TAG, "setChannelSpacing()-- setChannelSpacing ->Error");
                showAlert(this, "FmReceiver",
                        getString(R.string.not_able_to_set_channel_spacing_to_value));
            }
            sChannelSpace = channelSpace;
        }


        // set RDS related configuration
        boolean rdsEnable = fmConfigPreferences.getBoolean(RDS, DEFAULT_RDS);
        Log.i(TAG, "setRDS()--- rdsEnable= " + rdsEnable);
        if (mRds != rdsEnable) {

            if (rdsEnable) {
                if (MAKE_FM_APIS_BLOCKING) {
                    mStatus = sFmReceiver.enableRds();
                }
                //else
                //    mStatus = sFmReceiver.rxEnableRds_nb();
                if (!mStatus) {
                    Log.e(TAG, "setRDS()-- enableRds() ->Error");
                    showAlert(this, "FmReceiver", getString(R.string.not_able_enable_rds));
                }

            } else {
                if (MAKE_FM_APIS_BLOCKING) {
                    mStatus = sFmReceiver.disableRds();
                }
                // else
                //     mStatus = sFmReceiver.rxDisableRds_nb();

                if (!mStatus) {
                    Log.e(TAG, "setRDS()-- disableRds() ->Error");
                    showAlert(this, "FmReceiver", getString(R.string.not_able_disable_rds));
                } else {
                    Log.e(TAG, "setRDS()-- disableRds() ->success");
                    /* clear the PS and RDS text */
                    txtPsText.setText(null);
                    txtRadioText.setText(null);
                }
            }
            mRds = rdsEnable;
        }

        // setRdssystem
        int rdsSystem = fmConfigPreferences.getInt(RDSSYSTEM,
                DEFAULT_RDS_SYSTEM);
        if (DBG) {
            Log.d(TAG, "setRdsSystem()--- rdsSystem= " + rdsSystem);
        }
        if (mRdsSystem != rdsSystem) {
            // Set RDS-SYSTEM if a new choice is made by the user

            if (MAKE_FM_APIS_BLOCKING) {
                mStatus = sFmReceiver.setRdsSystem(fmConfigPreferences.getInt(
                        RDSSYSTEM, DEFAULT_RDS_SYSTEM));
            }
            //else
            //mStatus = sFmReceiver.rxSetRdsSystem_nb(fmConfigPreferences.getInt(
            //        RDSSYSTEM, DEFAULT_RDS_SYSTEM));

            if (!mStatus) {
                Log.e(TAG, " setRdsSystem()-- setRdsSystem ->Error");
                showAlert(this, "FmReceiver",
                        getString(R.string.not_able_to_set_rds_to_value));
            }
            mRdsSystem = rdsSystem;
        }

        boolean rdsAfSwitch = fmConfigPreferences.getBoolean(RDSAF,
                DEFAULT_RDS_AF);
        int rdsAf = 0;
        rdsAf = rdsAfSwitch ? 1 : 0;
        if (DBG) {
            Log.d(TAG, "setRdsAf()--- rdsAfSwitch= " + rdsAf);
        }
        if (mRdsAf != rdsAfSwitch) {
            // Set RDS-AF if a new choice is made by the user

            if (MAKE_FM_APIS_BLOCKING) {
                mStatus = sFmReceiver.setRdsAfSwitchMode(rdsAf);
            }
            //else
            //mStatus = sFmReceiver.rxSetRdsAfSwitchMode_nb(rdsAf);
            if (!mStatus) {
                Log.e(TAG, "setRdsAf()-- setRdsAfSwitchMode(1) ->Error");
                showAlert(this, "FmReceiver", getString(R.string.not_able_to_set_rds_af_on));
            }
            mRdsAf = rdsAfSwitch;
        }
        // Set Rssi
        int rssiThreshHold = fmConfigPreferences.getInt(RSSI, DEFAULT_RSSI);
        Log.i(TAG, "setRssi()-ENTER --- rssiThreshHold= " + rssiThreshHold);

        // Set RSSI if a new value is entered by the user

        if (MAKE_FM_APIS_BLOCKING) {
            mStatus = sFmReceiver.setRssiThreshold(rssiThreshHold);
        }
        //else
        //    mStatus = sFmReceiver.rxSetRssiThreshold_nb(rssiThreshHold);

        if (!mStatus) {
            showAlert(this, "FmReceiver", getString(R.string.not_able_to_set_rssi_threshold));
        }

        mRssi = rssiThreshHold;


        Log.i(TAG, "setRdsConfig()-exit");

    }

    /* Load the Default values from the preference when the application starts */
    private void loadDefaultConfiguration() {

        Log.i(TAG, "loadDefaultConfiguration()-entered");
        SharedPreferences fmConfigPreferences = getSharedPreferences("fmConfigPreferences",
                MODE_PRIVATE);

        sBand = fmConfigPreferences.getInt(BAND, DEFAULT_BAND);
        lastTunedFrequency = fmConfigPreferences.getFloat(FREQUENCY,
                (sBand == FM_BAND_EUROPE_US ? DEFAULT_FREQ_EUROPE
                        : DEFAULT_FREQ_JAPAN));
        mMode = fmConfigPreferences.getInt(MODE, DEFAULT_MODE);
        mToggleMute = fmConfigPreferences.getBoolean(MUTE, false);
        mRdsState = fmConfigPreferences.getBoolean(RDS, false);

        if (DBG) {
            Log.d(TAG, " Load default band " + sBand + "default volume" + mVolume + "last fre"
                    + lastTunedFrequency + "mode" + mMode + "mToggleMute" + mToggleMute + "mRdsState" + mRdsState);
        }

    }

    /* Save the Default values to the preference when the application exits */
    private void saveDefaultConfiguration() {
        Log.i(TAG, "saveDefaultConfiguration()-Entered");

        SharedPreferences fmConfigPreferences = getSharedPreferences(
                "fmConfigPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = fmConfigPreferences.edit();
        editor.putInt(BAND, sBand);
        editor.putBoolean(MUTE, mToggleMute);
        editor.putFloat(FREQUENCY, lastTunedFrequency);
        if (DBG) {
            Log.d(TAG, " save default band " + sBand + "default volume" + mVolume
                    + "last fre" + lastTunedFrequency + "mToggleMute" + mToggleMute);
        }
        editor.commit();
    }

    /* Initialise all the widgets */
    private void initControls() {
        Log.i(TAG, "enter initControls");

        imgFmMode = (ImageView) findViewById(R.id.imgMode);
        if (mMode == 0) {
            Log.i(TAG, "> setting stereo icon: " + mMode);
            imgFmMode.setImageResource(R.drawable.fm_stereo);
        } else {
            Log.i(TAG, "> setting mono icon: " + mMode);
            imgFmMode.setImageResource(R.drawable.fm_mono);
        }

        imgFmVolume = (ImageView) findViewById(R.id.imgMute);
        imgFmVolume.setOnClickListener(this);

        if (mToggleMute) {
            imgFmVolume.setImageResource(R.drawable.fm_volume_mute);
            Log.i(TAG, "> initControls  mute: " + mToggleMute);
        } else {
            imgFmVolume.setImageResource(R.drawable.fm_volume);
            Log.i(TAG, "> initControls  mute: " + mToggleMute);
        }


        ImageButton imageButtonAux = (ImageButton) findViewById(R.id.imgseekup);
        imageButtonAux.setOnClickListener(this);

        imageButtonAux = (ImageButton) findViewById(R.id.imgseekdown);
        imageButtonAux.setOnClickListener(this);

        txtStatusMsg = (TextView) findViewById(R.id.txtStatusMsg);
        txtRadioText = (TextView) findViewById(R.id.txtRadioText);
        txtPsText = (TextView) findViewById(R.id.txtPsText);

        Button btnFrequency = (Button) findViewById(R.id.btn_set_frequency);
        btnFrequency.setEnabled(true);
        btnFrequency.setOnClickListener(this);

        // ImageSwitcher for FM frequency
        initImageSwitcher();


        //read and present PreSets
        readPreSetsDatabase();
    }

    private void initNotifications() {
        //set up notifications
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification mNotification = new Notification(R.drawable.fm_statusbar_icon, getString(R.string.app_name), System.currentTimeMillis());

        Intent notificationIntent = new Intent(this, FmRxApp.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mNotification.contentIntent = contentIntent;
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification);
        contentView.setOnClickPendingIntent(R.id.ib_status_bar_collapse, buildServiceIntent(COMMAND_CLEAR));
        contentView.setOnClickPendingIntent(R.id.ib_seek_up, buildServiceIntent(COMMAND_SEEK_UP));
        contentView.setOnClickPendingIntent(R.id.ib_seek_down, buildServiceIntent(COMMAND_SEEK_DOWN));
        mNotification.contentView = contentView;

        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    /**
     * Reads the PreSets database and set's the UI in place according to it
     */
    private void readPreSetsDatabase() {
        PreSetsDB preSetsDB = new PreSetsDB(FmRxApp.this);
        preSetsDB.open();

        preSetRadios = preSetsDB.getAllPreSetRadios();
        //if application is starting there should be no stations set, so set some empty ones
        if (preSetRadios.size() == 0) {
            // create 1 empty radio, which will become an Add Button
            preSetsDB.createPreSetItem(getString(R.string.empty_text), "");
            preSetRadios = preSetsDB.getAllPreSetRadios();
        }

        preSetsDB.close();
        // display list
        ListView lv = (ListView) findViewById(R.id.lv_presets);
        lv.setDividerHeight(0);
        lv.setOnItemClickListener(this);
        lv.setOnItemLongClickListener(this);
        lv.setAdapter(new PreSetsAdapter(this, preSetRadios));
    }

    private void updateNotification(float frequency, String name) {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification = new Notification(R.drawable.fm_statusbar_icon, getString(R.string.app_name), System.currentTimeMillis());
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification);
        NumberFormat fmt = new DecimalFormat("#0.0");
        contentView.setTextViewText(R.id.tv_frequency, fmt.format(frequency).replaceAll(",", "."));
        contentView.setTextViewText(R.id.tv_station_name, name);
        /*if (!Preferences.getNotificationsUseRDSinsteadPreset(FmRxApp.this)){
            // USE PreSet name

        } else {
            // use RDS value
            //TODO: notifications RDS value assign and update
        } */
        mNotification.contentView = contentView;
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    /**
     * Adds Delay of 3 seconds
     */
    private void insertDelayThread() {

        new Thread() {
            public void run() {
                try {
                    // Add some delay to make sure all configuration has been
                    // completed.
                    sleep(3000);
                } catch (Exception e) {
                    Log.e(TAG, "InsertDelayThread()-- Exception !!");
                }
                // Dismiss the Dialog
                configPd.dismiss();
            }
        }.start();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult");

        switch (requestCode) {
            case (ACTIVITY_TUNE): {
                if (resultCode == Activity.RESULT_OK && data != null) {

                    Bundle extras = data.getExtras();
                    if (extras != null) {

                        lastTunedFrequency = extras.getFloat(FREQ_VALUE, 0);
                        updateFrequencyDisplay(lastTunedFrequency);
                        mStatus = sFmReceiver.tune((int) (lastTunedFrequency * 1000));
                        if (!mStatus) {
                            showAlert(this, "FmReceiver", getString(R.string.not_able_to_tune));
                        }
                    }
                }
            }
            break;

            case (ACTIVITY_CONFIG): {
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "ActivityFmRdsConfig configurationState "
                            + configurationState);
                    if (configurationState == CONFIGURATION_STATE_IDLE) {


                        setRdsConfig();
                        configPd = ProgressDialog.show(this, getString(R.string.please_wait),
                                getString(R.string.applying_new_config), true, false);
                        // The delay is inserted to make sure all the configurations
                        // have been completed.
                        insertDelayThread();
                    }
                }

            }
            break;

        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:

                return true;
            case KeyEvent.KEYCODE_DPAD_UP:

                return true;
            case KeyEvent.KEYCODE_BACK:
                /*
                 * Show a radio notification when the user presses back button and
                 * leaves FM app. The FM radio is still on
                 */
//                this.showNotification(R.drawable.radio, R.string.app_rx,
                //                      txtFmRxTunedFreq.getText(), false);
                saveDefaultConfiguration();
                finish();
                return true;

            /* Keys A to L are mapped to different get APIs for Testing */
            case KeyEvent.KEYCODE_A:

                /*if (MAKE_FM_APIS_BLOCKING == true) {
                          // Code for blocking call
                                      Log.i(TAG, "Testing getVolume()  returned volume = "
                        + sFmReceiver.rxGetVolume());

                        } else {
                          // Code for non blocking call
                                      Log.i(TAG, "Testing getVolume_nb()  returned volume = "
                        + sFmReceiver.rxGetVolume_nb());

                        }

                */
                return true;

            case KeyEvent.KEYCODE_B:

                if (MAKE_FM_APIS_BLOCKING) {
                    // Code for blocking call
                    Log.i(TAG, "Testing getTunedFrequency()  returned Tuned Freq = "
                            + sFmReceiver.getTunedFrequency());

                } else {
                    // Code for non blocking call
                    //            Log.i(TAG, "Testing getTunedFrequency_nb()  returned Tuned Freq = "
                    //          + sFmReceiver.rxGetTunedFrequency_nb());

                }

                return true;

            case KeyEvent.KEYCODE_C:
                if (MAKE_FM_APIS_BLOCKING) {
                    // Code for blocking call
                    Log.i(TAG, "Testing getRssiThreshold()    returned RSSI thrshld = "
                            + sFmReceiver.getRssiThreshold());
                } else {
                    // Code for non blocking call
                    //  Log.i(TAG, "Testing getRssiThreshold_nb()    returned RSSI thrshld = "
                    // + sFmReceiver.rxGetRssiThreshold_nb());
                }

                return true;

            case KeyEvent.KEYCODE_D:
                if (MAKE_FM_APIS_BLOCKING) {
                    // Code for blocking call
                    Log.i(TAG, "Testing getBand() returned Band  = "
                            + sFmReceiver.getBand());
                } else {
                    // Code for non blocking call
                    //       Log.i(TAG, "Testing getBand_nb() returned Band  = "
                    //       + sFmReceiver.rxGetBand_nb());
                }

                return true;

            case KeyEvent.KEYCODE_E:
                if (MAKE_FM_APIS_BLOCKING) {
                    // Code for blocking call
                    Log.i(TAG, "Testing getDeEmphasisFilter()    returned De-emp  = "
                            + sFmReceiver.getDeEmphasisFilter());
                } else {
                    // Code for non blocking call
                    //       Log.i(TAG, "Testing getDeEmphasisFilter_nb()    returned De-emp  = "
                    //       + sFmReceiver.rxGetDeEmphasisFilter_nb());
                }

                return true;

            case KeyEvent.KEYCODE_F:
                if (MAKE_FM_APIS_BLOCKING) {
                    // Code for blocking call
                    Log.i(TAG, "Testing getMonoStereoMode() returned MonoStereo = "
                            + sFmReceiver.getMonoStereoMode());

                } else {
                    // Code for non blocking call
                    //            Log.i(TAG, "Testing getMonoStereoMode_nb() returned MonoStereo = "
                    //         + sFmReceiver.rxGetMonoStereoMode_nb());

                }

                return true;

            case KeyEvent.KEYCODE_G:
                if (MAKE_FM_APIS_BLOCKING) {
                    // Code for blocking call
                    Log.i(TAG, "Testing getMuteMode()  returned MuteMode = "
                            + sFmReceiver.getMuteMode());
                } else {
                    // Code for non blocking call
                    //      Log.i(TAG, "Testing getMuteMode_nb()  returned MuteMode = "
                    //      + sFmReceiver.rxGetMuteMode_nb());
                }

                return true;

            case KeyEvent.KEYCODE_H:
                if (MAKE_FM_APIS_BLOCKING) {
                    // Code for blocking call
                    Log.i(TAG,
                            "Testing getRdsAfSwitchMode()    returned RdsAfSwitchMode = "
                                    + sFmReceiver.getRdsAfSwitchMode());
                } else {
                    // Code for non blocking call
                    //       Log.i(TAG,
                    //       "Testing getRdsAfSwitchMode_nb()    returned RdsAfSwitchMode = "
                    //               + sFmReceiver.rxGetRdsAfSwitchMode_nb());
                }

                return true;

            case KeyEvent.KEYCODE_I:
                if (MAKE_FM_APIS_BLOCKING) {
                    // Code for blocking call
                    Log.i(TAG, "Testing getRdsGroupMask() returned RdsGrpMask = "
                            + sFmReceiver.getRdsGroupMask());
                } else {
                    // Code for non blocking call
                    //    Log.i(TAG, "Testing getRdsGroupMask_nb() returned RdsGrpMask = "
                    //    + sFmReceiver.rxGetRdsGroupMask_nb());
                }

                return true;

            case KeyEvent.KEYCODE_J:
                if (MAKE_FM_APIS_BLOCKING) {
                    // Code for blocking call
                    Log.i(TAG, "Testing getRdsSystem() returned Rds System = "
                            + sFmReceiver.getRdsSystem());
                } else {
                    // Code for non blocking call
                    //    Log.i(TAG, "Testing getRdsSystem_nb() returned Rds System = "
                    //    + sFmReceiver.rxGetRdsSystem_nb());
                }

                return true;

            case KeyEvent.KEYCODE_K:
                if (MAKE_FM_APIS_BLOCKING) {
                    // Code for blocking call
                    Log.i(TAG,
                            "Testing getRfDependentMuteMode()    returned RfDepndtMuteMode = "
                                    + sFmReceiver.getRfDependentMuteMode());
                } else {
                    // Code for non blocking call
                    //      Log.i(TAG,
                    //      "Testing getRfDependentMuteMode_nb()    returned RfDepndtMuteMode = "
                    //              + sFmReceiver.rxGetRfDependentMuteMode_nb());
                }

                return true;

            case KeyEvent.KEYCODE_L:

                if (MAKE_FM_APIS_BLOCKING) {

                    LayoutInflater inflater = getLayoutInflater();
                    View layout = inflater.inflate(R.layout.toast,
                            (ViewGroup) findViewById(R.id.toast_layout));
                    TextView text = (TextView) layout.findViewById(R.id.text);
                    text.setText("The current Rssi    " + sFmReceiver.getRssi());

                    Toast toast = new Toast(getApplicationContext());
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_VERTICAL, 0, 0);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.setView(layout);
                    toast.show();
                } else {
                    // Log.i(TAG,
                    //     "Testing rxGetRssi_nb()    returned  = "
                    //             + sFmReceiver.rxGetRssi_nb());
                }

                return true;

            case KeyEvent.KEYCODE_M:
                Log.i(TAG, "Testing isValidChannel()    returned isValidChannel = "
                        + sFmReceiver.isValidChannel());

                return true;

            case KeyEvent.KEYCODE_N:
                Log.i(TAG, "Testing getFwVersion()    returned getFwVersion = "
                        + sFmReceiver.getFwVersion());

                return true;

            case KeyEvent.KEYCODE_O:
                if (MAKE_FM_APIS_BLOCKING) {
                    // Code for blocking call
                    Log.i(TAG,
                            "Testing getChannelSpacing()    returned getChannelSpacing = "
                                    + sFmReceiver.getChannelSpacing());
                } else {
                    // Code for non blocking call
                    //  Log.i(TAG,
                    //  "Testing getChannelSpacing_nb()    returned getChannelSpacing = "
                    //          + sFmReceiver.rxGetChannelSpacing_nb());
                }


                return true;

            case KeyEvent.KEYCODE_P:
                Log.i(TAG, "Testing completescan()");
                sFmReceiver.completeScan();
                return true;

            case KeyEvent.KEYCODE_Q:

                if (MAKE_FM_APIS_BLOCKING) {
                    Log.i(TAG,
                            "Testing getCompleteScanProgress()    returned scan progress = "
                                    + sFmReceiver.getCompleteScanProgress());
                } else {
                    //               Log.i(TAG,
                    //       "Testing getCompleteScanProgress()    returned scan progress = "
                    //               + sFmReceiver.rxGetCompleteScanProgress_nb());
                }

                return true;

            case KeyEvent.KEYCODE_R:
                if (MAKE_FM_APIS_BLOCKING) {
                    Log.i(TAG, "Testing stopCompleteScan()    returned status = "
                            + sFmReceiver.stopCompleteScan());
                } else {
                    //             Log.i(TAG, "Testing stopCompleteScan()    returned status = "
                    //     + sFmReceiver.rxStopCompleteScan_nb());
                }

                return true;

            case KeyEvent.KEYCODE_S:

                if (MAKE_FM_APIS_BLOCKING) {
                    // Code for blocking call
                    Log.i(TAG,
                            "Testing setRfDependentMuteMode()    returned RfDepndtMuteMode = "
                                    + sFmReceiver.setRfDependentMuteMode(1));
                } else {
                    // Code for non blocking call
                    /*Log.i(TAG,
       "Testing setRfDependentMuteMode()    returned RfDepndtMuteMode = "
               + sFmReceiver.rxSetRfDependentMuteMode_nb(1));    */
                }

                return true;

        }

        return false;
    }

    /**
     * Get the stored frequency from the arraylist and tune to that frequency
     *
     * @param text frequency
     */
    void tuneStationFrequency(String text) {
        tuneStationFrequency(text, "");
    }

    /**
     * Get the stored frequency from the arraylist and tune to that frequency
     *
     * @param text frequency
     * @param name Name - For updating notifications
     */
    void tuneStationFrequency(String text, String name) {
        try {
            float iFreq = Float.parseFloat(text);
            if (iFreq != 0) {
                lastTunedFrequency = iFreq * 10;
                Log.d(TAG, "lastTunedFrequency" + lastTunedFrequency);

                mStatus = sFmReceiver.tune(lastTunedFrequency.intValue() * 100);
                if (!mStatus) {
                    showAlert(getParent(), "FmReceiver", getString(R.string.not_able_to_tune));
                }
                //does notifications need initialization?
                if (isFirstPlay && !hidNotification) {
                    isFirstPlay = false;
                    initNotifications();
                }

                //update notifications bar
                if (!hidNotification) {
                    updateNotification(iFreq, name);
                }
            } else {

                new AlertDialog.Builder(this).setIcon(
                        android.R.drawable.ic_dialog_alert).setMessage(
                        "Enter valid frequency!!").setNegativeButton(
                        android.R.string.ok, null).show();

            }
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "nfe");
        }
    }

    public void onClick(View v) {
        int id = v.getId();

        switch (id) {

            case R.id.btn_set_frequency:
                startActivityForResult(new Intent(INTENT_RXTUNE), ACTIVITY_TUNE);
                break;

            case R.id.imgMute:
                if (mToggleMute) {
                    mStatus = sFmReceiver.setMuteMode(FM_MUTE);
                } else {
                    mStatus = sFmReceiver.setMuteMode(FM_UNMUTE);
                }
                if (!mStatus) {
                    showAlert(this, "FmRadio", getString(R.string.not_able_to_setmute));
                } else {
                    if (mToggleMute) {
                        imgFmVolume.setImageResource(R.drawable.fm_volume_mute);
                        mToggleMute = false;
                    } else {
                        imgFmVolume.setImageResource(R.drawable.fm_volume);
                        mToggleMute = true;
                    }
                }

                break;

            case R.id.imgseekdown:
                seekDown();

                break;
            case R.id.imgseekup:
                seekUp();
                break;
        }

    }


    private void seekDown() {
        mDirection = FM_SEEK_DOWN;
        // FM seek down

        if (mSeekState == SEEK_REQ_STATE_IDLE) {
            mStatus = sFmReceiver.seek(mDirection);
            if (!mStatus) {
                showAlert(this, "FmReceiver", getString(R.string.not_able_to_seek_down));
            } else {
                mSeekState = SEEK_REQ_STATE_PENDING;
                txtStatusMsg.setText(R.string.seeking);
            }

        }
    }

    private void seekUp() {

        mDirection = FM_SEEK_UP;
        // FM seek up
        if (mSeekState == SEEK_REQ_STATE_IDLE) {
            mStatus = sFmReceiver.seek(mDirection);
            if (!mStatus) {
                showAlert(this, "FmRadio", getString(R.string.not_able_to_seek_up));

            } else {
                mSeekState = SEEK_REQ_STATE_PENDING;
                txtStatusMsg.setText(R.string.seeking);
            }
        }
    }

    /* Creates the menu items */
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        MenuItem item;

        item = menu.add(0, MENU_CONFIGURE, 0, R.string.configure);
        item.setIcon(R.drawable.configure);

        item = menu.add(0, MENU_ABOUT, 0, R.string.about);
        item.setIcon(R.drawable.fm_menu_help);

        item = menu.add(0, MENU_EXIT, 0, R.string.exit);
        item.setIcon(R.drawable.radio);

        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case MENU_CONFIGURE:
                /* Start the configuration window */
                Intent irds = new Intent(INTENT_RDS_CONFIG);
                startActivityForResult(irds, ACTIVITY_CONFIG);
                break;

            case MENU_EXIT:
                /*
                 * The exit from the FM application happens here. FM will be
                 * disabled
                 */
                mStatus = sFmReceiver.disable();
                break;

            case MENU_ABOUT:
                /* Start the help window */
                Intent iTxHelp = new Intent(INTENT_RXHELP);
                startActivity(iTxHelp);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        Log.i(TAG, "onSaveInstanceState");
        /* save the fm state into bundle for the activity restart */
        mFmInterrupted = true;
        Bundle fmState = new Bundle();
        fmState.putBoolean(FM_INTERRUPTED_KEY, mFmInterrupted);

        icicle.putBundle(FM_STATE_KEY, fmState);
    }

    public void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
    }

    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");

        if (pd != null) {
            pd.dismiss();
        }

        saveDefaultConfiguration();

    }

    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);

    }

    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        //if(mFmServiceConnected == true)
        startup();
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        /*
         * Unregistering the receiver , so that we dont handle any FM events
         * when out of the FM application screen
         */
        unregisterReceiver(mReceiver);
        // TODO : uncomment line bellow?
        //sFmReceiver.close();
    }

    // Receives all of the FM intents and dispatches to the proper handler

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            String fmAction = intent.getAction();

            Log.i(TAG, "enter onReceive" + fmAction);
            if (fmAction.equals(FmReceiverIntent.FM_ENABLED_ACTION)) {
                Log.i(TAG, "enter onReceive FM_ENABLED_ACTION " + fmAction);

                mHandler.sendMessage(mHandler
                        .obtainMessage(EVENT_FM_ENABLED, 0));
            }
            if (fmAction.equals(FmReceiverIntent.FM_DISABLED_ACTION)) {
                Log.i(TAG, "enter onReceive FM_DISABLED_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_DISABLED,
                        0));
            }

            if (fmAction.equals(FmReceiverIntent.SET_MODE_MONO_STEREO_ACTION)) {
                Log.i(TAG, "enter onReceive SET_MODE_MONO_STEREO_ACTION "
                        + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_MONO_STEREO_CHANGE, 0));
            }
            if (fmAction
                    .equals(FmReceiverIntent.DISPLAY_MODE_MONO_STEREO_ACTION)) {
                Log.i(TAG, "enter onReceive DISPLAY_MODE_MONO_STEREO_ACTION "
                        + fmAction);
                Integer modeDisplay = intent.getIntExtra(
                        FmReceiverIntent.MODE_MONO_STEREO, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_MONO_STEREO_DISPLAY, modeDisplay));
            }

            if (fmAction.equals(FmReceiverIntent.RDS_TEXT_CHANGED_ACTION)) {
                //  Log.i(TAG, "enter onReceive RDS_TEXT_CHANGED_ACTION "+fmAction);
                if (FM_SEND_RDS_IN_BYTEARRAY) {
                    Bundle extras = intent.getExtras();

                    byte[] rdsText = extras.getByteArray(FmReceiverIntent.RDS);
                    int status = extras.getInt(FmReceiverIntent.STATUS, 0);

                    mHandler.sendMessage(mHandler.obtainMessage(EVENT_RDS_TEXT,
                            status, 0, rdsText));
                } else {
                    String rdstext = intent
                            .getStringExtra(FmReceiverIntent.RADIOTEXT_CONVERTED);
                    mHandler.sendMessage(mHandler.obtainMessage(EVENT_RDS_TEXT,
                            rdstext));
                }
            }
            if (fmAction.equals(FmReceiverIntent.PI_CODE_CHANGED_ACTION)) {
                Log.i(TAG, "enter onReceive PI_CODE_CHANGED_ACTION " + fmAction);

                Integer pi = intent.getIntExtra(FmReceiverIntent.PI, 0);

                mHandler.sendMessage(mHandler.obtainMessage(EVENT_PI_CODE, pi
                        .toString()));
            }

            if (fmAction.equals(FmReceiverIntent.TUNE_COMPLETE_ACTION)) {
                Log.i(TAG, "enter onReceive TUNE_COMPLETE_ACTION " + fmAction);

                int tuneFreq = intent.getIntExtra(
                        FmReceiverIntent.TUNED_FREQUENCY, 0);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_TUNE_COMPLETE, tuneFreq));

            }

            if (fmAction.equals(FmReceiverIntent.COMPLETE_SCAN_PROGRESS_ACTION)) {
                Log.i(TAG, "enter onReceive COMPLETE_SCAN_PROGRESS_ACTION " + fmAction);

                int progress = intent.getIntExtra(
                        FmReceiverIntent.SCAN_PROGRESS, 0);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_COMPLETE_SCAN_PROGRESS, progress));

            }


            if (fmAction.equals(FmReceiverIntent.VOLUME_CHANGED_ACTION)) {
                Log.i(TAG, "enter onReceive VOLUME_CHANGED_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_VOLUME_CHANGE, 0));
            }

            if (fmAction.equals(FmReceiverIntent.MUTE_CHANGE_ACTION)) {
                Log.i(TAG, "enter onReceive MUTE_CHANGE_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_MUTE_CHANGE,
                        0));
            }

            if (fmAction.equals(FmReceiverIntent.SEEK_STOP_ACTION)) {
                Log.i(TAG, "enter onReceive SEEK_STOP_ACTION " + fmAction);

                int freq = intent.getIntExtra(FmReceiverIntent.SEEK_FREQUENCY,
                        0);

                mHandler.sendMessage(mHandler.obtainMessage(EVENT_SEEK_STOPPED,
                        freq));
            }

            if (fmAction.equals(FmReceiverIntent.SEEK_ACTION)) {
                Log.i(TAG, "enter onReceive SEEK_ACTION " + fmAction);

                int freq = intent.getIntExtra(FmReceiverIntent.SEEK_FREQUENCY,
                        0);

                mHandler.sendMessage(mHandler.obtainMessage(EVENT_SEEK_STARTED,
                        freq));
            }

            if (fmAction.equals(FmReceiverIntent.BAND_CHANGE_ACTION)) {
                Log.i(TAG, "enter onReceive BAND_CHANGE_ACTION " + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(EVENT_BAND_CHANGE,
                        0));
            }


            if (fmAction.equals(FmReceiverIntent.GET_CHANNEL_SPACE_ACTION)) {
                Log.i(TAG, "enter onReceive GET_CHANNEL_SPACE_ACTION " + fmAction);

                Long chSpace = intent.getLongExtra(
                        FmReceiverIntent.GET_CHANNEL_SPACE, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_CHANNEL_SPACE_CHANGE, chSpace));
            }


            if (fmAction.equals(FmReceiverIntent.SET_CHANNEL_SPACE_ACTION)) {
                Log.i(TAG, "enter onReceive SET_CHANNEL_SPACE_ACTION " + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_SET_CHANNELSPACE, 0));
            }


            if (fmAction.equals(FmReceiverIntent.GET_RDS_AF_SWITCH_MODE_ACTION)) {
                Log.i(TAG, "enter onReceive GET_RDS_AF_SWITCH_MODE_ACTION " + fmAction);

                Long switchMode = intent.getLongExtra(
                        FmReceiverIntent.GET_RDS_AF_SWITCHMODE, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_RDS_AF_SWITCHMODE, switchMode));
            }


            if (fmAction.equals(FmReceiverIntent.GET_VOLUME_ACTION)) {
                Log.i(TAG, "enter onReceive GET_VOLUME_ACTION " + fmAction);

                Long gVolume = intent.getLongExtra(
                        FmReceiverIntent.GET_VOLUME, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_VOLUME, gVolume));
            }


            if (fmAction.equals(FmReceiverIntent.GET_MONO_STEREO_MODE_ACTION)) {
                Log.i(TAG, "enter onReceive GET_MONO_STEREO_MODE_ACTION " + fmAction);

                Long gMode = intent.getLongExtra(
                        FmReceiverIntent.GET_MODE, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_MODE, gMode));
            }


            if (fmAction.equals(FmReceiverIntent.GET_MUTE_MODE_ACTION)) {
                Log.i(TAG, "enter onReceive GET_MUTE_MODE_ACTION " + fmAction);

                Long gMuteMode = intent.getLongExtra(
                        FmReceiverIntent.GET_MUTE_MODE, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_MUTE_MODE, gMuteMode));
            }


            if (fmAction.equals(FmReceiverIntent.GET_BAND_ACTION)) {
                Log.i(TAG, "enter onReceive GET_BAND_ACTION " + fmAction);

                Long gBand = intent.getLongExtra(
                        FmReceiverIntent.GET_BAND, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_BAND, gBand));
            }


            if (fmAction.equals(FmReceiverIntent.GET_FREQUENCY_ACTION)) {
                Log.i(TAG, "enter onReceive GET_FREQUENCY_ACTION " + fmAction);

                int gFreq = intent.getIntExtra(
                        FmReceiverIntent.TUNED_FREQUENCY, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_FREQUENCY, gFreq));
            }


            if (fmAction.equals(FmReceiverIntent.GET_RF_MUTE_MODE_ACTION)) {
                Log.i(TAG, "enter onReceive GET_RF_MUTE_MODE_ACTION " + fmAction);

                Long gRfMuteMode = intent.getLongExtra(
                        FmReceiverIntent.GET_RF_MUTE_MODE, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_RF_MUTE_MODE, gRfMuteMode));
            }


            if (fmAction.equals(FmReceiverIntent.GET_RSSI_THRESHHOLD_ACTION)) {
                Log.i(TAG, "enter onReceive GET_RSSI_THRESHHOLD_ACTION " + fmAction);

                Long gRssiThreshhold = intent.getLongExtra(
                        FmReceiverIntent.GET_RSSI_THRESHHOLD, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_RSSI_THRESHHOLD, gRssiThreshhold));
            }


            if (fmAction.equals(FmReceiverIntent.GET_DEEMPHASIS_FILTER_ACTION)) {
                Log.i(TAG, "enter onReceive GET_DEEMPHASIS_FILTER_ACTION " + fmAction);

                Long gFilter = intent.getLongExtra(
                        FmReceiverIntent.GET_DEEMPHASIS_FILTER, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_DEEMPHASIS_FILTER, gFilter));
            }


            if (fmAction.equals(FmReceiverIntent.GET_RSSI_ACTION)) {
                Log.i(TAG, "enter onReceive GET_RSSI_ACTION " + fmAction);

                int gRssi = intent.getIntExtra(
                        FmReceiverIntent.GET_RSSI, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_RSSI, gRssi));
            }


            if (fmAction.equals(FmReceiverIntent.GET_RDS_SYSTEM_ACTION)) {
                Log.i(TAG, "enter onReceive GET_RDS_SYSTEM_ACTION " + fmAction);

                Long gRdsSystem = intent.getLongExtra(
                        FmReceiverIntent.GET_RDS_SYSTEM, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_RDS_SYSTEM, gRdsSystem));
            }


            if (fmAction.equals(FmReceiverIntent.GET_RDS_GROUPMASK_ACTION)) {
                Log.i(TAG, "enter onReceive GET_RDS_GROUPMASK_ACTION " + fmAction);

                Long gRdsMask = intent.getLongExtra(
                        FmReceiverIntent.GET_RDS_GROUPMASK, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_RDS_GROUPMASK, gRdsMask));
            }


            if (fmAction.equals(FmReceiverIntent.ENABLE_RDS_ACTION)) {
                Log.i(TAG, "enter onReceive ENABLE_RDS_ACTION " + fmAction);

                mHandler.sendMessage(mHandler
                        .obtainMessage(EVENT_ENABLE_RDS, 0));
            }

            if (fmAction.equals(FmReceiverIntent.DISABLE_RDS_ACTION)) {
                Log.i(TAG, "enter onReceive DISABLE_RDS_ACTION " + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(EVENT_DISABLE_RDS,
                        0));
            }

            if (fmAction.equals(FmReceiverIntent.SET_RDS_AF_ACTION)) {
                Log.i(TAG, "enter onReceive SET_RDS_AF_ACTION " + fmAction);

                mHandler.sendMessage(mHandler
                        .obtainMessage(EVENT_SET_RDS_AF, 0));
            }

            if (fmAction.equals(FmReceiverIntent.SET_RDS_SYSTEM_ACTION)) {
                Log.i(TAG, "enter onReceive SET_RDS_SYSTEM_ACTION " + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_SET_RDS_SYSTEM, 0));
            }

            if (fmAction.equals(FmReceiverIntent.SET_DEEMP_FILTER_ACTION)) {
                Log.i(TAG, "enter onReceive SET_DEEMP_FILTER_ACTION "
                        + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_SET_DEEMP_FILTER, 0));
            }

            if (fmAction.equals(FmReceiverIntent.PS_CHANGED_ACTION)) {
                Log.i(TAG, "enter onReceive PS_CHANGED_ACTION " + fmAction);

                if (FM_SEND_RDS_IN_BYTEARRAY) {
                    Bundle extras = intent.getExtras();
                    byte[] psName = extras.getByteArray(FmReceiverIntent.PS);
                    int status = extras.getInt(FmReceiverIntent.STATUS, 0);

                    mHandler.sendMessage(mHandler.obtainMessage(
                            EVENT_PS_CHANGED, status, 0, psName));
                } else {

                    String name = intent
                            .getStringExtra(FmReceiverIntent.PS_CONVERTED);

                    mHandler.sendMessage(mHandler.obtainMessage(
                            EVENT_PS_CHANGED, name));
                }
            }

            if (fmAction.equals(FmReceiverIntent.SET_RSSI_THRESHHOLD_ACTION)) {
                Log.i(TAG, "enter onReceive SET_RSSI_THRESHHOLD_ACTION "
                        + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_SET_RSSI_THRESHHOLD, 0));
            }

            if (fmAction.equals(FmReceiverIntent.SET_RF_DEPENDENT_MUTE_ACTION)) {
                Log.i(TAG, "enter onReceive SET_RF_DEPENDENT_MUTE_ACTION "
                        + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_SET_RF_DEPENDENT_MUTE, 0));
            }

            if (fmAction.equals(FmReceiverIntent.COMPLETE_SCAN_DONE_ACTION)) {
                Log.i(TAG, "enter onReceive COMPLETE_SCAN_DONE_ACTION "
                        + fmAction);

                Bundle extras = intent.getExtras();

                int[] channelList = extras
                        .getIntArray(FmReceiverIntent.SCAN_LIST);

                int noOfChannels = extras.getInt(
                        FmReceiverIntent.SCAN_LIST_COUNT, 0);

                int status = extras.getInt(FmReceiverIntent.STATUS, 0);

                Log.i(TAG, "noOfChannels" + noOfChannels);

                for (int i = 0; i < noOfChannels; i++)

                {
                    Log.i(TAG, "channelList" + channelList[i]);
                }

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_COMPLETE_SCAN_DONE, status, noOfChannels,
                        channelList));
            }

            if (fmAction.equals(FmReceiverIntent.COMPLETE_SCAN_STOP_ACTION)) {
                Log.i(TAG, "enter onReceive COMPLETE_SCAN_STOP_ACTION "
                        + fmAction);
                Bundle extras = intent.getExtras();
                int status = extras.getInt(FmReceiverIntent.STATUS, 0);
                int channelValue = extras.getInt(
                        FmReceiverIntent.LAST_SCAN_CHANNEL, 0);
                Log.i(TAG, "Last Scanned Channel Frequency before calling Stop Scan" + channelValue);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_COMPLETE_SCAN_STOP, status, channelValue));
            }

            if (fmAction.equals(FmReceiverIntent.MASTER_VOLUME_CHANGED_ACTION)) {
                Log.i(TAG, "enter onReceive MASTER_VOLUME_CHANGED_ACTION "
                        + fmAction);
                mVolume = intent.getIntExtra(FmReceiverIntent.MASTER_VOLUME, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_MASTER_VOLUME_CHANGED, mVolume));
            }

            //TODO: handle this errors or deprecate?
            /* if (fmAction.equals(FmReceiverIntent.FM_ERROR_ACTION)) {
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_ERROR, 0));
            }*/

        }
    };

    /* Get the volume */
    void getNewGain(int volume) {
        Log.d(TAG, "getNewGain" + volume);
        if (volume <= GAIN_STEP) {
            mVolume = MIN_VOLUME;
        } else if (volume >= MAX_VOLUME) {
            mVolume = MAX_VOLUME;
        } else {
            mVolume = volume;
        }
    }

    @Override
    public View makeView() {
        ImageView i = new ImageView(this);
        i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        i.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return i;
    }

    /**
     * Updates the TOP frequency display
     *
     * @param currentFreq current frequency like "94.4"
     */
    private void updateFrequencyDisplay(Float currentFreq) {

        int digit1, digit2, digit3, digit4, freq = (int) Math.floor(currentFreq * 10);//100.4 > 1004

        digit1 = freq / 1000;
        freq -= digit1 * 1000;
        digit2 = freq / 100;
        freq -= digit2 * 100;
        digit3 = freq / 10;
        freq -= digit3 * 10;
        digit4 = freq;

        //Log.v(TAG, "FMRadio updateDisplay: currentFreq " + currentFreq + " -> digits " + digit1 + " " + digit2 + " " + digit3 + " " + digit4);

        int[] numbers = NUMBER_IMAGES;

        mFreqDigits[0].setImageResource(numbers[digit1]);
        mFreqDigits[0].setVisibility(digit1 == 0 ? View.GONE : View.VISIBLE);
        mFreqDigits[1].setImageResource(numbers[digit2]);
        mFreqDigits[2].setImageResource(numbers[digit3]);
        mFreqDigits[3].setImageResource(R.drawable.fm_number_point);
        mFreqDigits[4].setImageResource(numbers[digit4]);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        if (preSetRadios != null) {
            if (preSetRadios.get(position).isStationSet()) {
                tuneStationFrequency(preSetRadios.get(position).getStationFrequency(), preSetRadios.get(position).getStationName());
            } else {
                //if not yet set, set it
                updateStation(position, false);
            }
        }
    }

    /**
     * Can either be used to create a new station or edit an existing station
     *
     * @param position  position in the list
     * @param isEditing True if updating existing station, False if creating a new one
     */
    private void updateStation(final int position, final boolean isEditing) {
        final Dialog simpleDialog = new Dialog(FmRxApp.this);
        simpleDialog.setContentView(R.layout.dialog_save_station);
        simpleDialog.setTitle(R.string.choose_station_name);
        simpleDialog.setCancelable(true);
        simpleDialog.setCanceledOnTouchOutside(false);
        Button btnContinue = (Button) simpleDialog.findViewById(R.id.btn_continue);
        final EditText stationName = (EditText) simpleDialog.findViewById(R.id.et_station_name);

        if (isEditing) {
            stationName.setText(preSetRadios.get(position).getStationName());
        } else {
            if (mRds && mPS.length() > 1) {
                stationName.setText(mPS);
            }
        }

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if empty just dismiss
                if (stationName.getText().toString().length() == 0) {
                    simpleDialog.dismiss();
                } else {
                    PreSetsDB preSetsDB = new PreSetsDB(FmRxApp.this);
                    preSetsDB.open();
                    preSetsDB.updateRadioPreSet(preSetRadios.get(position).getUid(),
                            stationName.getText().toString(), lastTunedFrequency.toString());

                    // if we set a station, increment the counter so we can set another one in the future
                    // but only if creating new one
                    if (!isEditing) {
                        preSetsDB.createPreSetItem(getString(R.string.empty_text), "");
                    }

                    preSetsDB.close();

                    //refresh list
                    readPreSetsDatabase();

                    simpleDialog.dismiss();
                }
            }
        });
        simpleDialog.show();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        //should not be able to remove last station, because it acts as new station action
        if (position < preSetRadios.size() - 1) {
            AlertDialog.Builder optionsList = new AlertDialog.Builder(FmRxApp.this);
            optionsList.setTitle(R.string.operations);
            CharSequence[] items = new CharSequence[]{getString(R.string.edit_station),
                    getString(R.string.remove_station)};
            optionsList.setItems(items, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //edit station
                    if (which == 0) {
                        updateStation(position, true);
                    } else if (which == 1) { //remove station
                        AlertDialog.Builder ad = new AlertDialog.Builder(FmRxApp.this);
                        ad.setTitle(R.string.confirm_action);
                        ad.setCancelable(true);
                        ad.setMessage(getString(R.string.confirm_action_remove_station) + " " + preSetRadios.get(position).getStationName());
                        ad.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                PreSetsDB preSetsDB = new PreSetsDB(FmRxApp.this);
                                preSetsDB.open();
                                preSetsDB.deletePreSetItem(preSetRadios.get(position));
                                preSetsDB.close();
                                readPreSetsDatabase();
                            }
                        });

                        ad.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        });

                        ad.show();
                    }
                }
            });

            AlertDialog a1 = optionsList.create();
            a1.show();
        }
        return true;
    }


    /**
     * handling callbacks from Notification bar here
     */

    @Override
    protected void onNewIntent(Intent intent) {
        Log.v(TAG, "called onNewIntent.");
        if (intent.hasExtra(EXTRA_COMMAND)) {
            Log.v(TAG, "Command: " + intent.getStringExtra(EXTRA_COMMAND));
            if (intent.getStringExtra(EXTRA_COMMAND).equals(COMMAND_CLEAR)) {
                NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nMgr.cancel(NOTIFICATION_ID);
                //set this as a control flag so that the notification does not reappear
                // if user hid it is because he/she does not want it. if he does just start app again
                hidNotification = true;
            } else if (intent.getStringExtra(EXTRA_COMMAND).equals(COMMAND_SEEK_UP)) {
                seekUp();
            } else if (intent.getStringExtra(EXTRA_COMMAND).equals(COMMAND_SEEK_DOWN)) {
                seekDown();
            }
        }
        //super.onNewIntent(intent);
    }
}