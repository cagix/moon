(ns moon.schema.image
  (:require [moon.schema :as schema]))

(defmethod schema/form :s/image [_]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])
