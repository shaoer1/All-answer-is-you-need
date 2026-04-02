package com.offlineqa.model;

import java.util.List;

public record UploadResponse(String docId, int chunkCount, List<String> ignoredHashes) {
}
