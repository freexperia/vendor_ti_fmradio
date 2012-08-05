/*
 * TI's FM
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 * Copyright (C) 2010, 2011 Sony Ericsson Mobile Communications AB.
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
/*******************************************************************************\
 *
 *   FILE NAME:      FmReceiver.java
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
package com.ti.fm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.content.ServiceConnection;
import android.util.Log;

/**
 * The Android FmReceiver API is not finalized, and *will* change. Use at your
 * own risk.
 * <p/>
 * Public API for controlling the FmReceiver Service. FmReceiver is a proxy
 * object for controlling the Fm Reception Service via IPC.
 * <p/>
 * Creating a FmReceiver object will create a binding with the FMRX service.
 * Users of this object should call close() when they are finished with the
 * FmReceiver, so that this proxy object can unbind from the service.
 *
 * @hide
 */
public class FmReceiver {

    private static final String TAG = "FmReceiver";

    private static final boolean DBG = true;

    private IFmReceiver mService;
    private Context mContext;
    private ServiceListener mServiceListener;

    public static final int STATE_ENABLED = 0;
    public static final int STATE_DISABLED = 1;
    public static final int STATE_ENABLING = 2;
    public static final int STATE_DISABLING = 3;
    public static final int STATE_PAUSE = 4;
    public static final int STATE_RESUME = 5;
    public static final int STATE_DEFAULT = 6;

    private static FmReceiver INSTANCE;

    /** Used when obtaining a reference to the singleton instance. */
    private static Object INSTANCE_LOCK = new Object();

    private boolean mInitialized = false;

    // Constructor
    private FmReceiver() {

    }

    private ServiceConnection mConnection = new ServiceConnection() {
       public void onServiceConnected(ComponentName className, IBinder service) {
          // This is called when the connection with the service has been
          // established, giving us the service object we can use to
          // interact with the service. We are communicating with our
          // service through an IDL interface, so get a client-side
          // representation of that from the raw service object.
          Log.i(TAG, "Service connected");
          mService = IFmReceiver.Stub.asInterface(service);
          if (mServiceListener != null) {
             Log.i(TAG, "Sending callback");
             mServiceListener.onServiceConnected();
          } else {
             Log.e(TAG, "mService is NULL");
          }
       }

       public void onServiceDisconnected(ComponentName className) {
          // This is called when the connection with the service has been
          // unexpectedly disconnected -- that is, its process crashed.
          Log.i(TAG, "Service disconnected");
          mService = null;
          if (mServiceListener != null) {
             mServiceListener.onServiceDisconnected();
          }
       }
    };

    public FmReceiver(Context context, ServiceListener listener) {
       // This will be around as long as this process is
       mContext = context.getApplicationContext();
       mServiceListener = listener;
       Log.i(TAG, "FmReceiver:Creating a FmReceiver proxy object: " + mConnection);
       mContext.bindService(new Intent("com.ti.server.FmService"), mConnection,
             Context.BIND_AUTO_CREATE);
    }

    /**
    * An interface for notifying FmReceiver IPC clients when they have been
    * connected to the FMRX service.
    */
    public interface ServiceListener {
       /**
        * Called to notify the client when this proxy object has been connected
        * to the FMRX service. Clients must wait for this callback before
        * making IPC calls on the FMRX service.
        */
       public void onServiceConnected();

       /**
        * Called to notify the client that this proxy object has been
        * disconnected from the FMRX service. Clients must not make IPC calls
        * on the FMRX service after this callback. This callback will currently
        * only occur if the application hosting the BluetoothAg service, but
        * may be called more often in future.
        */
       public void onServiceDisconnected();
    }

    protected void finalize() throws Throwable {
       if (DBG)
          Log.d(TAG, "FmReceiver:finalize");
       try {
          close();
       } finally {
          super.finalize();
       }
    }

    /**
    * Close the connection to the backing service. Other public functions of
    * BluetoothAg will return default error results once close() has been
    * called. Multiple invocations of close() are ok.
    */
    public synchronized void close() {
       Log.i(TAG, "FmReceiver:close");

       mContext.unbindService(mConnection);
    }

