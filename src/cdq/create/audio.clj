(ns cdq.create.audio
  (:require [cdq.ctx.audio]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.files :as files]))

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
                          (reify cdq.ctx.audio/Audio
                            (all-sounds [_]
                              (map first sounds))

                            (play-sound! [_ sound-name]
                              (assert (contains? sounds sound-name) (str sound-name))
                              (sound/play! (get sounds sound-name)))))))
