package me.lyh.protobuf.generic;

import java.io.IOException;
import java.math.RoundingMode;
import java.util.Arrays;

import static java.math.RoundingMode.*;

public class Base64 {

  private static final Alphabet alphabet =
      new Alphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray());

  private static final char paddingChar = '=';

  private static final char ASCII_MAX = 127;

  public static String encode(byte[] bytes) {
    return encode(bytes, 0, bytes.length);
  }

  public static byte[] decode(CharSequence chars) {
    try {
      return decodeChecked(chars);
    } catch (DecodingException badInput) {
      throw new IllegalArgumentException(badInput);
    }
  }

  private static String encode(byte[] bytes, int off, int len) {
    StringBuilder result = new StringBuilder(maxEncodedSize(len));
    try {
      encodeTo(result, bytes, off, len);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
    return result.toString();
  }

  private static void encodeTo(Appendable target, byte[] bytes, int off, int len) throws IOException {
    for (int i = 0; i < len; i += alphabet.bytesPerChunk) {
      encodeChunkTo(target, bytes, off + i, Math.min(alphabet.bytesPerChunk, len - i));
    }
  }

  private static void encodeChunkTo(Appendable target, byte[] bytes, int off, int len)
      throws IOException {
    long bitBuffer = 0;
    for (int i = 0; i < len; ++i) {
      bitBuffer |= bytes[off + i] & 0xFF;
      bitBuffer <<= 8; // Add additional zero byte in the end.
    }
    // Position of first character is length of bitBuffer minus bitsPerChar.
    final int bitOffset = (len + 1) * 8 - alphabet.bitsPerChar;
    int bitsProcessed = 0;
    while (bitsProcessed < len * 8) {
      int charIndex = (int) (bitBuffer >>> (bitOffset - bitsProcessed)) & alphabet.mask;
      target.append(alphabet.encode(charIndex));
      bitsProcessed += alphabet.bitsPerChar;
    }
    while (bitsProcessed < alphabet.bytesPerChunk * 8) {
      target.append(paddingChar);
      bitsProcessed += alphabet.bitsPerChar;
    }
  }

  private static byte[] decodeChecked(CharSequence chars) throws DecodingException {
    chars = trimTrailingFrom(chars);
    byte[] tmp = new byte[maxDecodedSize(chars.length())];
    int len = decodeTo(tmp, chars);
    return extract(tmp, len);
  }

  private static int decodeTo(byte[] target, CharSequence chars) throws DecodingException {
    chars = trimTrailingFrom(chars);
    if (!alphabet.isValidPaddingStartPosition(chars.length())) {
      throw new DecodingException("Invalid input length " + chars.length());
    }
    int bytesWritten = 0;
    for (int charIdx = 0; charIdx < chars.length(); charIdx += alphabet.charsPerChunk) {
      long chunk = 0;
      int charsProcessed = 0;
      for (int i = 0; i < alphabet.charsPerChunk; i++) {
        chunk <<= alphabet.bitsPerChar;
        if (charIdx + i < chars.length()) {
          chunk |= alphabet.decode(chars.charAt(charIdx + charsProcessed++));
        }
      }
      final int minOffset = alphabet.bytesPerChunk * 8 - charsProcessed * alphabet.bitsPerChar;
      for (int offset = (alphabet.bytesPerChunk - 1) * 8; offset >= minOffset; offset -= 8) {
        target[bytesWritten++] = (byte) ((chunk >>> offset) & 0xFF);
      }
    }
    return bytesWritten;
  }

  private static int maxEncodedSize(int bytes) {
    return alphabet.charsPerChunk * divide(bytes, alphabet.bytesPerChunk, CEILING);
  }

  private static int maxDecodedSize(int chars) {
    return (int) ((alphabet.bitsPerChar * (long) chars + 7L) / 8L);
  }

  private static byte[] extract(byte[] result, int length) {
    if (length == result.length) {
      return result;
    } else {
      byte[] trunc = new byte[length];
      System.arraycopy(result, 0, trunc, 0, length);
      return trunc;
    }
  }

  private static String trimTrailingFrom(CharSequence sequence) {
    int len = sequence.length();
    for (int last = len - 1; last >= 0; last--) {
      if (sequence.charAt(last) != paddingChar) {
        return sequence.subSequence(0, last + 1).toString();
      }
    }
    return "";
  }

  private static int divide(int p, int q, RoundingMode mode) {
    if (q == 0) {
      throw new ArithmeticException("/ by zero"); // for GWT
    }
    int div = p / q;
    int rem = p - q * div; // equal to p % q

    if (rem == 0) {
      return div;
    }

    /*
     * Normal Java division rounds towards 0, consistently with RoundingMode.DOWN. We just have to
     * deal with the cases where rounding towards 0 is wrong, which typically depends on the sign of
     * p / q.
     *
     * signum is 1 if p and q are both nonnegative or both negative, and -1 otherwise.
     */
    int signum = 1 | ((p ^ q) >> (Integer.SIZE - 1));
    boolean increment;
    switch (mode) {
      case UNNECESSARY:
        throw new ArithmeticException("mode was UNNECESSARY, but rounding was necessary");
        // fall through
      case DOWN:
        increment = false;
        break;
      case UP:
        increment = true;
        break;
      case CEILING:
        increment = signum > 0;
        break;
      case FLOOR:
        increment = signum < 0;
        break;
      case HALF_EVEN:
      case HALF_DOWN:
      case HALF_UP:
        int absRem = Math.abs(rem);
        int cmpRemToHalfDivisor = absRem - (Math.abs(q) - absRem);
        // subtracting two nonnegative ints can't overflow
        // cmpRemToHalfDivisor has the same sign as compare(abs(rem), abs(q) / 2).
        if (cmpRemToHalfDivisor == 0) { // exactly on the half mark
          increment = (mode == HALF_UP || (mode == HALF_EVEN & (div & 1) != 0));
        } else {
          increment = cmpRemToHalfDivisor > 0; // closer to the UP value
        }
        break;
      default:
        throw new AssertionError();
    }
    return increment ? div + signum : div;
  }

  private static int log2(int x, RoundingMode mode) {
    switch (mode) {
      case UNNECESSARY:
        if (x > 0 & (x & (x - 1)) != 0)
          throw new ArithmeticException("mode was UNNECESSARY, but rounding was necessary");
        // fall through
      case DOWN:
      case FLOOR:
        return (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(x);

      case UP:
      case CEILING:
        return Integer.SIZE - Integer.numberOfLeadingZeros(x - 1);

      case HALF_DOWN:
      case HALF_UP:
      case HALF_EVEN:
        // Since sqrt(2) is irrational, log2(x) - logFloor cannot be exactly 0.5
        int leadingZeros = Integer.numberOfLeadingZeros(x);
        int cmp = 0xB504F333 >>> leadingZeros;
        // floor(2^(logFloor + 0.5))
        int logFloor = (Integer.SIZE - 1) - leadingZeros;
        return logFloor + (~~(cmp - x) >>> (Integer.SIZE - 1));

      default:
        throw new AssertionError();
    }
  }

  private static final class DecodingException extends IOException {
    DecodingException(String message) {
      super(message);
    }
  }

  private static final class Alphabet {
    // this is meant to be immutable -- don't modify it!
    private final char[] chars;
    final int mask;
    final int bitsPerChar;
    final int charsPerChunk;
    final int bytesPerChunk;
    private final byte[] decodabet;
    private final boolean[] validPadding;

    Alphabet(char[] chars) {
      this.chars = chars;
      try {
        this.bitsPerChar = log2(chars.length, UNNECESSARY);
      } catch (ArithmeticException e) {
        throw new IllegalArgumentException("Illegal alphabet length " + chars.length, e);
      }

      /*
       * e.g. for base64, bitsPerChar == 6, charsPerChunk == 4, and bytesPerChunk == 3. This makes
       * for the smallest chunk size that still has charsPerChunk * bitsPerChar be a multiple of 8.
       */
      int gcd = Math.min(8, Integer.lowestOneBit(bitsPerChar));
      this.charsPerChunk = 8 / gcd;
      this.bytesPerChunk = bitsPerChar / gcd;

      this.mask = chars.length - 1;

      byte[] decodabet = new byte[ASCII_MAX + 1];
      Arrays.fill(decodabet, (byte) -1);
      for (int i = 0; i < chars.length; i++) {
        char c = chars[i];
        decodabet[c] = (byte) i;
      }
      this.decodabet = decodabet;

      boolean[] validPadding = new boolean[charsPerChunk];
      for (int i = 0; i < bytesPerChunk; i++) {
        validPadding[divide(i * 8, bitsPerChar, CEILING)] = true;
      }
      this.validPadding = validPadding;
    }

    char encode(int bits) {
      return chars[bits];
    }

    boolean isValidPaddingStartPosition(int index) {
      return validPadding[index % charsPerChunk];
    }

    int decode(char ch) throws DecodingException {
      if (ch > ASCII_MAX || decodabet[ch] == -1) {
        throw new DecodingException("Unrecognized character 0x" + Integer.toHexString(ch));
      }
      return decodabet[ch];
    }
  }
}
