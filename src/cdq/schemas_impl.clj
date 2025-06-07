(ns cdq.schemas-impl
  (:require [cdq.animation :as animation]
            [cdq.val-max :as val-max]
            [cdq.create.db :refer [malli-form]]
            [cdq.schema :as schema]
            [gdl.graphics :as g]))

(defmethod malli-form :s/val-max [_ _schemas] val-max/schema)

(defmethod malli-form :s/sound [_ _schemas] :string)

(defmethod malli-form :s/image [_ _schemas]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(defmethod schema/edn->value :s/image [_
                                       edn
                                       {:keys [ctx/graphics]}]
  (g/edn->sprite graphics edn))

(defmethod malli-form :s/animation [_ _schemas]
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
