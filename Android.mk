#
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Modified by Sony Ericsson Mobile Communications AB

ifeq ($(BOARD_HAVE_FM_RADIO_TI),true)
  include hardware/ti/fmradio/fm_stack/Android.mk
#  include hardware/ti/fmradio/fm_app/Android.mk
  include hardware/ti/fmradio/Fmapplication/Android.mk
  include hardware/ti/fmradio/service/Android.mk
  include hardware/ti/fmradio/service/src/jni/Android.mk
  include hardware/ti/fmradio/fmreceiverif/Android.mk
endif # SEMC_CFG_FM_SERVICE_TI
