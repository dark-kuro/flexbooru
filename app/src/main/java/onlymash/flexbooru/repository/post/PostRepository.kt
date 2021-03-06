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

package onlymash.flexbooru.repository.post

import onlymash.flexbooru.entity.Search
import onlymash.flexbooru.entity.TagBlacklist
import onlymash.flexbooru.entity.post.*
import onlymash.flexbooru.repository.Listing

interface PostRepository {

    fun getDanOnePosts(
        search: Search,
        tagBlacklists: MutableList<TagBlacklist>
    ): Listing<PostDanOne>

    fun getDanPosts(
        search: Search,
        tagBlacklists: MutableList<TagBlacklist>
    ): Listing<PostDan>

    fun getMoePosts(
        search: Search,
        tagBlacklists: MutableList<TagBlacklist>
    ): Listing<PostMoe>

    fun getGelPosts(
        search: Search,
        tagBlacklists: MutableList<TagBlacklist>
    ): Listing<PostGel>

    fun getSankakuPosts(
        search: Search,
        tagBlacklists: MutableList<TagBlacklist>
    ): Listing<PostSankaku>
}