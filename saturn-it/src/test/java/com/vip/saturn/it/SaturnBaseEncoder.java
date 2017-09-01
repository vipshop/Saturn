/**
 * 
 */
package com.vip.saturn.it;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author chembo.huang
 *
 */
public class SaturnBaseEncoder {
	private final byte[] newline;
	private final int linemax;
	private final boolean isURL;
	private final boolean doPadding;

	private SaturnBaseEncoder(boolean isURL, byte[] newline, int linemax, boolean doPadding) {
		this.isURL = isURL;
		this.newline = newline;
		this.linemax = linemax;
		this.doPadding = doPadding;
	}

	public static SaturnBaseEncoder getInstance() {
		return RFC4648;
	}

	/**
	 * This array is a lookup table that translates 6-bit positive integer index values into their "Base64 Alphabet"
	 * equivalents as specified in "Table 1: The Base64 Alphabet" of RFC 2045 (and RFC 4648).
	 */
	private static final char[] toBase64 = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
			'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
			'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4',
			'5', '6', '7', '8', '9', '+', '/' };

	/**
	 * It's the lookup table for "URL and Filename safe Base64" as specified in Table 2 of the RFC 4648, with the '+'
	 * and '/' changed to '-' and '_'. This table is used when BASE64_URL is specified.
	 */
	private static final char[] toBase64URL = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
			'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
			'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
			'4', '5', '6', '7', '8', '9', '-', '_' };

	static final SaturnBaseEncoder RFC4648 = new SaturnBaseEncoder(false, null, -1, true);

	private final int outLength(int srclen) {
		int len = 0;
		if (doPadding) {
			len = 4 * ((srclen + 2) / 3);
		} else {
			int n = srclen % 3;
			len = 4 * (srclen / 3) + (n == 0 ? 0 : n + 1);
		}
		if (linemax > 0) // line separators
			len += (len - 1) / linemax * newline.length;
		return len;
	}

	/**
	 * Encodes all bytes from the specified byte array into a newly-allocated byte array using the {@link Base64}
	 * encoding scheme. The returned byte array is of the length of the resulting bytes.
	 *
	 * @param src the byte array to encode
	 * @return A newly-allocated byte array containing the resulting encoded bytes.
	 */
	public byte[] encode(byte[] src) {
		int len = outLength(src.length); // dst array size
		byte[] dst = new byte[len];
		int ret = encode0(src, 0, src.length, dst);
		if (ret != dst.length)
			return Arrays.copyOf(dst, ret);
		return dst;
	}

	/**
	 * Encodes all bytes from the specified byte array using the {@link Base64} encoding scheme, writing the resulting
	 * bytes to the given output byte array, starting at offset 0.
	 *
	 * <p>
	 * It is the responsibility of the invoker of this method to make sure the output byte array {@code dst} has enough
	 * space for encoding all bytes from the input byte array. No bytes will be written to the output byte array if the
	 * output byte array is not big enough.
	 *
	 * @param src the byte array to encode
	 * @param dst the output byte array
	 * @return The number of bytes written to the output byte array
	 *
	 * @throws IllegalArgumentException if {@code dst} does not have enough space for encoding all input bytes.
	 */
	public int encode(byte[] src, byte[] dst) {
		int len = outLength(src.length); // dst array size
		if (dst.length < len)
			throw new IllegalArgumentException("Output byte array is too small for encoding all input bytes");
		return encode0(src, 0, src.length, dst);
	}

	/**
	 * Encodes the specified byte array into a String using the {@link Base64} encoding scheme.
	 *
	 * <p>
	 * This method first encodes all input bytes into a base64 encoded byte array and then constructs a new String by
	 * using the encoded byte array and the {@link java.nio.charset.StandardCharsets#ISO_8859_1 ISO-8859-1} charset.
	 *
	 * <p>
	 * In other words, an invocation of this method has exactly the same effect as invoking
	 * {@code new String(encode(src), StandardCharsets.ISO_8859_1)}.
	 *
	 * @param src the byte array to encode
	 * @return A String containing the resulting Base64 encoded characters
	 */
	@SuppressWarnings("deprecation")
	public String encodeToString(byte[] src) {
		byte[] encoded = encode(src);
		return new String(encoded, 0, 0, encoded.length);
	}

	/**
	 * Encodes all remaining bytes from the specified byte buffer into a newly-allocated ByteBuffer using the
	 * {@link Base64} encoding scheme.
	 *
	 * Upon return, the source buffer's position will be updated to its limit; its limit will not have been changed. The
	 * returned output buffer's position will be zero and its limit will be the number of resulting encoded bytes.
	 *
	 * @param buffer the source ByteBuffer to encode
	 * @return A newly-allocated byte buffer containing the encoded bytes.
	 */
	public ByteBuffer encode(ByteBuffer buffer) {
		int len = outLength(buffer.remaining());
		byte[] dst = new byte[len];
		int ret = 0;
		if (buffer.hasArray()) {
			ret = encode0(buffer.array(), buffer.arrayOffset() + buffer.position(),
					buffer.arrayOffset() + buffer.limit(), dst);
			buffer.position(buffer.limit());
		} else {
			byte[] src = new byte[buffer.remaining()];
			buffer.get(src);
			ret = encode0(src, 0, src.length, dst);
		}
		if (ret != dst.length)
			dst = Arrays.copyOf(dst, ret);
		return ByteBuffer.wrap(dst);
	}

	/**
	 * Returns an encoder instance that encodes equivalently to this one, but without adding any padding character at
	 * the end of the encoded byte data.
	 *
	 * <p>
	 * The encoding scheme of this encoder instance is unaffected by this invocation. The returned encoder instance
	 * should be used for non-padding encoding operation.
	 *
	 * @return an equivalent encoder that encodes without adding any padding character at the end
	 */
	public SaturnBaseEncoder withoutPadding() {
		if (!doPadding)
			return this;
		return new SaturnBaseEncoder(isURL, newline, linemax, false);
	}

	private int encode0(byte[] src, int off, int end, byte[] dst) {
		char[] base64 = isURL ? toBase64URL : toBase64;
		int sp = off;
		int slen = (end - off) / 3 * 3;
		int sl = off + slen;
		if (linemax > 0 && slen > linemax / 4 * 3)
			slen = linemax / 4 * 3;
		int dp = 0;
		while (sp < sl) {
			int sl0 = Math.min(sp + slen, sl);
			for (int sp0 = sp, dp0 = dp; sp0 < sl0;) {
				int bits = (src[sp0++] & 0xff) << 16 | (src[sp0++] & 0xff) << 8 | (src[sp0++] & 0xff);
				dst[dp0++] = (byte) base64[(bits >>> 18) & 0x3f];
				dst[dp0++] = (byte) base64[(bits >>> 12) & 0x3f];
				dst[dp0++] = (byte) base64[(bits >>> 6) & 0x3f];
				dst[dp0++] = (byte) base64[bits & 0x3f];
			}
			int dlen = (sl0 - sp) / 3 * 4;
			dp += dlen;
			sp = sl0;
			if (dlen == linemax && sp < end) {
				for (byte b : newline) {
					dst[dp++] = b;
				}
			}
		}
		if (sp < end) { // 1 or 2 leftover bytes
			int b0 = src[sp++] & 0xff;
			dst[dp++] = (byte) base64[b0 >> 2];
			if (sp == end) {
				dst[dp++] = (byte) base64[(b0 << 4) & 0x3f];
				if (doPadding) {
					dst[dp++] = '=';
					dst[dp++] = '=';
				}
			} else {
				int b1 = src[sp++] & 0xff;
				dst[dp++] = (byte) base64[(b0 << 4) & 0x3f | (b1 >> 4)];
				dst[dp++] = (byte) base64[(b1 << 2) & 0x3f];
				if (doPadding) {
					dst[dp++] = '=';
				}
			}
		}
		return dp;
	}
}
