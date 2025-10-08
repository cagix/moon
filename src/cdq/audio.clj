(ns cdq.audio
  (:import (com.badlogic.gdx Audio)
           (com.badlogic.gdx.audio Sound)))

(defn create [^Audio audio sound-names->file-handles]
  (update-vals sound-names->file-handles
               (partial Audio/.newSound audio)))

(defn sound-names [sounds]
  (map first sounds))

(defn play! [sounds sound-name]
  (assert (contains? sounds sound-name) (str sound-name))
  (Sound/.play (get sounds sound-name)))

(defn dispose! [sounds]
  (run! Sound/.dispose (vals sounds)))
