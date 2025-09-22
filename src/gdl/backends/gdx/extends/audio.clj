(ns gdl.backends.gdx.extends.audio
  (:require [gdl.audio]
            [gdl.audio.sound])
  (:import (com.badlogic.gdx Audio)
           (com.badlogic.gdx.audio Sound)))

(extend-type Audio
  gdl.audio/Audio
  (sound [this file-handle]
    (.newSound this file-handle)))

(extend-type Sound
  gdl.audio.sound/Sound
  (play! [this]
    (.play this))
  (dispose! [this]
    (.dispose this)))
