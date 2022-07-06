/*
 * Copyright (c) 2022 DuckDuckGo
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

package com.duckduckgo.app.browser.defaultbrowsing

import android.webkit.WebView
import com.duckduckgo.di.scopes.AppScope
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject

interface ViewportMod {
    fun onPageStarted(
        webView: WebView,
        url: String?,
        desktopMode: Boolean
    )
}

@ContributesBinding(AppScope::class)
class DesktopViewportMod @Inject constructor() : ViewportMod {
    override fun onPageStarted(
        webView: WebView,
        url: String?,
        desktopMode: Boolean
    ) {
        if (desktopMode) {
            webView.loadUrl(
                "javascript:document.getElementsByName('viewport')[0].setAttribute('content', " +
                    "'width=980px');"
            )
        }
    }
}
