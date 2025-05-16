(ns cdq.tx.sound
  (:require [cdq.ctx :as ctx]
            [gdl.audio.sound :as sound]))

(defn do! [sound-name]
  (->> sound-name
       (format ctx/sound-path-format)
       ctx/assets
       sound/play!))
