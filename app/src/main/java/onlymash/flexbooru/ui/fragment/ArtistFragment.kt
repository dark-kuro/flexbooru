/*
 * Copyright (C) 2019. by onlymash <im@fiepi.me>, All rights reserved
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package onlymash.flexbooru.ui.fragment

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.refreshable_list.*
import onlymash.flexbooru.Constants
import onlymash.flexbooru.R
import onlymash.flexbooru.Settings
import onlymash.flexbooru.database.UserManager
import onlymash.flexbooru.entity.Booru
import onlymash.flexbooru.entity.artist.SearchArtist
import onlymash.flexbooru.entity.User
import onlymash.flexbooru.entity.artist.ArtistBase
import onlymash.flexbooru.repository.NetworkState
import onlymash.flexbooru.repository.artist.ArtistRepositoryIml
import onlymash.flexbooru.repository.artist.ArtistRepository
import onlymash.flexbooru.ui.MainActivity
import onlymash.flexbooru.ui.SearchActivity
import onlymash.flexbooru.ui.adapter.ArtistAdapter
import onlymash.flexbooru.ui.viewholder.ArtistViewHolder
import onlymash.flexbooru.ui.viewmodel.ArtistViewModel
import onlymash.flexbooru.widget.search.SearchBar

class ArtistFragment : ListFragment() {

    companion object {
        private const val TAG = "ArtistFragment"

        private const val ORDER_DEFAULT = ""
        private const val ORDER_DATE = "date"
        private const val ORDER_UPDATED_AT = "updated_at"
        private const val ORDER_NAME = "name"
        private const val ORDER_COUNT = "post_count"

        @JvmStatic
        fun newInstance(booru: Booru, user: User?) =
            ArtistFragment().apply {
                arguments = when (booru.type) {
                    Constants.TYPE_DANBOORU -> Bundle().apply {
                        putString(Constants.SCHEME_KEY, booru.scheme)
                        putString(Constants.HOST_KEY, booru.host)
                        putInt(Constants.TYPE_KEY, Constants.TYPE_DANBOORU)
                        if (user != null) {
                            putString(Constants.USERNAME_KEY, user.name)
                            putString(Constants.AUTH_KEY, user.api_key)
                        } else {
                            putString(Constants.USERNAME_KEY, "")
                            putString(Constants.AUTH_KEY, "")
                        }
                    }
                    Constants.TYPE_MOEBOORU -> Bundle().apply {
                        putString(Constants.SCHEME_KEY, booru.scheme)
                        putString(Constants.HOST_KEY, booru.host)
                        putInt(Constants.TYPE_KEY, Constants.TYPE_MOEBOORU)
                        if (user != null) {
                            putString(Constants.USERNAME_KEY, user.name)
                            putString(Constants.AUTH_KEY, user.password_hash)
                        } else {
                            putString(Constants.USERNAME_KEY, "")
                            putString(Constants.AUTH_KEY, "")
                        }
                    }
                    Constants.TYPE_DANBOORU_ONE -> Bundle().apply {
                        putString(Constants.SCHEME_KEY, booru.scheme)
                        putString(Constants.HOST_KEY, booru.host)
                        putInt(Constants.TYPE_KEY, Constants.TYPE_DANBOORU_ONE)
                        if (user != null) {
                            putString(Constants.USERNAME_KEY, user.name)
                            putString(Constants.AUTH_KEY, user.password_hash)
                        } else {
                            putString(Constants.USERNAME_KEY, "")
                            putString(Constants.AUTH_KEY, "")
                        }
                    }
                    else -> Bundle().apply {
                        putInt(Constants.TYPE_KEY, Constants.TYPE_UNKNOWN)
                    }
                }
            }
    }

    private var type = -1
    private lateinit var search: SearchArtist

    override val stateChangeListener: SearchBar.StateChangeListener
        get() = object : SearchBar.StateChangeListener {
            override fun onStateChange(newState: Int, oldState: Int, animation: Boolean) {
                toggleArrowLeftDrawable()
            }
        }

    override val searchBarHelper: SearchBarHelper
        get() = object : SearchBarHelper {
            override fun onMenuItemClick(menuItem: MenuItem) {
                when (menuItem.itemId) {
                    R.id.action_artist_order_default -> {
                        search.order = ORDER_DEFAULT
                        refresh()
                    }
                    R.id.action_artist_order_date -> {
                        when (type) {
                            Constants.TYPE_DANBOORU -> search.order = ORDER_UPDATED_AT
                            Constants.TYPE_MOEBOORU,
                            Constants.TYPE_DANBOORU_ONE -> search.order = ORDER_DATE
                        }
                        refresh()
                    }
                    R.id.action_artist_order_name -> {
                        search.order = ORDER_NAME
                        refresh()
                    }
                    R.id.action_artist_order_count -> {
                        search.order = ORDER_COUNT
                        refresh()
                    }
                }
            }

            override fun onApplySearch(query: String) {
                if (type < 0) return
                search.name = query
                refresh()
            }
        }

    private fun refresh() {
        when (type) {
            Constants.TYPE_DANBOORU -> {
                swipe_refresh.isRefreshing = true
                artistViewModel.show(search)
                artistViewModel.refreshDan()
            }
            Constants.TYPE_MOEBOORU -> {
                swipe_refresh.isRefreshing = true
                artistViewModel.show(search)
                artistViewModel.refreshMoe()
            }
            Constants.TYPE_DANBOORU_ONE -> {
                swipe_refresh.isRefreshing = true
                artistViewModel.show(search)
                artistViewModel.refreshDanOne()
            }
        }
    }

    private lateinit var artistViewModel: ArtistViewModel
    private lateinit var artistAdapter: ArtistAdapter

    private val itemListener = object : ArtistViewHolder.ItemListener {
        override fun onClickItem(keyword: String) {
            SearchActivity.startActivity(requireContext(), keyword)
        }
    }

    private val userListener = object : UserManager.Listener {
        override fun onAdd(user: User) {
            updateUserInfoAndRefresh(user)
        }
        override fun onDelete(user: User) {
            if (user.booru_uid != Settings.activeBooruUid) return
            search.username = ""
            search.auth_key = ""
            when (type) {
                Constants.TYPE_DANBOORU -> {
                    artistViewModel.apply {
                        show(search)
                        refreshDan()
                    }
                }
                Constants.TYPE_MOEBOORU -> {
                    artistViewModel.apply {
                        show(search)
                        refreshMoe()
                    }
                }
                Constants.TYPE_DANBOORU_ONE -> {
                    artistViewModel.apply {
                        show(search)
                        refreshDanOne()
                    }
                }
            }
        }
        override fun onUpdate(user: User) {
            updateUserInfoAndRefresh(user)
        }
    }

    private fun updateUserInfoAndRefresh(user: User) {
        when (type) {
            Constants.TYPE_DANBOORU -> {
                search.username = user.name
                search.auth_key = user.api_key ?: ""
                artistViewModel.apply {
                    show(search)
                    refreshDan()
                }
            }
            Constants.TYPE_MOEBOORU -> {
                search.username = user.name
                search.auth_key = user.password_hash ?: ""
                artistViewModel.apply {
                    show(search)
                    refreshMoe()
                }
            }
            Constants.TYPE_DANBOORU_ONE -> {
                search.username = user.name
                search.auth_key = user.password_hash ?: ""
                artistViewModel.apply {
                    show(search)
                    refreshDanOne()
                }
            }
        }
    }

    private val navigationListener = object : MainActivity.NavigationListener {
        override fun onClickPosition(position: Int) {
            if (position == 4) {
                list.smoothScrollToPosition(0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arg = arguments ?: throw RuntimeException("arg is null")
        type = arg.getInt(Constants.TYPE_KEY, Constants.TYPE_UNKNOWN)
        if (type < 0) return
        search = SearchArtist(
            scheme = arg.getString(Constants.SCHEME_KEY, ""),
            host = arg.getString(Constants.HOST_KEY, ""),
            name = "",
            order = ORDER_DEFAULT,
            username = arg.getString(Constants.USERNAME_KEY, ""),
            auth_key = arg.getString(Constants.AUTH_KEY, ""),
            limit = Settings.pageLimit
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchBar.setTitle(R.string.title_artists)
        searchBar.setEditTextHint(getString(R.string.search_bar_hint_search_artists))
        if (type < 0) {
            list.visibility = View.GONE
            swipe_refresh.visibility = View.GONE
            notSupported.visibility = View.VISIBLE
            return
        }
        artistViewModel = getArtistViewModel(
            ArtistRepositoryIml(
                danbooruApi = danApi,
                danbooruOneApi = danOneApi,
                moebooruApi = moeApi,
                networkExecutor = ioExecutor
            )
        )
        artistAdapter = ArtistAdapter(
            listener = itemListener,
            retryCallback = {
                when (type) {
                    Constants.TYPE_DANBOORU -> artistViewModel.retryDan()
                    Constants.TYPE_MOEBOORU -> artistViewModel.retryMoe()
                    Constants.TYPE_DANBOORU_ONE -> artistViewModel.retryDanOne()
                }
            }
        )
        list.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = artistAdapter
        }
        when (type) {
            Constants.TYPE_DANBOORU -> {
                searchBar.setMenu(R.menu.artist_dan, requireActivity().menuInflater)
                artistViewModel.artistsDan.observe(this, Observer { artists ->
                    @Suppress("UNCHECKED_CAST")
                    artistAdapter.submitList(artists as PagedList<ArtistBase>)
                })
                artistViewModel.networkStateDan.observe(this, Observer { networkState ->
                    artistAdapter.setNetworkState(networkState)
                })
                initSwipeToRefreshDan()
            }
            Constants.TYPE_MOEBOORU -> {
                searchBar.setMenu(R.menu.artist_moe, requireActivity().menuInflater)
                artistViewModel.artistsMoe.observe(this, Observer { artists ->
                    @Suppress("UNCHECKED_CAST")
                    artistAdapter.submitList(artists as PagedList<ArtistBase>)
                })
                artistViewModel.networkStateMoe.observe(this, Observer { networkState ->
                    artistAdapter.setNetworkState(networkState)
                })
                initSwipeToRefreshMoe()
            }
            Constants.TYPE_DANBOORU_ONE -> {
                searchBar.setMenu(R.menu.artist_dan, requireActivity().menuInflater)
                artistViewModel.artistsDanOne.observe(this, Observer { artists ->
                    @Suppress("UNCHECKED_CAST")
                    artistAdapter.submitList(artists as PagedList<ArtistBase>)
                })
                artistViewModel.networkStateDanOne.observe(this, Observer { networkState ->
                    artistAdapter.setNetworkState(networkState)
                })
                initSwipeToRefreshDanOne()
            }
        }
        artistViewModel.show(search = search)
        UserManager.listeners.add(userListener)
        (requireActivity() as MainActivity).addNavigationListener(navigationListener)
    }

    private fun initSwipeToRefreshDan() {
        artistViewModel.refreshStateDan.observe(this, Observer<NetworkState> {
            if (it != NetworkState.LOADING) {
                swipe_refresh.isRefreshing = false
            }
        })
        swipe_refresh.setOnRefreshListener { artistViewModel.refreshDan() }
    }

    private fun initSwipeToRefreshDanOne() {
        artistViewModel.refreshStateDanOne.observe(this, Observer<NetworkState> {
            if (it != NetworkState.LOADING) {
                swipe_refresh.isRefreshing = false
            }
        })
        swipe_refresh.setOnRefreshListener { artistViewModel.refreshDanOne() }
    }

    private fun initSwipeToRefreshMoe() {
        artistViewModel.refreshStateMoe.observe(this, Observer<NetworkState> {
            if (it != NetworkState.LOADING) {
                swipe_refresh.isRefreshing = false
            }
        })
        swipe_refresh.setOnRefreshListener { artistViewModel.refreshMoe() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getArtistViewModel(repo: ArtistRepository): ArtistViewModel {
        return ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return ArtistViewModel(repo) as T
            }
        })[ArtistViewModel::class.java]
    }

    override fun onDestroy() {
        super.onDestroy()
        if (type < 0) return
        UserManager.listeners.remove(userListener)
        (requireActivity() as MainActivity).removeNavigationListener(navigationListener)
    }
}