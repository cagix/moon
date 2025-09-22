(ns gdl.backends.gdx.extends.files
  (:require [gdl.files]
            [gdl.files.file-handle])
  (:import (com.badlogic.gdx Files)
           (com.badlogic.gdx.files FileHandle)))

(extend-type Files
  gdl.files/Files
  (internal [this path]
    (.internal this path)))

(extend-type FileHandle
  gdl.files.file-handle/FileHandle
  (list [this]
    (.list this))
  (directory? [this]
    (.isDirectory this))
  (extension [this]
    (.extension this))
  (path [this]
    (.path this)))
