(ns cdq.application.create.audio
  (:require [gdl.files :as files]))

(defn do!
  [{:keys [ctx/gdx]
    :as ctx}
   {:keys [audio-impl
           sound-names
           path-format]}]
  (assoc ctx :ctx/audio (let [sound-names->file-handles
                              (into {}
                                    (for [sound-name sound-names]
                                      [sound-name
                                       (files/internal (:files gdx) (format path-format sound-name))]))]
                          (audio-impl (:audio gdx) sound-names->file-handles))))
