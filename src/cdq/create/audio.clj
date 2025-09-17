(ns cdq.create.audio
  (:require [cdq.audio]
            [clojure.audio :as audio]
            [clojure.audio.sound :as sound]
            [clojure.files :as files]))

(defn do!
  [{:keys [ctx/gdx]
    :as ctx}
   {:keys [sound-names
           path-format]}]
  (assoc ctx :ctx/audio (let [{:keys [clojure.gdx/audio
                                      clojure.gdx/files]} gdx
                              sounds (into {}
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
