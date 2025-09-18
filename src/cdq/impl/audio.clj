(ns cdq.impl.audio
  (:require [cdq.audio]
            [gdl.audio :as audio]
            [gdl.audio.sound :as sound]))

(defn create [audio sound-names->file-handles]
  (let [sounds (update-vals sound-names->file-handles
                            (fn [file-handle]
                              (audio/sound audio file-handle)))]
    (reify cdq.audio/Audio
      (dispose! [_]
        (run! sound/dispose! (vals sounds)))

      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ sound-name]
        (assert (contains? sounds sound-name) (str sound-name))
        (sound/play! (get sounds sound-name))))))
