(ns cdq.create.audio
  (:require [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.files :as files]
            [clojure.java.io :as io]))

(defn do!
  [ctx
   {:keys [sounds path-format]}]
  (assoc ctx :ctx/audio (into {}
                              (for [sound-name (->> sounds io/resource slurp edn/read-string)
                                    :let [path (format path-format sound-name)]]
                                [sound-name
                                 (audio/sound (gdx/audio) (files/internal (gdx/files) path))]))))
