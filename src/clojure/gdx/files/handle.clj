(ns clojure.gdx.files.handle
  (:refer-clojure :exclude [list])
  (:import (com.badlogic.gdx.files FileHandle)))

(defn list       [file-handle] (FileHandle/.list        file-handle))
(defn directory? [file-handle] (FileHandle/.isDirectory file-handle))
(defn extension  [file-handle] (FileHandle/.extension   file-handle))
(defn path       [file-handle] (FileHandle/.path        file-handle))
