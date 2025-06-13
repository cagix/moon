(ns gdx.files.file-handle
  (:refer-clojure :exclude [list])
  (:import (com.badlogic.gdx.files FileHandle)))

(def list       FileHandle/.list)
(def directory? FileHandle/.isDirectory)
(def path       FileHandle/.path)
(def extension  FileHandle/.extension)
