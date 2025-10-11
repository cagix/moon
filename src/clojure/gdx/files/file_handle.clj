(ns clojure.gdx.files.file-handle
  (:refer-clojure :exclude [list])
  (:import (com.badlogic.gdx.files FileHandle)))

(defn list [^FileHandle fh]
  (.list fh))

(defn directory? [^FileHandle fh]
  (.isDirectory fh))

(defn extension [^FileHandle fh]
  (.extension fh))

(defn path [^FileHandle fh]
  (.path fh))
