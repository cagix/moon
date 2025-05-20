(ns cdq.tx.sound
  (:require [gdl.audio.sound :as sound]))

(defn do! [{:keys [ctx/sound-path-format
                   ctx/assets]}
           sound-name]
  (->> sound-name
       (format sound-path-format)
       assets
       sound/play!))
