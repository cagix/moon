(ns ^:no-doc moon.schema.animation
  (:require [gdl.animation :as animation]
            [moon.schema :as schema]
            [gdl.ui :as ui]
            [moon.schema.image :as image]))

(defmethod schema/form :s/animation [_]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])

(defmethod schema/edn->value :s/animation [_ {:keys [frames frame-duration looping?]}]
  (animation/create (map image/edn->image frames)
                    :frame-duration frame-duration
                    :looping? looping?))

(defmethod schema/widget :s/animation [_ animation]
  (ui/table {:rows [(for [image (:frames animation)]
                      (ui/image-button (image/edn->image image)
                                       (fn on-clicked [])
                                       {:scale 2}))]
             :cell-defaults {:pad 1}}))
