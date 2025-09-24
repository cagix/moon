(ns clojure.files.file-handle
  "
/** Represents a file or directory on the filesystem, classpath, Android app storage, or Android assets directory. FileHandles are
 * created via a {@link Files} instance.
 *
 * Because some of the file types are backed by composite files and may be compressed (for example, if they are in an Android .apk
 * or are found via the classpath), the methods for extracting a {@link #path()} or {@link #file()} may not be appropriate for all
 * types. Use the Reader or Stream methods here to hide these dependencies from your platform independent code.
  "
  (:refer-clojure :exclude [list]))

(defprotocol FileHandle
  (list [_]
        "
	/** Returns the paths to the children of this directory. Returns an empty list if this file handle represents a file and not a
	 * directory. On the desktop, an {@link FileType#Internal} handle to a directory on the classpath will return a zero length
	 * array.
	 * @throws GdxRuntimeException if this file is an {@link FileType#Classpath} file. */
        "
        )
  (directory? [_]
              "

	/** Returns true if this file is a directory. Always returns false for classpath files. On Android, an
	 * {@link FileType#Internal} handle to an empty directory will return false. On the desktop, an {@link FileType#Internal}
	 * handle to a directory on the classpath will return false. */
              "

              )
  (extension [_]
             "

	/** Returns the file extension (without the dot) or an empty string if the file name doesn't contain a dot. */
             "
             )
  (path [_]
        "

	/** @return the path of the file as specified on construction, e.g. Gdx.files.internal("dir/file.png") -> dir/file.png.
	 *         backward slashes will be replaced by forward slashes. */
        "
        ))

(defn recursively-search
  "Returns all files in the folder (a file-handle) which match the set of extensions e.g. `#{\"png\" \"bmp\"}`."
  [folder extensions]
  (loop [[file & remaining] (list folder)
         result []]
    (cond (nil? file)
          result

          (directory? file)
          (recur (concat remaining (list file)) result)

          (extensions (extension file))
          (recur remaining (conj result (path file)))

          :else
          (recur remaining result))))
