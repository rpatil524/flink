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

package org.apache.flink.runtime.jobmaster.slotpool;

import org.apache.flink.api.common.JobID;
import org.apache.flink.runtime.clusterframework.types.AllocationID;
import org.apache.flink.runtime.clusterframework.types.ResourceID;
import org.apache.flink.runtime.jobmanager.slots.TaskManagerGateway;
import org.apache.flink.runtime.jobmaster.AllocatedSlotReport;
import org.apache.flink.runtime.jobmaster.JobMasterId;
import org.apache.flink.runtime.resourcemanager.ResourceManagerGateway;
import org.apache.flink.runtime.taskexecutor.slot.SlotOffer;
import org.apache.flink.runtime.taskmanager.TaskManagerLocation;
import org.apache.flink.util.function.TriFunction;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/** Testing {@link SlotPoolService} implementation. */
public class TestingSlotPoolService implements SlotPoolService {

    private final JobID jobId;

    private final BiFunction<? super JobMasterId, ? super String, Void> startConsumer;

    private final Runnable closeRunnable;

    private final TriFunction<
                    ? super TaskManagerLocation,
                    ? super TaskManagerGateway,
                    ? super Collection<SlotOffer>,
                    ? extends Collection<SlotOffer>>
            offerSlotsFunction;

    private final TriFunction<
                    ? super ResourceID,
                    ? super AllocationID,
                    ? super Exception,
                    Optional<ResourceID>>
            failAllocationFunction;

    private final Function<? super ResourceID, Boolean> registerTaskManagerFunction;

    private final BiFunction<? super ResourceID, ? super Exception, Boolean>
            releaseTaskManagerFunction;

    private final Consumer<? super ResourceManagerGateway> connectToResourceManagerConsumer;

    private final Runnable disconnectResourceManagerRunnable;

    private final BiFunction<? super JobID, ? super ResourceID, ? extends AllocatedSlotReport>
            createAllocatedSlotReportFunction;

    public TestingSlotPoolService(
            JobID jobId,
            BiFunction<? super JobMasterId, ? super String, Void> startConsumer,
            Runnable closeRunnable,
            TriFunction<
                            ? super TaskManagerLocation,
                            ? super TaskManagerGateway,
                            ? super Collection<SlotOffer>,
                            ? extends Collection<SlotOffer>>
                    offerSlotsFunction,
            TriFunction<
                            ? super ResourceID,
                            ? super AllocationID,
                            ? super Exception,
                            Optional<ResourceID>>
                    failAllocationFunction,
            Function<? super ResourceID, Boolean> registerTaskManagerFunction,
            BiFunction<? super ResourceID, ? super Exception, Boolean> releaseTaskManagerFunction,
            Consumer<? super ResourceManagerGateway> connectToResourceManagerConsumer,
            Runnable disconnectResourceManagerRunnable,
            BiFunction<? super JobID, ? super ResourceID, ? extends AllocatedSlotReport>
                    createAllocatedSlotReportFunction) {
        this.jobId = jobId;
        this.startConsumer = startConsumer;
        this.closeRunnable = closeRunnable;
        this.offerSlotsFunction = offerSlotsFunction;
        this.failAllocationFunction = failAllocationFunction;
        this.registerTaskManagerFunction = registerTaskManagerFunction;
        this.releaseTaskManagerFunction = releaseTaskManagerFunction;
        this.connectToResourceManagerConsumer = connectToResourceManagerConsumer;
        this.disconnectResourceManagerRunnable = disconnectResourceManagerRunnable;
        this.createAllocatedSlotReportFunction = createAllocatedSlotReportFunction;
    }

    @Override
    public void start(JobMasterId jobMasterId, String address) throws Exception {
        startConsumer.apply(jobMasterId, address);
    }

    @Override
    public void close() {
        closeRunnable.run();
    }

    @Override
    public Collection<SlotOffer> offerSlots(
            TaskManagerLocation taskManagerLocation,
            TaskManagerGateway taskManagerGateway,
            Collection<SlotOffer> offers) {
        return offerSlotsFunction.apply(taskManagerLocation, taskManagerGateway, offers);
    }

    @Override
    public Optional<ResourceID> failAllocation(
            @Nullable ResourceID taskManagerId, AllocationID allocationId, Exception cause) {
        return failAllocationFunction.apply(taskManagerId, allocationId, cause);
    }

    @Override
    public boolean registerTaskManager(ResourceID taskManagerId) {
        return registerTaskManagerFunction.apply(taskManagerId);
    }

    @Override
    public boolean releaseTaskManager(ResourceID taskManagerId, Exception cause) {
        return releaseTaskManagerFunction.apply(taskManagerId, cause);
    }

    @Override
    public void releaseFreeSlotsOnTaskManager(ResourceID taskManagerId, Exception cause) {
        throw new UnsupportedOperationException(
                "TestingSlotPoolService does not support this operation.");
    }

    @Override
    public void connectToResourceManager(ResourceManagerGateway resourceManagerGateway) {
        connectToResourceManagerConsumer.accept(resourceManagerGateway);
    }

    @Override
    public void disconnectResourceManager() {
        disconnectResourceManagerRunnable.run();
    }

    @Override
    public AllocatedSlotReport createAllocatedSlotReport(ResourceID taskManagerId) {
        return createAllocatedSlotReportFunction.apply(jobId, taskManagerId);
    }
}
