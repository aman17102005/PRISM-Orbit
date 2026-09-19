package com.prismorbit.app

data class KnownDsaProblem(
    val name: String,
    val platform: String,
    val topic: String,
    val difficulty: String
)

object KnownDsaProblems {

    val ALL = listOf(

        // ---------- ARRAYS ----------
        KnownDsaProblem("Two Sum", "LeetCode", "Arrays", "Easy"),
        KnownDsaProblem("Best Time to Buy and Sell Stock", "LeetCode", "Arrays", "Easy"),
        KnownDsaProblem("Contains Duplicate", "LeetCode", "Arrays", "Easy"),
        KnownDsaProblem("Product of Array Except Self", "LeetCode", "Arrays", "Medium"),
        KnownDsaProblem("Maximum Subarray", "LeetCode", "Arrays", "Medium"),
        KnownDsaProblem("Maximum Product Subarray", "LeetCode", "Arrays", "Medium"),
        KnownDsaProblem("Move Zeroes", "LeetCode", "Arrays", "Easy"),
        KnownDsaProblem("Merge Sorted Array", "LeetCode", "Arrays", "Easy"),
        KnownDsaProblem("Rotate Array", "LeetCode", "Arrays", "Medium"),
        KnownDsaProblem("Majority Element", "LeetCode", "Arrays", "Easy"),
        KnownDsaProblem("Find the Duplicate Number", "LeetCode", "Arrays", "Medium"),
        KnownDsaProblem("Missing Number", "LeetCode", "Arrays", "Easy"),
        KnownDsaProblem("Subarray Sum Equals K", "LeetCode", "Arrays", "Medium"),
        KnownDsaProblem("3Sum", "LeetCode", "Arrays", "Medium"),
        KnownDsaProblem("4Sum", "LeetCode", "Arrays", "Medium"),

        // ---------- STRINGS ----------
        KnownDsaProblem("Valid Anagram", "LeetCode", "Strings", "Easy"),
        KnownDsaProblem("Valid Palindrome", "LeetCode", "Strings", "Easy"),
        KnownDsaProblem("Longest Substring Without Repeating Characters", "LeetCode", "Strings", "Medium"),
        KnownDsaProblem("Longest Palindromic Substring", "LeetCode", "Strings", "Medium"),
        KnownDsaProblem("Group Anagrams", "LeetCode", "Strings", "Medium"),
        KnownDsaProblem("Valid Parentheses", "LeetCode", "Strings", "Easy"),
        KnownDsaProblem("Implement strStr()", "LeetCode", "Strings", "Easy"),
        KnownDsaProblem("Reverse String", "LeetCode", "Strings", "Easy"),
        KnownDsaProblem("Reverse Words in a String", "LeetCode", "Strings", "Medium"),
        KnownDsaProblem("String to Integer (atoi)", "LeetCode", "Strings", "Medium"),

        // ---------- SEARCHING & SORTING ----------
        KnownDsaProblem("Binary Search", "LeetCode", "Searching & Sorting", "Easy"),
        KnownDsaProblem("Search in Rotated Sorted Array", "LeetCode", "Searching & Sorting", "Medium"),
        KnownDsaProblem("Find Minimum in Rotated Sorted Array", "LeetCode", "Searching & Sorting", "Medium"),
        KnownDsaProblem("First Bad Version", "LeetCode", "Searching & Sorting", "Easy"),
        KnownDsaProblem("Search a 2D Matrix", "LeetCode", "Searching & Sorting", "Medium"),
        KnownDsaProblem("Merge Sort", "HackerRank", "Searching & Sorting", "Medium"),
        KnownDsaProblem("Quicksort", "HackerRank", "Searching & Sorting", "Medium"),

        // ---------- LINKED LIST ----------
        KnownDsaProblem("Reverse Linked List", "LeetCode", "Linked List", "Easy"),
        KnownDsaProblem("Merge Two Sorted Lists", "LeetCode", "Linked List", "Easy"),
        KnownDsaProblem("Linked List Cycle", "LeetCode", "Linked List", "Easy"),
        KnownDsaProblem("Remove Nth Node From End of List", "LeetCode", "Linked List", "Medium"),
        KnownDsaProblem("Middle of the Linked List", "LeetCode", "Linked List", "Easy"),
        KnownDsaProblem("Intersection of Two Linked Lists", "LeetCode", "Linked List", "Easy"),
        KnownDsaProblem("Add Two Numbers", "LeetCode", "Linked List", "Medium"),
        KnownDsaProblem("Palindrome Linked List", "LeetCode", "Linked List", "Easy"),

        // ---------- STACK & QUEUE ----------
        KnownDsaProblem("Valid Parentheses", "LeetCode", "Stack & Queue", "Easy"),
        KnownDsaProblem("Min Stack", "LeetCode", "Stack & Queue", "Medium"),
        KnownDsaProblem("Evaluate Reverse Polish Notation", "LeetCode", "Stack & Queue", "Medium"),
        KnownDsaProblem("Daily Temperatures", "LeetCode", "Stack & Queue", "Medium"),
        KnownDsaProblem("Implement Queue using Stacks", "LeetCode", "Stack & Queue", "Easy"),
        KnownDsaProblem("Implement Stack using Queues", "LeetCode", "Stack & Queue", "Easy"),

        // ---------- HASHING ----------
        KnownDsaProblem("Two Sum", "LeetCode", "Hashing", "Easy"),
        KnownDsaProblem("Contains Duplicate", "LeetCode", "Hashing", "Easy"),
        KnownDsaProblem("Group Anagrams", "LeetCode", "Hashing", "Medium"),
        KnownDsaProblem("Longest Consecutive Sequence", "LeetCode", "Hashing", "Medium"),
        KnownDsaProblem("Top K Frequent Elements", "LeetCode", "Hashing", "Medium"),
        KnownDsaProblem("Subarray Sum Equals K", "LeetCode", "Hashing", "Medium"),

        // ---------- RECURSION ----------
        KnownDsaProblem("Fibonacci Number", "LeetCode", "Recursion", "Easy"),
        KnownDsaProblem("Climbing Stairs", "LeetCode", "Recursion", "Easy"),
        KnownDsaProblem("Pow(x, n)", "LeetCode", "Recursion", "Medium"),
        KnownDsaProblem("K-th Symbol in Grammar", "LeetCode", "Recursion", "Medium"),

        // ---------- TREES / BST ----------
        KnownDsaProblem("Maximum Depth of Binary Tree", "LeetCode", "Trees / BST", "Easy"),
        KnownDsaProblem("Invert Binary Tree", "LeetCode", "Trees / BST", "Easy"),
        KnownDsaProblem("Same Tree", "LeetCode", "Trees / BST", "Easy"),
        KnownDsaProblem("Binary Tree Level Order Traversal", "LeetCode", "Trees / BST", "Medium"),
        KnownDsaProblem("Validate Binary Search Tree", "LeetCode", "Trees / BST", "Medium"),
        KnownDsaProblem("Lowest Common Ancestor of a Binary Tree", "LeetCode", "Trees / BST", "Medium"),
        KnownDsaProblem("Diameter of Binary Tree", "LeetCode", "Trees / BST", "Easy"),
        KnownDsaProblem("Balanced Binary Tree", "LeetCode", "Trees / BST", "Easy"),

        // ---------- HEAP ----------
        KnownDsaProblem("Kth Largest Element in an Array", "LeetCode", "Heap", "Medium"),
        KnownDsaProblem("Top K Frequent Elements", "LeetCode", "Heap", "Medium"),
        KnownDsaProblem("Merge K Sorted Lists", "LeetCode", "Heap", "Hard"),
        KnownDsaProblem("Find Median from Data Stream", "LeetCode", "Heap", "Hard"),

        // ---------- GREEDY ----------
        KnownDsaProblem("Jump Game", "LeetCode", "Greedy", "Medium"),
        KnownDsaProblem("Jump Game II", "LeetCode", "Greedy", "Medium"),
        KnownDsaProblem("Best Time to Buy and Sell Stock II", "LeetCode", "Greedy", "Medium"),
        KnownDsaProblem("Assign Cookies", "LeetCode", "Greedy", "Easy"),
        KnownDsaProblem("Gas Station", "LeetCode", "Greedy", "Medium"),

        // ---------- GRAPHS ----------
        KnownDsaProblem("Number of Islands", "LeetCode", "Graphs", "Medium"),
        KnownDsaProblem("Clone Graph", "LeetCode", "Graphs", "Medium"),
        KnownDsaProblem("Course Schedule", "LeetCode", "Graphs", "Medium"),
        KnownDsaProblem("Course Schedule II", "LeetCode", "Graphs", "Medium"),
        KnownDsaProblem("Pacific Atlantic Water Flow", "LeetCode", "Graphs", "Medium"),
        KnownDsaProblem("Rotting Oranges", "LeetCode", "Graphs", "Medium"),
        KnownDsaProblem("Word Ladder", "LeetCode", "Graphs", "Hard"),

        // ---------- BACKTRACKING ----------
        KnownDsaProblem("Subsets", "LeetCode", "Backtracking", "Medium"),
        KnownDsaProblem("Permutations", "LeetCode", "Backtracking", "Medium"),
        KnownDsaProblem("Combination Sum", "LeetCode", "Backtracking", "Medium"),
        KnownDsaProblem("N-Queens", "LeetCode", "Backtracking", "Hard"),
        KnownDsaProblem("Generate Parentheses", "LeetCode", "Backtracking", "Medium"),
        KnownDsaProblem("Word Search", "LeetCode", "Backtracking", "Medium"),

        // ---------- TRIES ----------
        KnownDsaProblem("Implement Trie (Prefix Tree)", "LeetCode", "Tries", "Medium"),
        KnownDsaProblem("Design Add and Search Words Data Structure", "LeetCode", "Tries", "Medium"),
        KnownDsaProblem("Word Search II", "LeetCode", "Tries", "Hard"),

        // ---------- DYNAMIC PROGRAMMING ----------
        KnownDsaProblem("Climbing Stairs", "LeetCode", "Dynamic Programming", "Easy"),
        KnownDsaProblem("House Robber", "LeetCode", "Dynamic Programming", "Medium"),
        KnownDsaProblem("House Robber II", "LeetCode", "Dynamic Programming", "Medium"),
        KnownDsaProblem("Coin Change", "LeetCode", "Dynamic Programming", "Medium"),
        KnownDsaProblem("Longest Increasing Subsequence", "LeetCode", "Dynamic Programming", "Medium"),
        KnownDsaProblem("Longest Common Subsequence", "LeetCode", "Dynamic Programming", "Medium"),
        KnownDsaProblem("Word Break", "LeetCode", "Dynamic Programming", "Medium"),
        KnownDsaProblem("Partition Equal Subset Sum", "LeetCode", "Dynamic Programming", "Medium"),
        KnownDsaProblem("Edit Distance", "LeetCode", "Dynamic Programming", "Hard"),
        KnownDsaProblem("Maximum Subarray", "LeetCode", "Dynamic Programming", "Medium"),

        // ---------- BIT MANIPULATION ----------
        KnownDsaProblem("Single Number", "LeetCode", "Bit Manipulation", "Easy"),
        KnownDsaProblem("Number of 1 Bits", "LeetCode", "Bit Manipulation", "Easy"),
        KnownDsaProblem("Counting Bits", "LeetCode", "Bit Manipulation", "Easy"),
        KnownDsaProblem("Reverse Bits", "LeetCode", "Bit Manipulation", "Easy"),
        KnownDsaProblem("Power of Two", "LeetCode", "Bit Manipulation", "Easy")
    )

    fun search(query: String): List<KnownDsaProblem> {
        val q = query.trim().lowercase()

        if (q.isEmpty()) return emptyList()

        return ALL
            .filter {
                it.name.lowercase().contains(q)
            }
            .distinctBy { "${it.name}|${it.platform}|${it.topic}" }
            .take(6)
    }
}