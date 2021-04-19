/*
 * Copyright (c) 2021 DuckDuckGo
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

package com.duckduckgo.app.browser.favorites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.duckduckgo.app.bookmarks.model.SavedSite
import com.duckduckgo.app.bookmarks.ui.FavoritesAdapter
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.browser.favicon.FaviconManager
import com.duckduckgo.app.browser.favorites.FavoritesQuickAccessAdapter.QuickAccessFavorite
import com.duckduckgo.app.browser.favorites.FavoritesQuickAccessAdapter.QuickAccessViewHolder
import kotlinx.android.synthetic.main.view_quick_access_item.view.*
import kotlinx.coroutines.launch

class FavoritesQuickAccessAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val faviconManager: FaviconManager
) : ListAdapter<QuickAccessFavorite, QuickAccessViewHolder>(QuickAccessAdapterDiffCallback()) {

    data class QuickAccessFavorite(val favorite: SavedSite.Favorite) : FavoritesAdapter.FavoriteItemTypes

    class QuickAccessViewHolder(
        private val layoutInflater: LayoutInflater,
        itemView: View,
        private val lifecycleOwner: LifecycleOwner,
        private val faviconManager: FaviconManager
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: QuickAccessFavorite) {
            with(item.favorite) {
                itemView.quickAccessTitle.text = title
                loadFavicon(url)
            }
        }

        private fun loadFavicon(url: String) {
            lifecycleOwner.lifecycleScope.launch {
                faviconManager.loadToViewFromPersisted(url, itemView.quickAccessFavicon)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickAccessViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.view_quick_access_item, parent, false)
        return QuickAccessViewHolder(inflater, view, lifecycleOwner, faviconManager)
    }

    override fun onBindViewHolder(holder: QuickAccessViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class QuickAccessAdapterDiffCallback : DiffUtil.ItemCallback<QuickAccessFavorite>() {
    override fun areItemsTheSame(oldItem: QuickAccessFavorite, newItem: QuickAccessFavorite): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: QuickAccessFavorite, newItem: QuickAccessFavorite): Boolean {
        return oldItem == newItem
    }
}