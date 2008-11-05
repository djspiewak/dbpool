/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

/**
 * Decodes passwords using the simple Rot13 algorithm.
 * This algorithm is very insecure, but is included as an example.
 * @author Giles Winstanley
 */
public class RotDecoder implements PasswordDecoder
{
	private static final int offset = 13;

	public char[] decode(String encoded)
	{
		return rot(encoded);
	}

	private char[] rot(String encoded)
	{
		StringBuffer sb = new StringBuffer(encoded);
		for (int a = 0; a < sb.length(); a++)
		{
			char c = sb.charAt(a);
			if (c >= 'A'  &&  c <= 'Z'  ||  c >= 'a'  &&  c <= 'z')
			{
				char base = Character.isUpperCase(c) ? 'A' : 'a';
				int i = c - base;
				c = (char)(base + (i + offset) % 26);
				sb.setCharAt(a, c);
			}
		}
		char[] out = new char[sb.length()];
		sb.getChars(0, out.length, out, 0);
		return out;
	}

/*	public static void main(String[] args) throws Exception
	{
		RotDecoder x = new RotDecoder();
		System.out.println(x.rot(args[0]));
	}*/
}