(ns cdq.schema.animation
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/animation [_ schemas]
  (schema/malli-form [:s/map [:animation/frames
                              :animation/frame-duration
                              :animation/looping?]]
                     schemas))
