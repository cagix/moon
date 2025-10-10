(ns clojure.sound)

(defprotocol Sound
  (play! [_])
  (dispose! [_]))

(extend-type com.badlogic.gdx.audio.Sound
  Sound
  (play! [this]
    (.play this))
  (dispose! [this]
    (.dispose this)))
