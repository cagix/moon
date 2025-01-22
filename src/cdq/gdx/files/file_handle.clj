(ns cdq.gdx.files.file-handle
  (:refer-clojure :exclude [list])
  (:import com.badlogic.gdx.files.FileHandle))

(defn directory? [fh]
  (FileHandle/.isDirectory fh))

(defn list [fh]
  (FileHandle/.list fh))

(defn extension [fh]
  (FileHandle/.extension fh))

(defn path [fh]
  (FileHandle/.path fh))
