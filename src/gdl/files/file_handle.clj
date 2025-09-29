(ns gdl.files.file-handle
  "
  /** Represents a file or directory on the filesystem, classpath, Android app storage, or Android assets directory. FileHandles are
  * created via a {@link Files} instance.
  *
  * Because some of the file types are backed by composite files and may be compressed (for example, if they are in an Android .apk
  * or are found via the classpath), the methods for extracting a {@link #path()} or {@link #file()} may not be appropriate for all
  * types. Use the Reader or Stream methods here to hide these dependencies from your platform independent code. "
  (:refer-clojure :exclude [list])
  (:import (com.badlogic.gdx.files FileHandle)))

(defn list [^FileHandle this]
  (.list this))

(defn directory? [^FileHandle this]
  (.isDirectory this))

(defn extension [^FileHandle this]
  (.extension this))

(defn path [^FileHandle this]
  (.path this))

; minimal API here only! ?
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
