(ns cdq.tx.sound
  (:require [gdl.audio.sound :as sound]))

(defn do! [{:keys [ctx/config
                   ctx/assets]}
           sound-name]
  (->> sound-name
       (format (:sound-path-format config))
       assets
       sound/play!))
