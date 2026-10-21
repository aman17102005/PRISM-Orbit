package com.prismorbit.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProofTextCheckerTest {

    private val offerLetter = """
        INFOSYS LIMITED
        OFFER LETTER
        Date: 12/05/2026
        Dear Aman Kapuria,
        We are pleased to offer you an internship at Infosys Limited as a Software Intern.
        Your joining date is 1 June 2026 and the monthly stipend will be paid every month.
    """.trimIndent()

    @Test
    fun goodOfferLetterIsOk() {
        val result = ProofTextChecker.check(offerLetter, "Infosys", "Aman Kapuria")
        assertEquals(ProofTextChecker.OK, result.status)
        assertTrue(result.companyMatch)
        assertTrue(result.nameMatch)
        assertTrue(result.keywordHits.isNotEmpty())
        assertTrue(result.datesFound >= 2)
    }

    @Test
    fun wrongCompanyIsPartial() {
        val result = ProofTextChecker.check(offerLetter, "Wipro", "Aman Kapuria")
        assertEquals(ProofTextChecker.PARTIAL, result.status)
        assertFalse(result.companyMatch)
        assertTrue(result.nameMatch)
    }

    @Test
    fun missingProfileNameStillOkWithCompanyKeywordsAndDates() {
        val result = ProofTextChecker.check(offerLetter, "Infosys", "")
        assertFalse(result.nameMatch)
        assertEquals(ProofTextChecker.OK, result.status)
    }

    @Test
    fun companyOnlyIsPartial() {
        val result = ProofTextChecker.check("Welcome to Infosys. Our office is on the third floor near the main road in the city.", "Infosys", "Someone Else")
        assertEquals(ProofTextChecker.PARTIAL, result.status)
        assertTrue(result.companyMatch)
    }

    @Test
    fun tooLittleTextIsUnreadable() {
        assertEquals(ProofTextChecker.UNREADABLE, ProofTextChecker.check("", "Infosys", "Aman").status)
        assertEquals(ProofTextChecker.UNREADABLE, ProofTextChecker.check("12/05/2026", "Infosys", "Aman").status)
        assertEquals(ProofTextChecker.UNREADABLE, ProofTextChecker.check("a b c ..", "Infosys", "Aman").status)
        assertEquals(ProofTextChecker.UNREADABLE, ProofTextChecker.check("Infosys offer", "Infosys", "Aman").status)
    }

    @Test
    fun certificateWordsAreFound() {
        val text = "This is to certify that Aman Kapuria has successfully completed the internship program with Zoho Corporation. Certificate of completion issued on 20-08-2026."
        val result = ProofTextChecker.check(text, "Zoho", "Aman Kapuria")
        assertTrue("certificate" in result.keywordHits)
        assertTrue("completion" in result.keywordHits)
        assertEquals(ProofTextChecker.OK, result.status)
    }

    @Test
    fun nameMatching() {
        assertTrue(ProofTextChecker.nameAppearsInText("Aman Kapuria", "dear aman kapuria,"))
        assertTrue(ProofTextChecker.nameAppearsInText("Aman Kapuria", "kapuria, aman"))
        assertFalse(ProofTextChecker.nameAppearsInText("Aman Kapuria", "dear aman"))
        assertFalse(ProofTextChecker.nameAppearsInText("", "dear aman"))
        assertFalse(ProofTextChecker.nameAppearsInText("Al", "al was here"))
        assertTrue(ProofTextChecker.nameAppearsInText("Priya", "to priya sharma"))
    }

    @Test
    fun datesInManyFormats() {
        assertEquals(1, ProofTextChecker.countDates("on 12/05/2026 we met"))
        assertEquals(1, ProofTextChecker.countDates("on 12-05-26"))
        assertEquals(1, ProofTextChecker.countDates("on 2026-05-12"))
        assertEquals(1, ProofTextChecker.countDates("starting 1 June 2026"))
        assertEquals(1, ProofTextChecker.countDates("starting 1st Jun, 2026"))
        assertEquals(1, ProofTextChecker.countDates("issued on June 1, 2026"))
        assertEquals(2, ProofTextChecker.countDates("12/05/2026 and 1 June 2026"))
        assertEquals(0, ProofTextChecker.countDates("no dates here, only 2026"))
    }

    @Test
    fun describesResultInSimpleWords() {
        val ok = ProofTextChecker.check(offerLetter, "Infosys", "Aman Kapuria").toSummary()
        val text = ProofTextChecker.describe(ok)
        assertTrue(text.contains("Company matched"))
        assertTrue(text.contains("your name matched"))
        assertTrue(text.contains("dates found"))
        assertEquals("No proof added.", ProofTextChecker.describe(ProofSummary()))
        assertTrue(ProofTextChecker.describe(ProofSummary(status = "UNREADABLE")).contains("could not read"))
    }

    @Test
    fun storedMapKeepsOnlyTheSmallResult() {
        val result = ProofTextChecker.check(offerLetter, "Infosys", "Aman Kapuria")
        val map = ProofStorage.toMap(result, "AAAA", "Infosys", now = 5L)
        assertEquals(setOf("status", "companyMatch", "nameMatch", "keywordHits", "datesFound", "checkedAt", "checkedCompany", "thumbnail"), map.keys)
        assertFalse(map.values.any { it is String && it.contains("pleased to offer") })

        // a thumbnail above the limit is dropped, so the document stays small
        val tooBig = "A".repeat(ProofReader.MAX_THUMBNAIL_BASE64_CHARS + 1)
        assertFalse("thumbnail" in ProofStorage.toMap(result, tooBig, "Infosys"))
        assertFalse("thumbnail" in ProofStorage.toMap(result, null, "Infosys"))

        val back = ProofStorage.summaryOf(map)
        assertEquals(result.status, back.status)
        assertEquals(result.companyMatch, back.companyMatch)
        assertEquals(result.datesFound, back.datesFound)
        assertFalse(ProofStorage.summaryOf(null).isPresent)
        assertFalse(ProofStorage.summaryOf(emptyMap()).isPresent)
    }
}
