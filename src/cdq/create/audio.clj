(ns cdq.create.audio
  (:require [cdq.ctx.audio :as audio]
            [clojure.edn :as edn]
            [clojure.gdx.files :as files]
            [clojure.java.io :as io]))

(defn do! [ctx]
  (assoc ctx :ctx/audio (let [audio (:clojure.gdx/audio (:ctx/gdx ctx))
                              {:keys [sound-names
                                      path-format]} (:cdq.audio/config (:ctx/config ctx))
                              sound-name->file-handle (->> sound-names
                                                           io/resource
                                                           slurp
                                                           edn/read-string
                                                           (map (fn [sound-name]
                                                                  [sound-name
                                                                   (files/internal (:ctx/files ctx)
                                                                                   (format path-format sound-name))])))]
                          (audio/create audio sound-name->file-handle))))
