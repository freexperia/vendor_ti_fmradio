/*
 * TI's FM
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
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
*   FILE NAME:      IFmReceiver.aidl
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

/**
 * System private API for FM Receiver service
 *
 * {@hide}
 */
interface IFmReceiver {
    boolean enable();
    boolean disable();
    boolean isEnabled() ;
    boolean isFMPaused();
    int getFMState();
    boolean resumeFm();
    boolean setBand(int band);
    boolean setBand_nb(int band);
    int getBand();
    boolean getBand_nb();
    boolean setChannelSpacing(int channelSpace);
    boolean setChannelSpacing_nb(int channelSpace);
    int getChannelSpacing();
    boolean getChannelSpacing_nb();
    boolean setMonoStereoMode(int mode);
    boolean setMonoStereoMode_nb(int mode);
    int getMonoStereoMode();
    boolean  getMonoStereoMode_nb();
    boolean setMuteMode(int muteMode);
    boolean setMuteMode_nb(int muteMode);
    int getMuteMode();
     boolean getMuteMode_nb();
    boolean setRfDependentMuteMode(int rfMuteMode);
    boolean setRfDependentMuteMode_nb(int rfMuteMode);
    int getRfDependentMuteMode();
    boolean getRfDependentMuteMode_nb();
    boolean setRssiThreshold(int threshhold);
    boolean setRssiThreshold_nb(int threshhold);
    int getRssiThreshold();
    boolean getRssiThreshold_nb();
    boolean setDeEmphasisFilter(int filter);
    boolean setDeEmphasisFilter_nb(int filter);
    int getDeEmphasisFilter();
     boolean getDeEmphasisFilter_nb();
    boolean setVolume(int volume);
    int getVolume();
     boolean getVolume_nb();
    boolean tune_nb(int freq);
    int getTunedFrequency();
    boolean getTunedFrequency_nb();
    boolean seek_nb(int direction);
    boolean stopSeek();
    boolean stopSeek_nb();
    int getRssi();
    boolean getRssi_nb();
    int getRdsSystem();
    boolean getRdsSystem_nb();
    boolean setRdsSystem(int system);
    boolean setRdsSystem_nb(int system);
    boolean enableRds();
    boolean enableRds_nb();
    boolean disableRds();
    boolean disableRds_nb();
    boolean setRdsGroupMask(int mask);
    boolean setRdsGroupMask_nb(int mask);
    long getRdsGroupMask();
    boolean getRdsGroupMask_nb();
    boolean setRdsAfSwitchMode(int mode);
    boolean setRdsAfSwitchMode_nb(int mode);
    int getRdsAfSwitchMode();
    boolean getRdsAfSwitchMode_nb();
    boolean changeAudioTarget(int mask,int digitalConfig);
    boolean changeDigitalTargetConfiguration(int digitalConfig);
    boolean enableAudioRouting();
    boolean disableAudioRouting();
    boolean isValidChannel();
    boolean completeScan_nb();
    double getFwVersion();
    int getCompleteScanProgress();
    boolean getCompleteScanProgress_nb();
    int stopCompleteScan();

}
