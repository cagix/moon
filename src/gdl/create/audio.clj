(ns gdl.create.audio
  (:require [clojure.gdx.audio :as audio]
            [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.files :as files]
            [gdl.audio]
            [gdl.utils.disposable :as disposable]))

(defn create-audio [audio files sounds-to-load]
  ;(println "create-audio. (count sounds-to-load): " (count sounds-to-load))
  (let [sounds (into {}
                     (for [file sounds-to-load]
                       [file (audio/sound audio (files/internal files file))]))]
    (reify
      disposable/Disposable
      (dispose! [_]
        (do
         ;(println "Disposing sounds ...")
         (run! disposable/dispose! (vals sounds))))

      gdl.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        (sound/play! (get sounds path))))))
