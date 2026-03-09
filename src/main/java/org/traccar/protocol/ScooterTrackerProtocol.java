package org.traccar.protocol;

import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.traccar.BaseProtocol;
import org.traccar.CharacterDelimiterFrameDecoder;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;

import jakarta.inject.Inject;

public class ScooterTrackerProtocol extends BaseProtocol {

    @Inject
    public ScooterTrackerProtocol(Config config) {
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new CharacterDelimiterFrameDecoder(4096, '$'));
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new StringDecoder());
                pipeline.addLast(new ScooterTrackerProtocolDecoder(ScooterTrackerProtocol.this));
            }
        });
    }

}
