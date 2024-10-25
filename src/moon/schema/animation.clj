(ns moon.schema.animation
  (:require [gdl.ui :as ui]
            [moon.entity.animation :refer [->animation]]
            [moon.graphics :as g]
            [moon.schema :as schema]
            [moon.schema.image :refer [image->button]]))

(defmethod schema/form :s/animation [_]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])

(defmethod schema/edn->value :s/animation [_ {:keys [frames frame-duration looping?]}]
  (->animation (map g/edn->image frames)
               :frame-duration frame-duration
               :looping? looping?))

(defmethod schema/widget :s/animation [_ animation]
  (ui/table {:rows [(for [image (:frames animation)]
                      (image->button image))]
             :cell-defaults {:pad 1}}))
