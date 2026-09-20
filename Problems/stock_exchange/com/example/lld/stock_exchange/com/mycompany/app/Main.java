package com.mycompany.app;

 class Solution {

    public int countBinaryPalindromes(long n) {
        int count = 0;
        int maxLen = Long.toBinaryString(n).length();

        // Explicitly count '0'
        if (n >= 0) {
            count += 1;
        }

        // Generate palindromes for all lengths from 1 to maxLen
        for (int len = 1; len <= maxLen; len++) {
            count += countPalindromesOfLength(n, len);
        }

        return count;
    }

    private int countPalindromesOfLength(long n, int length) {
        int halfLen = (length + 1) / 2;
        long start = 1L << (halfLen - 1);
        long end = 1L << halfLen;
        int result = 0;

        for (long half = start; half < end; half++) {
            long palin = createPalindrome(half, length);
            if (palin <= n) {
                result++;
            }
        }
        return result;
    }

    private long createPalindrome(long half, int length) {
        long palin = half;
        int bitsToMirror = length / 2;
        for (int i = 0; i < bitsToMirror; i++) {
            long bit = (half >> i) & 1L;
            palin = (palin << 1) | bit;
        }
        return palin;
    }
     public static void main(String[] args) {
         Solution solution = new Solution();

         System.out.println(solution.countBinaryPalindromes(4));    // Output: 3
         System.out.println(solution.countBinaryPalindromes(9));    // Output: 6
         System.out.println(solution.countBinaryPalindromes(5));    // Output: 4
         System.out.println(solution.countBinaryPalindromes(1));    // Output: 2
         System.out.println(solution.countBinaryPalindromes(20));   // Output: 9
         System.out.println(solution.countBinaryPalindromes(1000000000000000L)); // Large test
     }
 }

