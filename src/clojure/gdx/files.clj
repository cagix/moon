(ns clojure.gdx.files
  (:import (com.badlogic.gdx Gdx)))

(defn internal
  "Convenience method that returns a {@link FileType#Internal} file handle."
  [path]
  (.internal Gdx/files path))
