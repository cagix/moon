(ns clojure.gdx.files.file-handle
  (:refer-clojure :exclude [list]))

(defn directory? [fh]
  (.isDirectory fh))

(defn list [fh]
  (.list fh))

(defn extension [fh]
  (.extension fh))

(defn path [fh]
  (.path fh))
