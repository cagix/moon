(ns cdq.tx.sound
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [gdl.assets :as assets]
            [gdl.audio.sound :as sound]))

(defmethod do! :tx/sound [[_ sound-name]
                          {:keys [ctx/assets]}]
  (->> sound-name
       (format "sounds/%s.wav")
       (assets/sound assets)
       sound/play!))
