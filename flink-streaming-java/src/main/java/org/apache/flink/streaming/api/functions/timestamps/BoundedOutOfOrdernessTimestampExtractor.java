/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.api.functions.timestamps;

import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.operators.util.WatermarkStrategyWithPeriodicWatermarks;

import java.time.Duration;

/**
 * This is a {@link WatermarkStrategyWithPeriodicWatermarks} used to emit Watermarks that lag behind
 * the element with the maximum timestamp (in event time) seen so far by a fixed amount of time,
 * <code>
 * t_late</code>. This can help reduce the number of elements that are ignored due to lateness when
 * computing the final result for a given window, in the case where we know that elements arrive no
 * later than <code>t_late</code> units of time after the watermark that signals that the system
 * event-time has advanced past their (event-time) timestamp.
 */
public abstract class BoundedOutOfOrdernessTimestampExtractor<T>
        implements WatermarkStrategyWithPeriodicWatermarks<T> {

    private static final long serialVersionUID = 1L;

    /** The current maximum timestamp seen so far. */
    private long currentMaxTimestamp;

    /** The timestamp of the last emitted watermark. */
    private long lastEmittedWatermark = Long.MIN_VALUE;

    /**
     * The (fixed) interval between the maximum seen timestamp seen in the records and that of the
     * watermark to be emitted.
     */
    private final long maxOutOfOrderness;

    public BoundedOutOfOrdernessTimestampExtractor(Duration maxOutOfOrderness) {
        if (maxOutOfOrderness.isNegative()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Tried to set the maximum allowed lateness to %s. This parameter cannot be negative.",
                            maxOutOfOrderness));
        }

        this.maxOutOfOrderness = maxOutOfOrderness.toMillis();
        this.currentMaxTimestamp = Long.MIN_VALUE + this.maxOutOfOrderness;
    }

    public long getMaxOutOfOrdernessInMillis() {
        return maxOutOfOrderness;
    }

    /**
     * Extracts the timestamp from the given element.
     *
     * @param element The element that the timestamp is extracted from.
     * @return The new timestamp.
     */
    public abstract long extractTimestamp(T element);

    @Override
    public final Watermark getCurrentWatermark() {
        // this guarantees that the watermark never goes backwards.
        long potentialWM = currentMaxTimestamp - maxOutOfOrderness;
        if (potentialWM >= lastEmittedWatermark) {
            lastEmittedWatermark = potentialWM;
        }
        return new Watermark(lastEmittedWatermark);
    }

    @Override
    public final long extractTimestamp(T element, long previousElementTimestamp) {
        long timestamp = extractTimestamp(element);
        if (timestamp > currentMaxTimestamp) {
            currentMaxTimestamp = timestamp;
        }
        return timestamp;
    }
}
