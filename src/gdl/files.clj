(ns gdl.files)

(defprotocol Files
  (internal [_ path] "Convenience method that returns a {@link FileType#Internal} file handle."))

(extend-type com.badlogic.gdx.Files
  Files
  (internal [this path]
    (.internal this path)))
