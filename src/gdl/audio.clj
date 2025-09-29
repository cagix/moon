(ns gdl.audio
  (:import (com.badlogic.gdx Audio)))

(defn new-sound [^Audio audio file-handle]
  (.newSound audio file-handle))
