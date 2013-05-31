/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.robustness;

import java.io.IOException;
import java.io.InputStream;

public class EmptyInputStream extends InputStream {
	long count;

	public EmptyInputStream(long reads) {
		count = reads;
	}

	@Override
	public int read() throws IOException {
		count--;
		if (count >= 0)
			return 0;
		else
			return -1;
	}
}
