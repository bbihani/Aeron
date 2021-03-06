/*
 * Copyright 2014 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.aeron.common.concurrent.logbuffer;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import static uk.co.real_logic.aeron.common.concurrent.logbuffer.FrameDescriptor.FRAME_ALIGNMENT;
import static uk.co.real_logic.aeron.common.concurrent.logbuffer.FrameDescriptor.frameLengthVolatile;

/**
 * Scans for gaps in the sequence of bytes in a replicated term buffer between the tail and the
 * high-water-mark. This can be used for detecting loss and generating a NACK message to the source.
 *
 * <b>Note:</b> This class is threadsafe to be used across multiple threads.
 */
public class GapScanner extends LogBufferPartition
{
    /**
     * Handler for notifying of gaps in the log.
     */
    @FunctionalInterface
    public interface GapHandler
    {
        /**
         * Gap detected in log buffer that is being rebuilt.
         *
         * @param buffer containing the gap.
         * @param offset at which the gap begins.
         * @param length of the gap in bytes.
         * @return true if scanning should continue otherwise false to halt scanning.
         */
        boolean onGap(UnsafeBuffer buffer, int offset, int length);
    }

    /**
     * Construct a gap scanner over a log and state buffer.
     *
     * @param termBuffer     containing the sequence of frames.
     * @param metaDataBuffer containing the state of the rebuild process.
     */
    public GapScanner(final UnsafeBuffer termBuffer, final UnsafeBuffer metaDataBuffer)
    {
        super(termBuffer, metaDataBuffer);
    }

    /**
     * Is the log complete with ticks gaps?
     *
     * @return true is he log is complete with no gaps otherwise false.
     */
    public boolean isComplete()
    {
        return tailVolatile() >= capacity();
    }

    /**
     * Scan for gaps from the tail up to the high-water-mark. Each gap will be reported to the {@link GapHandler}.
     *
     * @param handler to be notified of gaps.
     * @return the number of gaps founds.
     */
    public int scan(final GapHandler handler)
    {
        int count = 0;
        final int highWaterMark = highWaterMarkVolatile();
        int offset = tailVolatile();
        final UnsafeBuffer termBuffer = termBuffer();

        while (offset < highWaterMark)
        {
            final int frameLength = BitUtil.align(frameLengthVolatile(termBuffer, offset), FRAME_ALIGNMENT);
            if (frameLength > 0)
            {
                offset += frameLength;
            }
            else
            {
                offset = scanGap(termBuffer, handler, offset, highWaterMark);
                ++count;
            }
        }

        return count;
    }

    private static int scanGap(
        final UnsafeBuffer termBuffer, final GapHandler handler, final int offset, final int highWaterMark)
    {
        int gapLength = 0;
        int alignedFrameLength;
        do
        {
            gapLength += FRAME_ALIGNMENT;
            alignedFrameLength = BitUtil.align(frameLengthVolatile(termBuffer, offset + gapLength), FRAME_ALIGNMENT);
        }
        while (0 == alignedFrameLength);

        return handler.onGap(termBuffer, offset, gapLength) ? (offset + gapLength) : highWaterMark;
    }
}
