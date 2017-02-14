package org.camunda.tngp.broker.taskqueue.processor;

import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.camunda.tngp.protocol.clientapi.EventType.TASK_EVENT;

import org.agrona.concurrent.UnsafeBuffer;
import org.camunda.tngp.broker.Constants;
import org.camunda.tngp.broker.logstreams.BrokerEventMetadata;
import org.camunda.tngp.broker.logstreams.processor.HashIndexSnapshotSupport;
import org.camunda.tngp.broker.logstreams.processor.MetadataFilter;
import org.camunda.tngp.broker.taskqueue.data.TaskEvent;
import org.camunda.tngp.broker.taskqueue.data.TaskEventType;
import org.camunda.tngp.broker.transport.clientapi.CommandResponseWriter;
import org.camunda.tngp.hashindex.Long2BytesHashIndex;
import org.camunda.tngp.hashindex.store.IndexStore;
import org.camunda.tngp.logstreams.log.LogStreamWriter;
import org.camunda.tngp.logstreams.log.LoggedEvent;
import org.camunda.tngp.logstreams.processor.EventProcessor;
import org.camunda.tngp.logstreams.processor.StreamProcessor;
import org.camunda.tngp.logstreams.processor.StreamProcessorContext;
import org.camunda.tngp.logstreams.spi.SnapshotSupport;
import org.camunda.tngp.protocol.clientapi.EventType;

public class TaskInstanceStreamProcessor implements StreamProcessor
{
    protected static final int INDEX_VALUE_LENGTH = SIZE_OF_INT + SIZE_OF_INT;
    protected static final int INDEX_STATE_OFFSET = 0;
    protected static final int INDEX_LOCK_OWNER_OFFSET = SIZE_OF_INT;

    protected BrokerEventMetadata sourceEventMetadata = new BrokerEventMetadata();
    protected final BrokerEventMetadata targetEventMetadata = new BrokerEventMetadata();

    protected final CommandResponseWriter responseWriter;
    protected final IndexStore indexStore;

    protected final CreateTaskProcessor createTaskProcessor = new CreateTaskProcessor();
    protected final LockTaskProcessor lockTaskProcessor = new LockTaskProcessor();
    protected final CompleteTaskProcessor completeTaskProcessor = new CompleteTaskProcessor();

    protected final Long2BytesHashIndex taskIndex;
    protected final HashIndexSnapshotSupport<Long2BytesHashIndex> indexSnapshotSupport;

    protected final IndexAccessor indexAccessor = new IndexAccessor();
    protected final IndexWriter indexWriter = new IndexWriter();

    protected final TaskEvent taskEvent = new TaskEvent();

    protected int streamId;

    protected long eventPosition = 0;
    protected long eventKey = 0;

    public TaskInstanceStreamProcessor(CommandResponseWriter responseWriter, IndexStore indexStore)
    {
        this.responseWriter = responseWriter;
        this.indexStore = indexStore;

        taskIndex = new Long2BytesHashIndex(indexStore, 100, 1, INDEX_VALUE_LENGTH);
        indexSnapshotSupport = new HashIndexSnapshotSupport<>(taskIndex, indexStore);
    }

    @Override
    public SnapshotSupport getStateResource()
    {
        return indexSnapshotSupport;
    }

    public void onOpen(StreamProcessorContext context)
    {
        streamId = context.getSourceStream().getId();
    }

    @Override
    public EventProcessor onEvent(LoggedEvent event)
    {
        eventPosition = event.getPosition();
        eventKey = event.getLongKey();

        event.readMetadata(sourceEventMetadata);

        taskEvent.reset();
        event.readValue(taskEvent);

        EventProcessor eventProcessor = null;

        switch (taskEvent.getEventType())
        {
            case CREATE:
                eventProcessor = createTaskProcessor;
                break;
            case LOCK:
                eventProcessor = lockTaskProcessor;
                break;
            case COMPLETE:
                eventProcessor = completeTaskProcessor;
                break;

            default:
                break;
        }

        return eventProcessor;
    }

    @Override
    public void afterEvent()
    {
        taskEvent.reset();
    }

    class CreateTaskProcessor implements EventProcessor
    {

        @Override
        public void processEvent()
        {
            taskEvent.setEventType(TaskEventType.CREATED);
        }

        @Override
        public boolean executeSideEffects()
        {
            boolean success = true;

            success = responseWriter.brokerEventMetadata(sourceEventMetadata)
                .topicId(streamId)
                .longKey(eventKey)
                .eventWriter(taskEvent)
                .tryWriteResponse();

            return success;
        }

        @Override
        public long writeEvent(LogStreamWriter writer)
        {
            targetEventMetadata.reset();
            targetEventMetadata
                .protocolVersion(Constants.PROTOCOL_VERSION)
                .eventType(TASK_EVENT);

            // TODO: targetEventMetadata.raftTermId(raftTermId);

            return writer.key(eventKey)
                .metadataWriter(targetEventMetadata)
                .valueWriter(taskEvent)
                .tryWrite();
        }

        @Override
        public void updateState()
        {
            indexWriter.write(eventKey, TaskEventType.CREATED, -1);
        }
    }

