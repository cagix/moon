(ns cdq.create.audio
  (:require [cdq.impl.audio]
            [gdl.files :as files]))

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
                          (cdq.impl.audio/create audio sound-names->file-handles))))
