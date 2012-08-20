/*
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
 *
 */
package com.ti.fmapp.logic;

public class PreSetRadio {

    private long uid;
    private String stationFrequency = "", stationName = "";

    public PreSetRadio() {
        stationFrequency = "";
        stationName = "";
    }

    public PreSetRadio(String stationFrequency, String stationName) {
        this.stationFrequency = stationFrequency;
        this.stationName = stationName;
    }

    public String getStationFrequency() {
        return stationFrequency;
    }

    public void setStationFrequency(String stationFrequency) {
        this.stationFrequency = stationFrequency;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    /**
     * @return True if this station is set, False otherwise
     */
    public boolean isStationSet() {
        return stationFrequency.length() > 1;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }
}