    class LockTaskProcessor implements EventProcessor
    {
        protected boolean isLocked;

        @Override
        public void processEvent()
        {
            isLocked = false;

            indexAccessor.wrapIndexKey(eventKey);
            final int typeId = indexAccessor.getTypeId();

            if (typeId == TaskEventType.CREATED.id() && taskEvent.getLockTime() > 0)
            {
                taskEvent.setEventType(TaskEventType.LOCKED);
                isLocked = true;
            }

            if (!isLocked)
            {
                taskEvent.setEventType(TaskEventType.LOCK_FAILED);
            }
        }

        @Override
        public boolean executeSideEffects()
        {
            boolean success = true;

            if (isLocked)
            {
                // TODO send response with full duplex protocol
                success = responseWriter
                    .brokerEventMetadata(sourceEventMetadata)
                    .topicId(streamId)
                    .longKey(eventKey)
                    .eventWriter(taskEvent)
                    .tryWriteResponse();
            }
            return success;
        }

        @Override
        public long writeEvent(LogStreamWriter writer)
        {
            targetEventMetadata.reset();
            targetEventMetadata
                .protocolVersion(Constants.PROTOCOL_VERSION)
                .eventType(TASK_EVENT);

            // TODO: targetEventMetadata.raftTermId(raftTermId);

            return writer.key(eventKey)
                .metadataWriter(targetEventMetadata)
                .valueWriter(taskEvent)
                .tryWrite();
        }

        @Override
        public void updateState()
        {
            if (isLocked)
            {
                indexWriter.write(eventKey, TaskEventType.LOCKED, taskEvent.getLockOwner());
            }
        }
    }

    class CompleteTaskProcessor implements EventProcessor
    {
        protected boolean isCompleted;

        @Override
        public void processEvent()
        {
            isCompleted = false;

            indexAccessor.wrapIndexKey(eventKey);
            final int typeId = indexAccessor.getTypeId();
            final int lockOwner = indexAccessor.getLockOwner();

            if (typeId == TaskEventType.LOCKED.id() && lockOwner == taskEvent.getLockOwner())
            {
                taskEvent.setEventType(TaskEventType.COMPLETED);
                isCompleted = true;
            }

            if (!isCompleted)
            {
                taskEvent.setEventType(TaskEventType.COMPLETE_FAILED);
            }
        }

        @Override
        public boolean executeSideEffects()
        {
            final boolean success = responseWriter
                .brokerEventMetadata(sourceEventMetadata)
                .topicId(streamId)
                .longKey(eventKey)
                .eventWriter(taskEvent)
                .tryWriteResponse();

            return success;
        }

        @Override
        public long writeEvent(LogStreamWriter writer)
        {
            targetEventMetadata.reset();
            targetEventMetadata
                .protocolVersion(Constants.PROTOCOL_VERSION)
                .eventType(TASK_EVENT);

            // TODO: targetEventMetadata.raftTermId(raftTermId);

            return writer.key(eventKey)
                .metadataWriter(targetEventMetadata)
                .valueWriter(taskEvent)
                .tryWrite();
        }

        @Override
        public void updateState()
        {
            if (isCompleted)
            {
                indexWriter.write(eventKey, TaskEventType.COMPLETED, -1);
            }
        }
    }

    class IndexAccessor
    {
        static final int MISSING_VALUE_TYPE_ID = -1;
        static final int MISSING_VALUE_LOCK_OWNER_ID = -1;

        protected final UnsafeBuffer indexValueReadBuffer = new UnsafeBuffer(new byte[INDEX_VALUE_LENGTH]);

        protected boolean isRead = false;

        void wrapIndexKey(long key)
        {
            isRead = false;

            final byte[] indexValue = taskIndex.get(key);
            if (indexValue != null)
            {
                indexValueReadBuffer.wrap(indexValue);

                isRead = true;
            }
        }

        public int getLockOwner()
        {
            int lockOwner = MISSING_VALUE_LOCK_OWNER_ID;

            if (isRead)
            {
                lockOwner = indexValueReadBuffer.getInt(INDEX_LOCK_OWNER_OFFSET);
            }
            return lockOwner;
        }

        public int getTypeId()
        {
            int typeId = MISSING_VALUE_TYPE_ID;

            if (isRead)
            {
                typeId = indexValueReadBuffer.getInt(INDEX_STATE_OFFSET);
            }
            return typeId;
        }
    }

    class IndexWriter
    {
        protected final UnsafeBuffer indexValueWriteBuffer = new UnsafeBuffer(new byte[INDEX_VALUE_LENGTH]);

        protected void write(long eventKey, TaskEventType eventType, int lockOwner)
        {
            indexValueWriteBuffer.putInt(INDEX_STATE_OFFSET, eventType.id());
            indexValueWriteBuffer.putInt(INDEX_LOCK_OWNER_OFFSET, lockOwner);

            taskIndex.put(eventKey, indexValueWriteBuffer.byteArray());
        }
    }

    public static MetadataFilter eventFilter()
    {
        return (m) -> m.getEventType() == EventType.TASK_EVENT;
    }

}
