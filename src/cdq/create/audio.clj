(ns cdq.create.audio
  (:require [clojure.gdx.audio :as audio]
            [clojure.gdx.files :as files]))

(defn do!
  [{:keys [ctx/gdx]
    :as ctx}
   {:keys [sound-names
           path-format]}]
  (assoc ctx :ctx/audio (let [{:keys [clojure.gdx/audio
                                      clojure.gdx/files]} gdx]
                          (into {}
                                (for [sound-name sound-names]
                                  [sound-name
                                   (audio/sound audio (files/internal files (format path-format sound-name)))])))))