    /**
    * Returns true if the FM RX is enabled. Returns false if not enabled, or if
    * this proxy object if not currently connected to the FmReceiver service.
    */
    public boolean isEnabled() {
       if (DBG)
          Log.d(TAG, "FmReceiver:isEnabled");
       if (mService != null) {
          try {
             return mService.isEnabled();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    /**
    * Returns the current FM RX state
    */
    public int getFMState() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getFMState");
       if (mService != null) {
          try {
             return mService.getFMState();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return -1;
    }

    /**
    * Returns true if the FM RX is paused. Returns false if not paused, or if
    * this proxy object if not currently connected to the FmReceiver service.
    */

    public boolean isPaused() {
       if (DBG)
          Log.d(TAG, "FmReceiver:isPaused");
       if (mService != null) {
          try {
             return mService.isPaused();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    /**
    * Returns true if the FM RX is enabled. Returns false if not enabled, or if
    * this proxy object if not currently connected to the FmReceiver service.
    */
    public boolean enable() {
       if (DBG)
          Log.d(TAG, "FmReceiver:enable");
       if (mService != null) {
          try {
             return mService.enable();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    /**
    * Returns true if the EM RX is disabled. Returns false if not enabled, or
    * if this proxy object if not currently connected to the FmReceiver service.
    */
    public boolean disable() {
       if (DBG)
          Log.d(TAG, "FmReceiver:disable");
       if (mService != null) {
          try {
             return mService.disable();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean resumeFm() {
       if (DBG)
          Log.d(TAG, "FmReceiver:resumeFm");
       if (mService != null) {
          try {
             return mService.resumeFm();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }


    public boolean setBand(int band) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setBand");
       if (mService != null) {
          try {
             return mService.setBand(band);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setBand_nb(int band) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setBand_nb");
       if (mService != null) {
          try {
             return mService.setBand_nb(band);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int getBand() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getBand");
       if (mService != null) {
          try {
             return mService.getBand();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getBand_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getBand_nb");
       if (mService != null) {
          try {
             return mService.getBand_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setMonoStereoMode(int mode) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setMonoStereoMode");
       if (mService != null) {
          try {
             return mService.setMonoStereoMode(mode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setMonoStereoMode_nb(int mode) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setMonoStereoMode_nb");
       if (mService != null) {
          try {
             return mService.setMonoStereoMode_nb(mode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean isValidChannel() {
       if (DBG)
          Log.d(TAG, "FmReceiver:isValidChannel");
       if (mService != null) {
          try {
             return mService.isValidChannel();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean completeScan_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:completeScan_nb");
       if (mService != null) {
          try {
             return mService.completeScan_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean stopCompleteScan_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:stopCompleteScan_nb");
       if (mService != null) {
          try {
             return mService.stopCompleteScan_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int stopCompleteScan() {
       if (DBG)
          Log.d(TAG, "FmReceiver:stopCompleteScan");
       if (mService != null) {
          try {
             return mService.stopCompleteScan();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public double getFwVersion() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getFwVersion");
       if (mService != null) {
          try {
             return mService.getFwVersion();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public int getCompleteScanProgress() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getCompleteScanProgress");
       if (mService != null) {
          try {
             return mService.getCompleteScanProgress();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getCompleteScanProgress_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getCompleteScanProgress_nb");
       if (mService != null) {
          try {
             return mService.getCompleteScanProgress_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int getMonoStereoMode() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getMonoStereoMode");
       if (mService != null) {
          try {
             return mService.getMonoStereoMode();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getMonoStereoMode_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getMonoStereoMode_nb");
       if (mService != null) {
          try {
             return mService.getMonoStereoMode_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setMuteMode(int muteMode) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setMuteMode");
       if (mService != null) {
          try {
             return mService.setMuteMode(muteMode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setMuteMode_nb(int muteMode) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setMuteMode_nb");
       if (mService != null) {
          try {
             return mService.setMuteMode_nb(muteMode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int getMuteMode() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getMuteMode");
       if (mService != null) {
          try {
             return mService.getMuteMode();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getMuteMode_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getMuteMode_nb");
       if (mService != null) {
          try {
             return mService.getMuteMode_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setRfDependentMuteMode(int rfMuteMode) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setRfDependentMuteMode");
       if (mService != null) {
          try {
             return mService.setRfDependentMuteMode(rfMuteMode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setRfDependentMuteMode_nb(int rfMuteMode) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setRfDependentMuteMode_nb");
       if (mService != null) {
          try {
             return mService.setRfDependentMuteMode_nb(rfMuteMode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int getRfDependentMuteMode() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getRfDependentMuteMode");
       if (mService != null) {
          try {
             return mService.getRfDependentMuteMode();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getRfDependentMuteMode_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getRfDependentMuteMode_nb");
       if (mService != null) {
          try {
             return mService.getRfDependentMuteMode_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setRssiThreshold(int threshhold) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setRssiThreshold");
       if (mService != null) {
          try {
             return mService.setRssiThreshold(threshhold);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setRssiThreshold_nb(int threshhold) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setRssiThreshold_nb");
       if (mService != null) {
          try {
             return mService.setRssiThreshold_nb(threshhold);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int getRssiThreshold() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getRssiThreshold");
       if (mService != null) {
          try {
             return mService.getRssiThreshold();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getRssiThreshold_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getRssiThreshold_nb");
       if (mService != null) {
          try {
             return mService.getRssiThreshold_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setDeEmphasisFilter(int filter) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setDeEmphasisFilter");
       if (mService != null) {
          try {
             return mService.setDeEmphasisFilter(filter);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setDeEmphasisFilter_nb(int filter) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setDeEmphasisFilter_nb");
       if (mService != null) {
          try {
             return mService.setDeEmphasisFilter_nb(filter);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int getDeEmphasisFilter() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getDeEmphasisFilter");
       if (mService != null) {
          try {
             return mService.getDeEmphasisFilter();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getDeEmphasisFilter_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getDeEmphasisFilter_nb");
       if (mService != null) {
          try {
             return mService.getDeEmphasisFilter_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setVolume(int volume) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setVolume");
       if (mService != null) {
          try {
             return mService.setVolume(volume);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setChannelSpacing(int channelSpace) {

       if (DBG)
          Log.d(TAG, "FmReceiver:setChannelSpacing");
       if (mService != null) {
          try {
             return mService.setChannelSpacing(channelSpace);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setChannelSpacing_nb(int channelSpace) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setChannelSpacing_nb");
       if (mService != null) {
          try {
             return mService.setChannelSpacing_nb(channelSpace);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int getVolume() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getVolume");
       if (mService != null) {
          try {
             return mService.getVolume();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getVolume_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getVolume_nb");
       if (mService != null) {
          try {
             return mService.getVolume_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int getChannelSpacing() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getChannelSpacing");
       if (mService != null) {
          try {
             return mService.getChannelSpacing();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getChannelSpacing_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getChannelSpacing");
       if (mService != null) {
          try {
             return mService.getChannelSpacing_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean tune_nb(int freq) {
       if (DBG)
          Log.d(TAG, "FmReceiver:tune_nb");
       if (mService != null) {
          try {
             return mService.tune_nb(freq);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int getTunedFrequency() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getTunedFrequency");
       if (mService != null) {
          try {
             return mService.getTunedFrequency();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getTunedFrequency_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getTunedFrequency_nb");
       if (mService != null) {
          try {
             return mService.getTunedFrequency_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean seek_nb(int direction) {
       if (DBG)
          Log.d(TAG, "FmReceiver:seek_nb");
       if (mService != null) {
          try {
             return mService.seek_nb(direction);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean stopSeek() {
       if (DBG)
          Log.d(TAG, "FmReceiver:stopSeek");
       if (mService != null) {
          try {
             return mService.stopSeek();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean stopSeek_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:stopSeek_nb");
       if (mService != null) {
          try {
             return mService.stopSeek_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int getRdsSystem() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getRdsSystem");
       if (mService != null) {
          try {
             return mService.getRdsSystem();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getRdsSystem_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getRdsSystem_nb");
       if (mService != null) {
          try {
             return mService.getRdsSystem_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int getRssi() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getRssi");
       if (mService != null) {
          try {
             return mService.getRssi();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getRssi_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getRssi_nb");
       if (mService != null) {
          try {
             return mService.getRssi_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setRdsSystem(int system) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setRdsSystem");
       if (mService != null) {
          try {
             return mService.setRdsSystem(system);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setRdsSystem_nb(int system) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setRdsSystem_nb");
       if (mService != null) {
          try {
             return mService.setRdsSystem_nb(system);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean enableRds() {
       if (DBG)
          Log.d(TAG, "FmReceiver:enableRds");
       if (mService != null) {
          try {
             return mService.enableRds();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean disableRds() {
       if (DBG)
          Log.d(TAG, "FmReceiver:disableRds");
       if (mService != null) {
          try {
             return mService.disableRds();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean enableRds_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:enableRds_nb");
       if (mService != null) {
          try {
             return mService.enableRds_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean disableRds_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:disableRds_nb");
       if (mService != null) {
          try {
             return mService.disableRds_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setRdsGroupMask(int mask) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setRdsGroupMask");
       if (mService != null) {
          try {
             return mService.setRdsGroupMask(mask);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setRdsGroupMask_nb(int mask) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setRdsGroupMask_nb");
       if (mService != null) {
          try {
             return mService.setRdsGroupMask_nb(mask);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public long getRdsGroupMask() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getRdsGroupMask");
       if (mService != null) {
          try {
             return mService.getRdsGroupMask();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getRdsGroupMask_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getRdsGroupMask_nb");
       if (mService != null) {
          try {
             return mService.getRdsGroupMask_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setRdsAfSwitchMode(int mode) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setRdsAfSwitchMode");
       if (mService != null) {
          try {
             return mService.setRdsAfSwitchMode(mode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean setRdsAfSwitchMode_nb(int mode) {
       if (DBG)
          Log.d(TAG, "FmReceiver:setRdsAfSwitchMode_nb");
       if (mService != null) {
          try {
             return mService.setRdsAfSwitchMode_nb(mode);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public int getRdsAfSwitchMode() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getRdsAfSwitchMode");
       if (mService != null) {
          try {
             return mService.getRdsAfSwitchMode();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return 0;
    }

    public boolean getRdsAfSwitchMode_nb() {
       if (DBG)
          Log.d(TAG, "FmReceiver:getRdsAfSwitchMode_nb");
       if (mService != null) {
          try {
             return mService.getRdsAfSwitchMode_nb();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean changeAudioTarget(int mask, int digitalConfig) {
       if (DBG)
          Log.d(TAG, "FmReceiver:changeAudioTarget");
       if (mService != null) {
          try {
             return mService.changeAudioTarget(mask, digitalConfig);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean changeDigitalTargetConfiguration(int digitalConfig) {
       if (DBG)
          Log.d(TAG, "FmReceiver:changeDigitalTargetConfiguration");
       if (mService != null) {
          try {
             return mService.changeDigitalTargetConfiguration(digitalConfig);
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean enableAudioRouting() {
       if (DBG)
          Log.d(TAG, "FmReceiver:enableAudioRouting");
       if (mService != null) {
          try {
             return mService.enableAudioRouting();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

    public boolean disableAudioRouting() {
       if (DBG)
          Log.d(TAG, "FmReceiver:disableAudioRouting");
       if (mService != null) {
          try {
             return mService.disableAudioRouting();
          } catch (RemoteException e) {
             Log.e(TAG, e.toString());
          }
       } else {
          Log.w(TAG, "Proxy not attached to service");
          if (DBG)
             Log.d(TAG, Log.getStackTraceString(new Throwable()));
       }
       return false;
    }

}
