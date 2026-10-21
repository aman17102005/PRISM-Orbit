package com.prismorbit.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanyMatcherTest {

    @Test
    fun matchesTopCompaniesIgnoringCaseAndSuffixes() {
        assertTrue(CompanyMatcher.isTopCompany("Google"))
        assertTrue(CompanyMatcher.isTopCompany("GOOGLE"))
        assertTrue(CompanyMatcher.isTopCompany("google india pvt ltd"))
        assertTrue(CompanyMatcher.isTopCompany("Google LLC"))
        assertTrue(CompanyMatcher.isTopCompany("Microsoft Corporation"))
        assertTrue(CompanyMatcher.isTopCompany("Amazon Web Services"))
        assertTrue(CompanyMatcher.isTopCompany("  Amazon.com, Inc.  "))
        assertTrue(CompanyMatcher.isTopCompany("TCS"))
        assertTrue(CompanyMatcher.isTopCompany("Tata Consultancy Services Limited"))
        assertTrue(CompanyMatcher.isTopCompany("Infosys Technologies"))
        assertTrue(CompanyMatcher.isTopCompany("Goldman  Sachs"))
        assertTrue(CompanyMatcher.isTopCompany("goldmansachs"))
        assertTrue(CompanyMatcher.isTopCompany("Razorpay Software Pvt. Ltd."))
    }

    @Test
    fun doesNotMatchUnknownOrLookAlikeNames() {
        assertFalse(CompanyMatcher.isTopCompany(""))
        assertFalse(CompanyMatcher.isTopCompany("   "))
        assertFalse(CompanyMatcher.isTopCompany("Ravi Web Studio"))
        assertFalse(CompanyMatcher.isTopCompany("Pineapple Labs"))
        assertFalse(CompanyMatcher.isTopCompany("Googleplex Cafe Bangalore"))
        assertFalse(CompanyMatcher.isTopCompany("Pvt Ltd"))
    }

    @Test
    fun normalizeRemovesNoise() {
        assertEquals("google", CompanyMatcher.normalize("Google India Pvt. Ltd."))
        assertEquals("zoho", CompanyMatcher.normalize("ZOHO Corporation"))
        assertEquals("", CompanyMatcher.normalize("Pvt Ltd"))
    }

    @Test
    fun companyTypeChipScales() {
        assertEquals(0.20, CompanyMatcher.CompanyType.LOCAL.share, 0.0)
        assertEquals(0.50, CompanyMatcher.CompanyType.STARTUP.share, 0.0)
        assertEquals(0.70, CompanyMatcher.CompanyType.MEDIUM.share, 0.0)
        assertEquals(1.00, CompanyMatcher.CompanyType.BIG.share, 0.0)
        assertEquals(CompanyMatcher.CompanyType.BIG, CompanyMatcher.CompanyType.fromName("big"))
        assertEquals(null, CompanyMatcher.CompanyType.fromName("huge"))
        assertEquals(null, CompanyMatcher.CompanyType.fromName(null))
    }

    @Test
    fun usesTheHigherOfListAndChip() {
        assertEquals(1.0, CompanyMatcher.companyStrengthShare("Google", CompanyMatcher.CompanyType.LOCAL), 0.0)
        assertEquals(0.5, CompanyMatcher.companyStrengthShare("My Startup", CompanyMatcher.CompanyType.STARTUP), 0.0)
        assertEquals(1.0, CompanyMatcher.companyStrengthShare("My Startup", CompanyMatcher.CompanyType.BIG), 0.0)
        assertEquals(0.2, CompanyMatcher.companyStrengthShare("My Startup", null), 0.0)
    }

    @Test
    fun findsCompanyInsideText() {
        val text = "OFFER LETTER\nWe are pleased to offer Aman an internship at Infosys Limited starting 1 June 2026"
        assertTrue(CompanyMatcher.nameAppearsInText("Infosys", text))
        assertTrue(CompanyMatcher.nameAppearsInText("infosys pvt ltd", text))
        assertFalse(CompanyMatcher.nameAppearsInText("Wipro", text))
        assertFalse(CompanyMatcher.nameAppearsInText("", text))
        // words that run together in the scan still match
        assertTrue(CompanyMatcher.nameAppearsInText("Goldman Sachs", "Welcome to GoldmanSachs India"))
    }
}
