(ns cdq.ctx.audio
  (:require [clojure.gdx.audio :as audio]
            [clojure.gdx.audio.sound :as sound]))

(defn create [audio sound-name->file-handle]
  (into {}
        (for [[sound-name file-handle] sound-name->file-handle]
          [sound-name
           (audio/sound audio file-handle)])))

(defn all-sounds [sounds]
  (map first sounds))

(defn play-sound! [sounds sound-name]
  (assert (contains? sounds sound-name) (str sound-name))
  (sound/play! (get sounds sound-name)))
