package com.prismorbit.app

// ============================================================
// COMPANY MATCHER
// ============================================================
// A bundled list of top and well-known companies, plus forgiving
// name matching: ignores case, spaces, punctuation and words like
// "pvt ltd", "inc", "technologies".
// ============================================================

object CompanyMatcher {

    /** Words that do not help to tell one company from another. */
    private val NOISE_WORDS = setOf(
        "pvt", "private", "ltd", "limited", "inc", "incorporated", "llc", "llp",
        "corp", "corporation", "co", "company", "technologies", "technology",
        "tech", "solutions", "software", "services", "systems", "labs", "lab",
        "group", "india", "global", "the", "and"
    )

    /** Top-tier companies. Names are written normally; matching cleans them. */
    private val TOP_COMPANIES = listOf(
        "Google", "Alphabet", "Microsoft", "Amazon", "Amazon Web Services", "AWS",
        "Apple", "Meta", "Facebook", "Instagram", "WhatsApp", "Netflix", "Nvidia", "Intel", "AMD",
        "Adobe", "Salesforce", "Oracle", "IBM", "Cisco", "Qualcomm", "Samsung", "Sony",
        "Uber", "Airbnb", "LinkedIn", "Atlassian", "Stripe", "Databricks", "Snowflake",
        "OpenAI", "Anthropic", "DeepMind", "Tesla", "SpaceX", "PayPal", "Visa", "Mastercard",
        "Goldman Sachs", "JP Morgan", "JPMorgan Chase", "Morgan Stanley", "Deutsche Bank",
        "American Express", "Barclays", "Citi", "Citibank", "BlackRock", "Bloomberg",
        "Flipkart", "Swiggy", "Zomato", "Paytm", "PhonePe", "Razorpay", "CRED", "Zerodha",
        "Ola", "Myntra", "Meesho", "Groww", "Dream11", "Freshworks", "Zoho", "Postman",
        "BrowserStack", "InMobi", "ShareChat", "Dunzo", "Nykaa", "Cars24", "Unacademy",
        "PhysicsWallah", "Walmart", "Walmart Global Tech", "Target", "Cisco Systems",
        "VMware", "Dell", "HP", "Hewlett Packard", "SAP", "Siemens", "Bosch", "Philips",
        "Texas Instruments", "Micron", "Arista", "Nutanix", "Juniper", "Palo Alto Networks",
        "Tata Consultancy Services", "TCS", "Infosys", "Wipro", "HCL", "HCLTech",
        "Tech Mahindra", "Cognizant", "Accenture", "Capgemini", "Deloitte", "PwC", "KPMG",
        "EY", "Ernst and Young", "McKinsey", "BCG", "Boston Consulting Group", "Bain",
        "ISRO", "DRDO", "CERN", "Reliance Jio", "Jio", "Reliance", "Airtel", "Bharti Airtel",
        "Intuit", "ServiceNow", "Workday", "Twilio", "Shopify", "Spotify", "Pinterest",
        "Snap", "Snapchat", "Twitter", "X Corp", "ByteDance", "TikTok", "Tencent", "Alibaba"
    )

    private fun tokens(name: String): List<String> =
        name.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .split(' ')
            .filter { it.isNotBlank() && it !in NOISE_WORDS }

    /** A cleaned, space-less version used for comparing two names. */
    fun normalize(name: String): String = tokens(name).joinToString("")

    private val topTokens: List<List<String>> by lazy {
        TOP_COMPANIES.map { tokens(it) }.filter { it.isNotEmpty() }
    }

    private val topCollapsed: Set<String> by lazy {
        topTokens.map { it.joinToString("") }.toSet()
    }

    /**
     * True if the typed company is a top or well-known company.
     * "Google India Pvt Ltd" -> true, "Pineapple Labs" -> false.
     */
    fun isTopCompany(typedName: String): Boolean {
        val typed = tokens(typedName)
        if (typed.isEmpty()) return false

        val collapsed = typed.joinToString("")
        if (collapsed in topCollapsed) return true

        // The typed name starts with a known company name,
        // for example "Amazon Web Services" or "Google Cloud".
        return topTokens.any { top ->
            top.size <= typed.size && typed.subList(0, top.size) == top &&
                    top.joinToString("").length >= 3
        }
    }

    /** Company type chip values and the share of the company score each gives. */
    enum class CompanyType(val label: String, val share: Double) {
        LOCAL("Local", 0.20),
        STARTUP("Startup", 0.50),
        MEDIUM("Medium", 0.70),
        BIG("Big or top", 1.00);

        companion object {
            fun fromName(name: String?): CompanyType? =
                entries.firstOrNull { it.name == name?.trim()?.uppercase() }
        }
    }

    /** Share (0-1) of the company_strength parameter: the higher of list and chip. */
    fun companyStrengthShare(typedName: String, chip: CompanyType?): Double {
        val fromList = if (isTopCompany(typedName)) 1.0 else 0.0
        val fromChip = chip?.share ?: CompanyType.LOCAL.share
        return maxOf(fromList, fromChip)
    }

    /** Forgiving check used by the proof reader: is the company in the text? */
    fun nameAppearsInText(companyName: String, text: String): Boolean {
        val companyTokens = tokens(companyName)
        if (companyTokens.isEmpty()) return false
        val textTokens = text.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
        val textCollapsed = textTokens.replace(" ", "")

        val collapsed = companyTokens.joinToString("")
        if (collapsed.length >= 3 && textCollapsed.contains(collapsed)) return true

        val textSet = textTokens.split(' ').toSet()
        val strong = companyTokens.filter { it.length >= 3 }
        return strong.isNotEmpty() && strong.all { it in textSet }
    }
}
