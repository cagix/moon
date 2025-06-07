(ns cdq.schema.image
  (:require [cdq.schema :as schema]
            [gdl.graphics :as g]))

(defmethod schema/malli-form :s/image [_ _schemas]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(defmethod schema/edn->value :s/image [_
                                       edn
                                       {:keys [ctx/graphics]}]
  (g/edn->sprite graphics edn))
