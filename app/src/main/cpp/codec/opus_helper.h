#ifndef OPUS_HELPER_H
#define OPUS_HELPER_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    uint32_t sample_rate;
    uint8_t channels;
    uint16_t frame_size_ms;
    uint32_t bitrate_bps;
} OpusAudioConfig;

/**
 * Validates and packetizes a raw audio frame into an encapsulated RTP/VoIP packet.
 */
int opus_packetize_frame(
    const uint8_t *pcm_in,
    size_t pcm_len,
    uint8_t *packet_out,
    size_t max_out_len,
    uint32_t sequence_num,
    uint32_t timestamp
);

/**
 * Calculates real-time audio amplitude for voice waveform visualization.
 */
float opus_calculate_waveform_amplitude(const int16_t *samples, size_t count);

#ifdef __cplusplus
}
#endif

#endif // OPUS_HELPER_H
