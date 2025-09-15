(ns cdq.create.audio
  (:require [cdq.ctx.audio :as audio]
            [clojure.edn :as edn]
            [clojure.gdx.files :as files]
            [clojure.java.io :as io]))

(defn do! [{:keys [ctx/config
                   ctx/gdx]
            :as ctx}]
  (assoc ctx :ctx/audio (let [{:keys [clojure.gdx/audio
                                      clojure.gdx/files]} gdx
                              {:keys [sound-names
                                      path-format]} (:cdq.audio/config config)
                              sound-name->file-handle (->> sound-names
                                                           io/resource
                                                           slurp
                                                           edn/read-string
                                                           (map (fn [sound-name]
                                                                  [sound-name
                                                                   (files/internal files
                                                                                   (format path-format sound-name))])))]
                          (audio/create audio sound-name->file-handle))))
