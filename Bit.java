// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Bit {

    /**
     * Checks if the bit at the specified position is set.
     *
     * @param x    The bitmask.
     * @param pos  The position to check (0-63).
     * @return True if the bit at position `pos` is set; otherwise, false.
     */
    public static boolean isSet(long x, int pos) {
        return (x & (1L << pos)) != 0;
    }

    /**
     * Sets the bit at the specified position.
     *
     * @param x    The original bitmask.
     * @param pos  The position to set (0-63).
     * @return A new bitmask with the bit at position `pos` set.
     */
    public static long set(long x, int pos) {
        return x | (1L << pos);
    }

    /**
     * Clears the bit at the specified position.
     *
     * @param x    The original bitmask.
     * @param pos  The position to clear (0-63).
     * @return A new bitmask with the bit at position `pos` cleared.
     */
    public static long clear(long x, int pos) {
        return x & ~(1L << pos);
    }

    /**
     * Returns a bitmask with only the bit at the specified position set.
     *
     * @param position  The position to set (0-63).
     * @return A bitmask with only the bit at position `position` set.
     */
    public static long positionMask(int position) {
        return mask(position);
    }

    /**
     * Returns a bitmask with only the bit at the specified position set.
     * Alias for positionMask to maintain consistency.
     *
     * @param position  The position to set (0-63).
     * @return A bitmask with only the bit at position `position` set.
     */
    public static long mask(int position) {
        return 1L << position;
    }

    /**
     * Finds the position of the leading one in the bitmask.
     *
     * @param x The bitmask.
     * @return The position of the leading one, or -1 if `x` is zero.
     */
    public static int leadingOne(long x) {
        // Position of the leading one (inverse of mask function).
        // Note: returns -1 if x is zero.
        return x == 0 ? -1 : 63 - countLeadingZeros(x);
    }

    /**
     * Counts the number of set bits (1s) in the bitmask.
     *
     * @param x The bitmask.
     * @return The number of set bits.
     */
    public static int countOnes(long x) {
        int count = 0;
        while (x != 0) {
            x &= (x - 1);
            count++;
        }
        return count;
    }

    /**
     * Counts the number of leading zeros in the bitmask.
     *
     * @param x The bitmask.
     * @return The number of leading zeros.
     */
    public static int countLeadingZeros(long x) {
        int count = 0;
        int shift = 32;
        long y;

        while (shift > 0) {
            y = x >>> shift;
            if (y != 0) {
                count += shift;
                x = y;
            }
            shift >>= 1;
        }
        return x == 0 ? 64 : count + Long.numberOfLeadingZeros(x);
    }

    /**
     * Returns an iterator over the positions of set bits (1s) in the bitmask.
     *
     * @param bits The bitmask.
     * @return An iterator over the positions of set bits.
     */
    public static Iterator<Integer> iterator(long bits) {
        return new BitIterator(bits);
    }

    /**
     * Returns an iterable over the positions of set bits (1s) in the bitmask.
     *
     * @param bits The bitmask.
     * @return An iterable over the positions of set bits.
     */
    public static Iterable<Integer> ones(long bits) {
        return () -> new BitIterator(bits);
    }

    /**
     * Returns a list of positions where bits are set (1s) in the bitmask.
     *
     * @param bits The bitmask.
     * @return A list of positions with set bits.
     */
    public static List<Integer> onesList(long bits) {
        List<Integer> list = new ArrayList<>();
        for (int pos : ones(bits)) {
            list.add(pos);
        }
        return list;
    }

    /**
     * Private class to implement the Iterator for set bits.
     */
    private static class BitIterator implements Iterator<Integer> {

        private long bits;

        public BitIterator(long bits) {
            this.bits = bits;
        }

        public boolean hasNext() {
            return this.bits != 0;
        }

        public Integer next() {
            long temp = this.bits & (this.bits - 1);   // Clear the least significant 1 bit
            long mask = this.bits ^ temp;              // Mask for the bit just cleared
            this.bits = temp;                          // Update the iterator state
            return leadingOne(mask);                   // Position of the bit just cleared
        }
    }
}
