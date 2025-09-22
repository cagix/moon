(ns cdq.create.audio
  (:require [cdq.audio]
            [gdl.audio :as audio]
            [gdl.audio.sound :as sound]
            [gdl.files :as files]))

(defn- audio-impl [audio sound-names->file-handles]
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

(defn do!
  [{:keys [ctx/audio
           ctx/files]
    :as ctx}
   {:keys [sound-names
           path-format]}]
  (assoc ctx :ctx/audio (let [sound-names->file-handles
                              (into {}
                                    (for [sound-name sound-names]
                                      [sound-name
                                       (files/internal files (format path-format sound-name))]))]
                          (audio-impl audio sound-names->file-handles))))
