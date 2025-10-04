(ns clojure.gdx.files
  (:require clojure.files))

(extend-type com.badlogic.gdx.Files
  clojure.files/Files
  (internal [this path]
    (.internal this path)))
