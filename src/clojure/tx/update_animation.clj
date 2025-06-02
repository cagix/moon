(ns clojure.tx.update-animation
  (:require [clojure.animation :as animation]
            [clojure.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/update-animation [[_ eid animation] {:keys [ctx/delta-time]}]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc :entity/animation (animation/tick animation delta-time)))))
