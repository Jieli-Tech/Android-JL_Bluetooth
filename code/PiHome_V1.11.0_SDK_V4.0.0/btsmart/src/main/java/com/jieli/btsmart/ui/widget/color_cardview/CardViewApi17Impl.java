/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jieli.btsmart.ui.widget.color_cardview;


import androidx.annotation.RequiresApi;

@RequiresApi(17)
class CardViewApi17Impl extends CardViewBaseImpl {

    @Override
    public void initStatic() {
        RoundRectDrawableWithShadow.sRoundRectHelper =
                (canvas, bounds, cornerRadius, paint) -> canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint);
    }
}
