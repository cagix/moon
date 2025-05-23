(ns cdq.tx.sound
  (:require [cdq.g :as g]
            [gdl.audio.sound :as sound]))

(defn do! [ctx sound-name]
  (->> sound-name
       (format (g/config ctx :sound-path-format))
       (g/sound ctx)
       sound/play!))
