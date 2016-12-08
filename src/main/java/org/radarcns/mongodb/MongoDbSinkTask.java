package org.radarcns.mongodb;

import com.google.common.base.Strings;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.bson.Document;
import org.radarcns.serialization.AggregatedAccelerationRecordConverter;
import org.radarcns.serialization.DoubleAggregatedRecordConverter;
import org.radarcns.serialization.RecordConverter;
import org.radarcns.util.Monitor;
import org.radarcns.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.radarcns.mongodb.MongoDbSinkConnector.BUFFER_CAPACITY;

/**
 * Created by Francesco Nobilia on 28/11/2016.
 */
public class MongoDbSinkTask extends SinkTask {

    private static final Logger log = LoggerFactory.getLogger(MongoDbSinkTask.class);

    private final AtomicInteger count;

    private BlockingQueue<SinkRecord> buffer;
    // Assuming record sizes of 1 kB, we default to a 20 MB buffer
    private static final int DEFAULT_BUFFER_CAPACITY = 20_000;

    private MongoDbWriter writer;

    private Timer timer;

    public MongoDbSinkTask() {
        count = new AtomicInteger(0);
    }

    @Override
    public String version() {
        return new MongoDbSinkConnector().version();
    }

    @Override
    public void start(Map<String, String> props) {
        if(validateConfig(props)){
            int bufferCapacity = Utility.getInt(props, BUFFER_CAPACITY, DEFAULT_BUFFER_CAPACITY);
            buffer = new ArrayBlockingQueue<>(bufferCapacity);

            List<RecordConverter<Document>> mongoConverters = Arrays.asList(
                    new AggregatedAccelerationRecordConverter(),
                    new DoubleAggregatedRecordConverter());
            writer = new MongoDbWriter(props, buffer, mongoConverters);
            writer.start();

            timer = new Timer();
            timer.schedule(new Monitor(log, count, "have been processed"), 0, 30000);
        }
    }

    @Override
    public void put(Collection<SinkRecord> sinkRecords) {
        for (SinkRecord record : sinkRecords) {
            buffer.add(record);
            count.incrementAndGet();
        }
    }

    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> offsets) {
        writer.flush(offsets);
    }

    @Override
    public void stop() {
        timer.purge();
        writer.shutdown();
    }

    private boolean validateConfig(Map<String, String> config) {
        for (String key : Utility.stringToSet(config.get(MongoDbSinkConnector.MUST_HAVE))) {
            if (Strings.isNullOrEmpty(config.get(key))){
                log.error("MongoDbSinkTask cannot be created. {} is not defined", key);
                throw new ConnectException("MongoDbSinkTask cannot be created. " + key
                        + " is not defined");
            }
        }

        return true;
    }
}