/*
 * Java Reliable Event Logging Protocol Library Server Implementation RLP-03
 * Copyright (C) 2021-2024 Suomen Kanuuna Oy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 * Additional permission under GNU Affero General Public License version 3
 * section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with other code, such other code is not for that reason alone subject to any
 * of the requirements of the GNU Affero GPL version 3 as long as this Program
 * is the same Program as licensed from Suomen Kanuuna Oy without any additional
 * modifications.
 *
 * Supplemented terms under GNU Affero General Public License version 3
 * section 7
 *
 * Origin of the software must be attributed to Suomen Kanuuna Oy. Any modified
 * versions must be marked as "Modified version of" The Program.
 *
 * Names of the licensors and authors may not be used for publicity purposes.
 *
 * No rights are granted for use of trade names, trademarks, or service marks
 * which are in The Program if any.
 *
 * Licensee must indemnify licensors and authors for any liability that these
 * contractual assumptions impose on licensors and authors.
 *
 * To the extent this program is licensed as part of the Commercial versions of
 * Teragrep, the applicable Commercial License may apply to this file if you as
 * a licensee so wish it.
 */
package com.teragrep.rlp_03.frame.fragment.clocks;

import com.teragrep.rlp_03.frame.fragment.Fragment;
import com.teragrep.rlp_03.frame.fragment.FragmentImpl;
import com.teragrep.rlp_03.frame.fragment.FragmentStub;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public final class PayloadLengthClock {

    private static final FragmentStub fragmentStub = new FragmentStub();
    private final LinkedList<ByteBuffer> bufferSliceList;

    private static final int maximumStringLength = 9 + 1; // space

    public PayloadLengthClock() {
        this.bufferSliceList = new LinkedList<>();
    }

    public Fragment submit(ByteBuffer input) {

        ByteBuffer slice = input.slice();
        int bytesRead = 0;
        boolean complete = false;
        while (input.hasRemaining()) {
            byte b = input.get();
            bytesRead++;
            checkOverSize(bytesRead, bufferSliceList);
            if (b == '\n') {
                /*
                 '\n' is especially for librelp which should follow:
                 HEADER = TXNR SP COMMAND SP DATALEN SP;
                 but sometimes librelp follows:
                 HEADER = TXNR SP COMMAND SP DATALEN LF; and LF is for EndOfTransfer
                 */
                // seek one byte backwards buffer as '\n' is for EndOfTransfer
                input.position(input.position() - 1);

                ((ByteBuffer) slice).limit(bytesRead - 1);

                complete = true;
                break;
            }
            else if (b == ' ') {
                // adjust limit so that bufferSlice contains only this data, without the terminating ' '
                ((ByteBuffer) slice).limit(bytesRead - 1);
                complete = true;
                break;
            }
        }

        bufferSliceList.add(slice);

        Fragment fragment;
        if (complete) {
            fragment = new FragmentImpl(new LinkedList<>(bufferSliceList));
            bufferSliceList.clear();
        }
        else {
            fragment = fragmentStub;
        }

        return fragment;
    }

    private void checkOverSize(int bytesRead, LinkedList<ByteBuffer> bufferSliceList) {
        long currentLength = 0;
        for (ByteBuffer slice : bufferSliceList) {
            currentLength = currentLength + ((ByteBuffer) slice).limit();
        }

        currentLength = currentLength + bytesRead;
        if (currentLength > maximumStringLength) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("payloadLength too long");
            throw illegalArgumentException;
        }
    }
}
