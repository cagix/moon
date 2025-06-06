(ns cdq.tx.sound
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [gdl.audio :as audio]))

(defmethod do! :tx/sound [[_ sound-name]
                          {:keys [ctx/audio]}]
  (->> sound-name
       (format "sounds/%s.wav")
       (audio/play-sound! audio))
  nil)
