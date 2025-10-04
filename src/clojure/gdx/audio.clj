(ns clojure.gdx.audio
  (:require clojure.audio))

(extend-type com.badlogic.gdx.Audio
  clojure.audio/Audio
  (new-sound [this file-handle]
    (.newSound this file-handle)))
