#include "opus_helper.h"
#include <string.h>
#include <math.h>

int opus_packetize_frame(
    const uint8_t *pcm_in,
    size_t pcm_len,
    uint8_t *packet_out,
    size_t max_out_len,
    uint32_t sequence_num,
    uint32_t timestamp
) {
    if (!pcm_in || !packet_out || max_out_len < 12 + pcm_len) {
        return -1;
    }

    // RTP Header format (RFC 3550) for VoIP stream:
    // V=2, P=0, X=0, CC=0, M=0, PT=111 (Opus dynamic payload type)
    packet_out[0] = 0x80;
    packet_out[1] = 0x6F; // PT=111

    // Sequence Number (16-bit big-endian)
    packet_out[2] = (uint8_t)((sequence_num >> 8) & 0xFF);
    packet_out[3] = (uint8_t)(sequence_num & 0xFF);

    // Timestamp (32-bit big-endian)
    packet_out[4] = (uint8_t)((timestamp >> 24) & 0xFF);
    packet_out[5] = (uint8_t)((timestamp >> 16) & 0xFF);
    packet_out[6] = (uint8_t)((timestamp >> 8) & 0xFF);
    packet_out[7] = (uint8_t)(timestamp & 0xFF);

    // SSRC identifier: 0x5741545A ('WATZ' in ASCII)
    packet_out[8]  = 0x57;
    packet_out[9]  = 0x41;
    packet_out[10] = 0x54;
    packet_out[11] = 0x5A;

    // Copy audio payload
    memcpy(packet_out + 12, pcm_in, pcm_len);
    return (int)(12 + pcm_len);
}

float opus_calculate_waveform_amplitude(const int16_t *samples, size_t count) {
    if (!samples || count == 0) return 0.0f;
    double sum = 0.0;
    for (size_t i = 0; i < count; ++i) {
        sum += (double)samples[i] * (double)samples[i];
    }
    double rms = sqrt(sum / (double)count);
    float normalized = (float)(rms / 32768.0);
    return normalized > 1.0f ? 1.0f : normalized;
}
