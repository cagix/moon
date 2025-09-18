(ns cdq.create.audio
  (:require [cdq.audio]
            [gdl.audio :as audio]
            [gdl.audio.sound :as sound]
            [gdl.files :as files]))

(defn do!
  [{:keys [ctx/audio
           ctx/files]
    :as ctx}
   {:keys [sound-names
           path-format]}]
  (assoc ctx :ctx/audio (let [sounds (into {}
                                           (for [sound-name sound-names]
                                             [sound-name
                                              (audio/sound audio (files/internal files (format path-format sound-name)))]))]
                          (reify cdq.audio/Audio
                            (dispose! [_]
                              (run! com.badlogic.gdx.audio.Sound/.dispose (vals sounds)))

                            (all-sounds [_]
                              (map first sounds))

                            (play-sound! [_ sound-name]
                              (assert (contains? sounds sound-name) (str sound-name))
                              (sound/play! (get sounds sound-name)))))))
