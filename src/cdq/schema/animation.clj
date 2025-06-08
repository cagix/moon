(ns cdq.schema.animation
  (:require [cdq.animation :as animation]
            [cdq.schema :as schema]))

(defmethod schema/malli-form :s/animation [_ _schemas]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])

(defmethod schema/edn->value :s/animation [_
                                           {:keys [frames frame-duration looping?]}
                                           _ctx]
  (animation/create frames
                    :frame-duration frame-duration
                    :looping? looping?))
