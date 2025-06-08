(ns cdq.schema.image
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/image [_ _schemas]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])
