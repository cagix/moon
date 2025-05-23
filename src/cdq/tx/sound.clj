(ns cdq.tx.sound
  (:require [cdq.g :as g]
            [gdl.audio.sound :as sound]))

(defn do! [{:keys [ctx/assets] :as ctx} sound-name]
  (->> sound-name
       (format (g/config ctx :sound-path-format))
       assets
       sound/play!))
