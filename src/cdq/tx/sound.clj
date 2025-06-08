(ns cdq.tx.sound
  (:require [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/sound [[_ sound-name] _ctx]
  [:world.event/sound sound-name])
