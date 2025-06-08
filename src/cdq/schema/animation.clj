(ns cdq.schema.animation
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/animation [_ _schemas]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])
