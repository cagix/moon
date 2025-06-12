(ns gdx.audio
  (:import (com.badlogic.gdx Audio)))

(defn new-sound [audio file-handle]
  (.newSound audio file-handle))
