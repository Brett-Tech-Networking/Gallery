package com.bretttech.gallery.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * Utility class for securely shredding (deleting) image files.
 * Overwrites file content before deletion for enhanced privacy.
 */
public class ImageShredder {
    private static final String TAG = "ImageShredder";
    private static final int OVERWRITE_PASSES = 3; // Number of times to overwrite

    /**
     * Securely shreds an image file by overwriting its content before deletion.
     *
     * @param context Application context
     * @param imageUri URI of the image to shred
     * @return true if successfully shredded, false otherwise
     */
    public static boolean shredImage(Context context, Uri imageUri) {
        try {
            // First, overwrite the file content
            if (!overwriteFile(context, imageUri)) {
                Log.w(TAG, "Failed to overwrite file, but will attempt deletion");
            }

            // Then delete the file
            return deleteFile(context, imageUri);
        } catch (Exception e) {
            Log.e(TAG, "Error shredding image: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Overwrites file content with random data multiple times.
     */
    private static boolean overwriteFile(Context context, Uri imageUri) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            
            // Get file size
            long fileSize = getFileSize(context, imageUri);
            if (fileSize <= 0) {
                return false;
            }

            Random random = new Random();
            byte[] overwriteBuffer;

            // Perform multiple overwrite passes
            for (int pass = 0; pass < OVERWRITE_PASSES; pass++) {
                try (OutputStream os = contentResolver.openOutputStream(imageUri)) {
                    if (os == null) {
                        return false;
                    }

                    // Write random data
                    long bytesWritten = 0;
                    int bufferSize = Math.min((int) fileSize, 8192);
                    overwriteBuffer = new byte[bufferSize];

                    while (bytesWritten < fileSize) {
                        random.nextBytes(overwriteBuffer);
                        int toWrite = (int) Math.min(bufferSize, fileSize - bytesWritten);
                        os.write(overwriteBuffer, 0, toWrite);
                        bytesWritten += toWrite;
                    }
                }
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error overwriting file: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Deletes a file from the content provider.
     */
    private static boolean deleteFile(Context context, Uri imageUri) {
        try {
            // Try using content resolver first (safer for MediaStore)
            int deleted = context.getContentResolver().delete(imageUri, null, null);
            if (deleted > 0) {
                Log.d(TAG, "File deleted via ContentResolver");
                return true;
            }

            // Fallback: try direct file deletion
            String path = getFilePath(context, imageUri);
            if (path != null) {
                File file = new File(path);
                if (file.exists() && file.delete()) {
                    Log.d(TAG, "File deleted directly");
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gets the file size in bytes.
     */
    private static long getFileSize(Context context, Uri imageUri) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            
            // Try to get size from MediaStore
            try (var cursor = contentResolver.query(
                    imageUri,
                    new String[]{MediaStore.MediaColumns.SIZE},
                    null,
                    null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                    if (sizeIndex >= 0) {
                        return cursor.getLong(sizeIndex);
                    }
                }
            }

            // Try to get size by reading
            try (InputStream is = contentResolver.openInputStream(imageUri)) {
                if (is != null) {
                    return is.available();
                }
            }

            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Gets the file path from URI.
     */
    private static String getFilePath(Context context, Uri uri) {
        try {
            if (uri.getScheme() != null && uri.getScheme().equals("file")) {
                return uri.getPath();
            }

            // Try to get path from MediaStore
            String[] projection = {MediaStore.MediaColumns.DATA};
            try (var cursor = context.getContentResolver().query(uri, projection, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                    if (columnIndex >= 0) {
                        return cursor.getString(columnIndex);
                    }
                }
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting file path: " + e.getMessage());
            return null;
        }
    }
}
