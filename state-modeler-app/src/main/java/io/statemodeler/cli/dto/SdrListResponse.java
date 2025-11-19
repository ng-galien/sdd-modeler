package io.statemodeler.cli.dto;

import io.statemodeler.repository.SdrMetadata;
import java.time.Instant;
import java.util.List;

public record SdrListResponse(List<SdrMetadataDto> sdrs, int total) {

    public record SdrMetadataDto(
            String name, String version, String hash, String sdrVersion, String buildFingerprint, Instant createdAt) {

        public static SdrMetadataDto from(SdrMetadata metadata) {
            return new SdrMetadataDto(
                    metadata.modelName(),
                    metadata.modelVersion(),
                    metadata.schemaHash(),
                    metadata.sdrVersion(),
                    metadata.buildFingerprint(),
                    metadata.createdAt());
        }
    }

    public static SdrListResponse from(List<SdrMetadata> metadataList) {
        List<SdrMetadataDto> dtos =
                metadataList.stream().map(SdrMetadataDto::from).toList();
        return new SdrListResponse(dtos, dtos.size());
    }
}
