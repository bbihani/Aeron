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
package uk.co.real_logic.aeron.driver.buffer;

import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

/**
 * Represents the collection of term and associated state buffers for the connection between a publisher and subscriber
 * connection for the replicated log.
 */
public interface RawLog extends AutoCloseable
{
    /**
     * A {@link Stream} of the {@link RawLogPartition} buffers.
     *
     * @return a {@link Stream} of the {@link RawLogPartition} buffers.
     */
    Stream<? extends RawLogPartition> stream();

    /**
     * An array of the {@link RawLogPartition} buffers.
     *
     * @return an array of the {@link RawLogPartition} buffers.
     */
    RawLogPartition[] partitions();

    /**
     * The meta data storage for the overall log.
     *
     * @return the meta data storage for the overall log.
     */
    UnsafeBuffer logMetaData();

    /**
     * Slice the underlying buffer to provide an array of term buffers in order.
     *
     * @return slices of the underlying buffer to provide an array of term buffers in order.
     */
    ByteBuffer[] sliceTerms();

    /**
     * Get the fully qualified file name for the log file.
     *
     * @return the fully qualified file name for the log file.
     */
    String logFileName();

    void close();
}
