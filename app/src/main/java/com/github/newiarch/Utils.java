package com.github.newiarch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

/** Junk drawer of utility methods. */
final class Utils {
  static final Charset US_ASCII = Charset.forName("US-ASCII");
  static final Charset UTF_8 = Charset.forName("UTF-8");
  public static final int IO_BUFFER_SIZE = 8 * 1024;

  private Utils() {
  }

  static String readFully(Reader reader) throws IOException {
    try {
      StringWriter writer = new StringWriter();
      char[] buffer = new char[1024];
      int count;
      while ((count = reader.read(buffer)) != -1) {
        writer.write(buffer, 0, count);
      }
      return writer.toString();
    } finally {
      reader.close();
    }
  }

  /**
   * Deletes the contents of {@code dir}. Throws an IOException if any file
   * could not be deleted, or if {@code dir} is not a readable directory.
   */
  static void deleteContents(File dir) throws IOException {
    File[] files = dir.listFiles();
    if (files == null) {
      throw new IOException("not a readable directory: " + dir);
    }
    for (File file : files) {
      if (file.isDirectory()) {
        deleteContents(file);
      }
      if (!file.delete()) {
        throw new IOException("failed to delete file: " + file);
      }
    }
  }

  static void closeQuietly(/*Auto*/Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (RuntimeException rethrown) {
        throw rethrown;
      } catch (Exception ignored) {
      }
    }
  }
  
  public static boolean isExternalStorageRemovable() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
          return Environment.isExternalStorageRemovable();
      }
      return true;
  }

  public static File getExternalCacheDir(Context context) {
      if (hasExternalCacheDir()) {
          return context.getExternalCacheDir();
      }

      // Before Froyo we need to construct the external cache dir ourselves
      final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
      return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
  }

  public static boolean hasExternalCacheDir() {
      return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
  }

}
