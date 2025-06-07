(ns cdq.schema.animation
  (:require [cdq.animation :as animation]
            [cdq.schema :as schema]
            [gdl.graphics :as g]))

(defmethod schema/malli-form :s/animation [_ _schemas]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])

(defmethod schema/edn->value :s/animation [_
                                           {:keys [frames frame-duration looping?]}
                                           {:keys [ctx/graphics]}]
  (animation/create (map #(g/edn->sprite graphics %) frames)
                    :frame-duration frame-duration
                    :looping? looping?))
