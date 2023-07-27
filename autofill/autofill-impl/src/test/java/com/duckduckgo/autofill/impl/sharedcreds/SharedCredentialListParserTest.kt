/*
 * Copyright (c) 2023 DuckDuckGo
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

package com.duckduckgo.autofill.impl.sharedcreds

import com.duckduckgo.app.FileUtilities
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedCredentialListParserTest {

    private val moshi = Moshi.Builder().build()
    private val json = "apple_shared_credentials".loadJsonFile()

    data class Rule(
        val shared: List<String>?,
        val from: List<String>?,
        val to: List<String>?,
        val fromDomainsAreObsoleted: Boolean?,
    )

    data class OmnidirectionalRule(val shared: List<String>)
    data class UnidirectionalRule(val from: List<String>, val to: List<String>, val fromDomainsAreObsoleted: Boolean?)

    @Test
    fun whenasdThen() {
        val adapter = moshi.adapter<List<Rule>>(Types.newParameterizedType(List::class.java, Rule::class.java))
        val rules = adapter.fromJson(json)

        if (rules != null) {
            val omnidirectionalRules = mutableListOf<OmnidirectionalRule>()
            val unidirectionalRules = mutableListOf<UnidirectionalRule>()

            rules.forEach { rule ->
                if (rule.shared != null) {
                    omnidirectionalRules.add(OmnidirectionalRule(rule.shared))
                } else if (rule.from != null && rule.to != null) {
                    unidirectionalRules.add(UnidirectionalRule(rule.from, rule.to, rule.fromDomainsAreObsoleted))
                } else {
                    // not a rule we understand
                }
            }

            assertEquals(35, rules.size)
            assertEquals(21, omnidirectionalRules.size)
            assertEquals(14, unidirectionalRules.size)
        }
    }

    private fun String.loadJsonFile(): String {
        return FileUtilities.loadText(
            SharedCredentialListParserTest::class.java.classLoader!!,
            "json/$this.json",
        )
    }
}
