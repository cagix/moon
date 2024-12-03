(ns clojure.gdx.audio
  (:import (com.badlogic.gdx.audio Sound)))

(defn play [sound]
  (Sound/.play sound))
