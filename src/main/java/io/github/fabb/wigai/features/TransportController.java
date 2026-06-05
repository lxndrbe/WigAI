package io.github.fabb.wigai.features;

import io.github.fabb.wigai.bitwig.BitwigApiFacade;
import io.github.fabb.wigai.common.Logger;
import io.github.fabb.wigai.common.error.BitwigApiException;
import io.github.fabb.wigai.common.error.ErrorCode;

/**
 * Controller class for transport control features.
 * Bridges between MCP tools and Bitwig API operations for transport control.
 */
public class TransportController {
    private final BitwigApiFacade bitwigApiFacade;
    private final Logger logger;

    /**
     * Creates a new TransportController instance.
     *
     * @param bitwigApiFacade The facade for Bitwig API interactions
     * @param logger          The logger for logging operations
     */
    public TransportController(BitwigApiFacade bitwigApiFacade, Logger logger) {
        this.bitwigApiFacade = bitwigApiFacade;
        this.logger = logger;
    }

    /**
     * Starts the transport playback.
     *
     * @return A message indicating the operation result
     */
    public String startTransport() {
        try {
            logger.info("TransportController: Starting transport playback");
            bitwigApiFacade.startTransport();
            return "Transport playback started.";
        } catch (Exception e) {
            logger.info("TransportController: Error starting transport playback: " + e.getMessage());
            throw new BitwigApiException(ErrorCode.TRANSPORT_ERROR, "startTransport", "Failed to start transport playback", e);
        }
    }

    /**
     * Stops the transport playback.
     *
     * @return A message indicating the operation result
     */
    public String stopTransport() {
        try {
            logger.info("TransportController: Stopping transport playback");
            bitwigApiFacade.stopTransport();
            return "Transport playback stopped.";
        } catch (Exception e) {
            logger.info("TransportController: Error stopping transport playback: " + e.getMessage());
            throw new BitwigApiException(ErrorCode.TRANSPORT_ERROR, "stopTransport", "Failed to stop transport playback", e);
        }
    }

    /**
     * Configures various transport settings.
     *
     * @param tempo     Optional tempo in BPM
     * @param metronome Optional metronome toggle
     * @param loop      Optional arranger loop toggle
     * @param recordArm Optional arranger record-arm toggle
     * @return A message indicating configuration results
     */
    public String configureTransport(Double tempo, Boolean metronome, Boolean loop, Boolean recordArm) {
        StringBuilder sb = new StringBuilder("Transport configuration updated:");
        if (tempo != null) {
            bitwigApiFacade.setTempo(tempo);
            sb.append(" tempo=").append(tempo).append(" BPM");
        }
        if (metronome != null) {
            bitwigApiFacade.setMetronome(metronome);
            sb.append(" metronome=").append(metronome);
        }
        if (loop != null) {
            bitwigApiFacade.setLoop(loop);
            sb.append(" loop=").append(loop);
        }
        if (recordArm != null) {
            bitwigApiFacade.setRecordArm(recordArm);
            sb.append(" recordArm=").append(recordArm);
        }
        return sb.toString();
    }
}
