(ns cdq.create.audio
  (:require [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.files :as files]
            [clojure.java.io :as io]))

(defn do! [ctx]
  (assoc ctx :ctx/audio (let [{:keys [sound-names
                                      path-format]} (:cdq.audio/config (:ctx/config ctx))]
                          (into {}
                                (for [sound-name (->> sound-names io/resource slurp edn/read-string)
                                      :let [path (format path-format sound-name)]]
                                  [sound-name
                                   (audio/sound (gdx/audio) (files/internal (:ctx/files ctx) path))])))))
