(ns moon.schema.image
  (:require [gdl.ui :as ui]
            [moon.assets :as assets]
            [moon.graphics :as g]
            [moon.schema :as schema])
  (:import (com.badlogic.gdx.graphics Texture)))

(defmethod schema/form :s/image [_]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(defmethod schema/edn->value :s/image [_ image]
  (g/edn->image image))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
(defn- texture-rows []
  (for [file (sort (assets/all-of-class Texture))]
    [(ui/image-button (g/image file) (fn []))]
    #_[(ui/text-button file (fn []))]))

(defmethod schema/widget :s/image [_ image]
  (ui/image-button (g/edn->image image)
                   (fn on-clicked [])
                   {:scale 2})
  #_(ui/image-button image
                     #(stage/add! (scrollable-choose-window (texture-rows)))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here
