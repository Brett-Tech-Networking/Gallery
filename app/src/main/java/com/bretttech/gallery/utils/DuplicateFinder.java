package com.bretttech.gallery.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.graphics.BitmapCompat;

import com.bretttech.gallery.ui.pictures.Image;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for finding duplicate or similar images using multiple strategies.
 */
public class DuplicateFinder {
    private static final String TAG = "DuplicateFinder";
    private final Context context;
    private final ExecutorService executorService;

    private static final int HASH_SIZE = 8; // 8x8 for perceptual hash
    private static final int SIMILARITY_THRESHOLD = 5; // Hamming distance threshold

    public interface DuplicateFinderCallback {
        void onDuplicatesFound(List<DuplicateGroup> duplicateGroups);
        void onError(String error);
    }

    /**
     * Represents a group of duplicate/similar images.
     */
    public static class DuplicateGroup {
        public final List<Image> images;
        public final int matchType; // 0: exact hash, 1: perceptual similarity

        public DuplicateGroup(List<Image> images, int matchType) {
            this.images = images;
            this.matchType = matchType;
        }
    }

    public DuplicateFinder(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Finds duplicates among a list of images.
     * Uses both exact hash matching and perceptual hashing for similar images.
     */
    public void findDuplicates(List<Image> images, DuplicateFinderCallback callback) {
        executorService.execute(() -> {
            try {
                List<DuplicateGroup> duplicates = new ArrayList<>();

                // First pass: Find exact duplicates using file hashing
                Map<String, List<Image>> exactHashMap = new HashMap<>();
                for (Image image : images) {
                    String hash = getFileHash(image.getUri());
                    if (hash != null) {
                        exactHashMap.computeIfAbsent(hash, k -> new ArrayList<>()).add(image);
                    }
                }

                // Add groups with more than one image
                for (List<Image> group : exactHashMap.values()) {
                    if (group.size() > 1) {
                        duplicates.add(new DuplicateGroup(group, 0));
                    }
                }

                // Second pass: Find perceptually similar images
                Map<String, List<Image>> perceptualHashMap = new HashMap<>();
                for (Image image : images) {
                    // Skip if already in an exact duplicate group
                    if (isInGroup(image, duplicates)) {
                        continue;
                    }

                    String pHash = getPerceptualHash(image.getUri());
                    if (pHash != null) {
                        // Find similar hashes
                        for (String existingHash : perceptualHashMap.keySet()) {
                            if (hammingDistance(pHash, existingHash) <= SIMILARITY_THRESHOLD) {
                                perceptualHashMap.get(existingHash).add(image);
                                pHash = null;
                                break;
                            }
                        }

                        if (pHash != null) {
                            perceptualHashMap.computeIfAbsent(pHash, k -> new ArrayList<>()).add(image);
                        }
                    }
                }

                // Add perceptual duplicate groups
                for (List<Image> group : perceptualHashMap.values()) {
                    if (group.size() > 1) {
                        duplicates.add(new DuplicateGroup(group, 1));
                    }
                }

                callback.onDuplicatesFound(duplicates);
            } catch (Exception e) {
                Log.e(TAG, "Error finding duplicates: " + e.getMessage(), e);
                callback.onError("Failed to find duplicates: " + e.getMessage());
            }
        });
    }

    /**
     * Gets the exact file hash (SHA-256).
     */
    private String getFileHash(Uri imageUri) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;

            try (var is = context.getContentResolver().openInputStream(imageUri)) {
                if (is == null) return null;

                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.e(TAG, "Error computing file hash: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the perceptual hash using average hash algorithm.
     * This is more resilient to image transformations (resize, compress, etc.)
     */
    private String getPerceptualHash(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
            if (bitmap == null) return null;

            // Resize to HASH_SIZE x HASH_SIZE
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, HASH_SIZE, HASH_SIZE, true);

            // Convert to grayscale and calculate average brightness
            long[] pixelSum = new long[3];
            for (int i = 0; i < resized.getWidth(); i++) {
                for (int j = 0; j < resized.getHeight(); j++) {
                    int pixel = resized.getPixel(i, j);
                    pixelSum[0] += (pixel >> 16) & 0xFF; // R
                    pixelSum[1] += (pixel >> 8) & 0xFF;  // G
                    pixelSum[2] += pixel & 0xFF;          // B
                }
            }

            int totalPixels = HASH_SIZE * HASH_SIZE;
            long avgBrightness = (pixelSum[0] + pixelSum[1] + pixelSum[2]) / (3L * totalPixels);

            // Create hash based on which pixels are above average
            StringBuilder hash = new StringBuilder();
            for (int i = 0; i < resized.getWidth(); i++) {
                for (int j = 0; j < resized.getHeight(); j++) {
                    int pixel = resized.getPixel(i, j);
                    long brightness = ((pixel >> 16) & 0xFF) + ((pixel >> 8) & 0xFF) + (pixel & 0xFF);
                    hash.append(brightness > avgBrightness ? "1" : "0");
                }
            }

            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            if (!resized.isRecycled()) {
                resized.recycle();
            }

            return hash.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error computing perceptual hash: " + e.getMessage());
            return null;
        }
    }

    /**
     * Calculates Hamming distance between two binary hashes.
     */
    private int hammingDistance(String hash1, String hash2) {
        if (hash1.length() != hash2.length()) {
            return Integer.MAX_VALUE;
        }

        int distance = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                distance++;
            }
        }
        return distance;
    }

    /**
     * Checks if an image is already in a duplicate group.
     */
    private boolean isInGroup(Image image, List<DuplicateGroup> groups) {
        for (DuplicateGroup group : groups) {
            for (Image img : group.images) {
                if (img.getUri().equals(image.getUri())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
