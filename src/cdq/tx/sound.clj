(ns cdq.tx.sound
  (:require [clojure.ctx.effect-handler :refer [do!]]
            [clojure.assets :as assets]
            [clojure.audio.sound :as sound]))

(defmethod do! :tx/sound [[_ sound-name]
                          {:keys [ctx/assets]}]
  (->> sound-name
       (format "sounds/%s.wav")
       (assets/sound assets)
       sound/play!))
