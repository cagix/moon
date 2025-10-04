(ns clojure.gdx.audio.sound
  (:require clojure.audio.sound))

(extend-type com.badlogic.gdx.audio.Sound
  clojure.audio.sound/Sound
  (play! [this]
    (.play this)))
