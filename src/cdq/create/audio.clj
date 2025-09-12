(ns cdq.create.audio
  (:require [cdq.ctx.audio :as audio]
            [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.files :as files]
            [clojure.java.io :as io]))

(defn do! [ctx]
  (assoc ctx :ctx/audio (let [{:keys [sound-names
                                      path-format]} (:cdq.audio/config (:ctx/config ctx))
                              audio (gdx/audio)
                              sound-name->file-handle (->> sound-names
                                                           io/resource
                                                           slurp
                                                           edn/read-string
                                                           (map (fn [sound-name]
                                                                  [sound-name
                                                                   (files/internal (:ctx/files ctx)
                                                                                   (format path-format sound-name))])))]
                          (audio/create audio sound-name->file-handle))))
