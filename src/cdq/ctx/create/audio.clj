(ns cdq.ctx.create.audio
  (:require [cdq.audio :as audio]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def ^:private sound-names (->> "sounds.edn" io/resource slurp edn/read-string))
(def ^:private path-format "sounds/%s.wav")

(defn do! [{:keys [ctx/audio
                   ctx/files]
            :as ctx}]
  (assoc ctx :ctx/audio (audio/create audio
                                      files
                                      sound-names
                                      path-format)))
