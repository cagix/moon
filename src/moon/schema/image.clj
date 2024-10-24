(ns moon.schema.image
  (:require [moon.graphics :as g]
            [moon.schema :as schema]))

(defmethod schema/form :s/image [_]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(defmethod schema/edn->value :s/image [_ image]
  (g/edn->image image))
