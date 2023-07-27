package com.duckduckgo.autofill.sharedcreds

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.CoroutineTestRule
import com.duckduckgo.autofill.api.TestUrlUnicodeNormalizer
import com.duckduckgo.autofill.api.urlmatcher.AutofillUrlMatcher.ExtractedUrlParts
import com.duckduckgo.autofill.impl.sharedcreds.RealShareableCredentialsUrlGenerator
import com.duckduckgo.autofill.impl.sharedcreds.SharedCredentialsParser
import com.duckduckgo.autofill.impl.sharedcreds.SharedCredentialsParser.OmnidirectionalRule
import com.duckduckgo.autofill.impl.sharedcreds.SharedCredentialsParser.SharedCredentialConfig
import com.duckduckgo.autofill.impl.sharedcreds.SharedCredentialsParser.UnidirectionalRule
import com.duckduckgo.autofill.impl.urlmatcher.AutofillDomainNameUrlMatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class RealShareableCredentialsUrlGeneratorTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val jsonParser: SharedCredentialsParser = mock()

    private val testee = RealShareableCredentialsUrlGenerator(
        autofillUrlMatcher = AutofillDomainNameUrlMatcher(TestUrlUnicodeNormalizer()),
    )

    @Before
    fun before() {
        runTest {
            whenever(jsonParser.read()).thenReturn(SharedCredentialConfig(omnidirectionalRules = emptyList(), unidirectionalRules = emptyList()))
        }
    }

    @Test
    fun whenSiteInOmnidirectionalListThenAllRelatedSitesReturned() = runTest {
        val config = config(
            omnidirectionalRules = listOf(
                omnidirectionalRule(listOf("foo.com", "example.com", "bar.com")),
                omnidirectionalRule(listOf("unrelated.com")),
            ),
        )

        val result = testee.generateShareableUrls("example.com", config)

        result.assertMatches(
            listOf(
                "foo.com",
                "bar.com",
            ),
        )
    }

    @Test
    fun whenSiteInOmnidirectionalListMultipleTimesThenOnlyReturnedOnce() = runTest {
        val config = config(
            omnidirectionalRules = listOf(
                omnidirectionalRule(listOf("foo.com", "example.com", "bar.com")),
                omnidirectionalRule(listOf("foo.com", "example.com", "bar.com")),
            ),
        )

        val result = testee.generateShareableUrls("example.com", config)

        result.assertMatches(
            listOf(
                "foo.com",
                "bar.com",
            ),
        )
    }

    @Test
    fun whenSiteInInToUnidirectionalListThenNotReturned() = runTest {
        val config = config(
            unidirectionalRules = listOf(
                unidirectionalRule(
                    from = listOf("unrelated.com"),
                    to = listOf("example.com"),
                ),
            ),
        )

        val result = testee.generateShareableUrls("example.com", config)

        result.assertMatches(emptyList())
    }

    @Test
    fun whenSiteInFromUnidirectionalListThenReturned() = runTest {
        val config = config(
            unidirectionalRules = listOf(
                unidirectionalRule(
                    from = listOf("example.com"),
                    to = listOf("expected.com"),
                ),
            ),
        )
        val result = testee.generateShareableUrls("example.com", config)

        result.assertMatches(listOf("expected.com"))
    }

    @Test
    fun whenSiteInMultipleUnidirectionalListThenReturnedOnce() = runTest {
        val config = config(
            unidirectionalRules = listOf(
                unidirectionalRule(
                    from = listOf("example.com"),
                    to = listOf("expected.com"),
                ),
                unidirectionalRule(
                    from = listOf("example.com"),
                    to = listOf("expected.com"),
                ),
            ),
        )
        val result = testee.generateShareableUrls("example.com", config)

        result.assertMatches(listOf("expected.com"))
    }

    @Test
    fun whenToUnidirectionalListHasMultipleSitesThenAllReturned() = runTest {
        val config = config(
            unidirectionalRules = listOf(
                unidirectionalRule(
                    from = listOf("unrelated.com"),
                    to = listOf("not-expected.com"),
                ),
                unidirectionalRule(
                    from = listOf("example.com"),
                    to = listOf("expected.com", "also-expected.com", "and-another.com"),
                ),
                unidirectionalRule(
                    from = listOf("example.com"),
                    to = listOf("expected-from-another-list.com"),
                ),
            ),
        )
        val result = testee.generateShareableUrls("example.com", config)

        result.assertMatches(
            listOf(
                "expected-from-another-list.com",
                "expected.com",
                "and-another.com",
                "also-expected.com",
            ),
        )
    }

    @Test
    fun whenMatchesFromOmnidirectionalAndUnidirectionalThenReturnedOnce() = runTest {
        val config = config(
            omnidirectionalRules = listOf(
                omnidirectionalRule(listOf("example.com", "expected.com")),
            ),
            unidirectionalRules = listOf(
                unidirectionalRule(
                    from = listOf("example.com"),
                    to = listOf("expected.com"),
                ),
            ),
        )
        val result = testee.generateShareableUrls("example.com", config)
        result.assertMatches(listOf("expected.com"))
    }

    @Test
    fun whenFullUncleanedUrlGivenThenStillMatches() = runTest {
        val config = config(omnidirectionalRules = listOf(omnidirectionalRule(listOf("example.com", "expected.com"))))
        val result = testee.generateShareableUrls("https://example.com/hello/world", config)
        result.assertMatches(listOf("expected.com"))
    }

    @Test
    fun whenConfigIsEmptyThenNoSitesReturned() = runTest {
        val config = emptyLists()
        val result = testee.generateShareableUrls("example.com", config)
        result.assertMatches(emptyList())
    }

    // @Test
    // fun whenSitesAreNotAssociatedInAnyDirectionThenNotShareable() = runTest {
    //     val sourceSite = urlParts("example.com")
    //     val targetSite = urlParts("fill.dev")
    //     assertFalse(testee.isShareable(sourceSite, targetSite))
    // }
    //
    // @Test
    // fun whenMatchingEtldPlusOneThenShareableEvenWhenConfigEmpty() = runTest {
    //     emptyLists().use()
    //     val sourceSite = urlParts("example.com")
    //     val targetSite = urlParts("example.com")
    //     assertTrue(testee.isShareable(sourceSite, targetSite))
    // }
    //
    // @Test
    // fun whenSourceSiteAppearsInOmnidirectionalListButTargetDoesNotThenNotShareable() = runTest {
    //     config(
    //         omnidirectionalRules = listOf(
    //             omnidirectionalRule(listOf("example.com")),
    //         ),
    //     ).use()
    //     val sourceSite = urlParts("example.com")
    //     val targetSite = urlParts("foo.com")
    //     assertFalse(testee.isShareable(sourceSite, targetSite))
    // }
    //
    // @Test
    // fun whenSourceSiteAppearsInOmnidirectionalListAndTargetAppearsInAnotherListThenNotShareable() = runTest {
    //     config(
    //         omnidirectionalRules = listOf(
    //             // source and target appear in different lists
    //             omnidirectionalRule(listOf("example.com")),
    //             omnidirectionalRule(listOf("foo.com")),
    //         ),
    //     ).use()
    //     val sourceSite = urlParts("example.com")
    //     val targetSite = urlParts("foo.com")
    //     assertFalse(testee.isShareable(sourceSite, targetSite))
    // }
    //
    // @Test
    // fun whenSourceSiteAppearsInSameOmnidirectionalListAsTargetThenIsShareable() = runTest {
    //     config(
    //         omnidirectionalRules = listOf(
    //             // source and target appear in same list
    //             OmnidirectionalRule(
    //                 listOf(
    //                     "example.com",
    //                     "foo.com",
    //                 ),
    //             ),
    //         ),
    //     ).use()
    //     val sourceSite = urlParts("example.com")
    //     val targetSite = urlParts("foo.com")
    //     assertTrue(testee.isShareable(sourceSite, targetSite))
    // }
    //
    // @Test
    // fun whenSourceSiteAppearsInUnidirectionalListAndTargetIsValidThenShareable() = runTest {
    //     config(
    //         unidirectionalRules = listOf(
    //             UnidirectionalRule(
    //                 from = listOf("example.com"),
    //                 to = listOf("foo.com"),
    //                 fromDomainsAreObsoleted = false,
    //             ),
    //         ),
    //     ).use()
    //     val sourceSite = urlParts("example.com")
    //     val targetSite = urlParts("foo.com")
    //     assertTrue(testee.isShareable(sourceSite, targetSite))
    // }
    //
    // @Test
    // fun whenSitesAreInOppositeDirectionToWhatIsAllowedThenNotShareable() = runTest {
    //     config(
    //         unidirectionalRules = listOf(
    //             UnidirectionalRule(
    //                 from = listOf("foo.com"),
    //                 to = listOf("example.com"),
    //                 fromDomainsAreObsoleted = false,
    //             ),
    //         ),
    //     ).use()
    //     val sourceSite = urlParts("example.com")
    //     val targetSite = urlParts("foo.com")
    //     assertFalse(testee.isShareable(sourceSite, targetSite))
    // }
    //
    // @Test
    // fun whenSourceSiteIsInUnidirectionListButLinkedListIsEmptyThenNotSharable() = runTest {
    //     config(
    //         unidirectionalRules = listOf(
    //             UnidirectionalRule(
    //                 from = listOf("example.com"),
    //                 to = emptyList(),
    //                 fromDomainsAreObsoleted = false,
    //             ),
    //         ),
    //     ).use()
    //     val sourceSite = urlParts("example.com")
    //     val targetSite = urlParts("foo.com")
    //     assertFalse(testee.isShareable(sourceSite, targetSite))
    // }
    //
    // @Test
    // fun whenSourceSiteAppearsInUnidirectionalListButTargetIsNotLinkedThenNotShareable() = runTest {
    //     config(
    //         unidirectionalRules = listOf(
    //             UnidirectionalRule(
    //                 from = listOf("example.com"),
    //                 to = listOf("a.com", "b.com", "c.com", "d.com"),
    //                 fromDomainsAreObsoleted = false,
    //             ),
    //         ),
    //     ).use()
    //     val sourceSite = urlParts("example.com")
    //     val targetSite = urlParts("foo.com")
    //     assertFalse(testee.isShareable(sourceSite, targetSite))
    // }
    //
    // private fun urlParts(
    //     eTldPlus1: String,
    //     subdomain: String? = null,
    //     port: Int? = null
    // ): ExtractedUrlParts {
    //     return ExtractedUrlParts(
    //         eTldPlus1 = eTldPlus1,
    //         userFacingETldPlus1 = eTldPlus1,
    //         subdomain = subdomain,
    //         port = port,
    //     )
    // }

    private suspend fun SharedCredentialConfig.use() {
        whenever(jsonParser.read()).thenReturn(this)
    }

    private fun emptyLists(): SharedCredentialConfig {
        return config()
    }

    private fun config(
        omnidirectionalRules: List<OmnidirectionalRule> = emptyList(),
        unidirectionalRules: List<UnidirectionalRule> = emptyList(),
    ): SharedCredentialConfig {
        return SharedCredentialConfig(
            omnidirectionalRules = omnidirectionalRules,
            unidirectionalRules = unidirectionalRules,
        )
    }

    private fun omnidirectionalRule(
        shared: List<String>,
    ): OmnidirectionalRule {
        return OmnidirectionalRule(
            shared = shared.toUrlParts(),
        )
    }

    private fun unidirectionalRule(
        from: List<String>,
        to: List<String>,
        fromDomainsAreObsoleted: Boolean? = null,
    ): UnidirectionalRule {
        return UnidirectionalRule(
            from = from.toUrlParts(),
            to = to.toUrlParts(),
            fromDomainsAreObsoleted = fromDomainsAreObsoleted,
        )
    }
}

private fun List<String>.toUrlParts(): List<ExtractedUrlParts> {
    return this.map { ExtractedUrlParts(it, it, null, 443) }
}

private fun List<ExtractedUrlParts>.assertMatches(expected: List<String>) {
    assertEquals("Lists are different sizes", expected.size, this.size)
    assertEquals(expected.toUrlParts().toHashSet(), this.toHashSet())
}
