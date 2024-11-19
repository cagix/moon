(ns ^:no-doc moon.schema.image
  (:require [gdl.assets :as assets]
            [moon.schema :as schema]
            [gdl.ui :as ui]
            [moon.app :refer [asset-manager image sprite sprite-sheet]])
  (:import (com.badlogic.gdx.graphics Texture)))

(defmethod schema/form :s/image [_]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(defn edn->image [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (sprite (sprite-sheet file tilew tileh)
              [(int (/ sprite-x tilew))
               (int (/ sprite-y tileh))]))
    (image file)))

(defmethod schema/edn->value :s/image [_ edn]
  (edn->image edn))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
(defn- texture-rows []
  (for [file (sort (assets/all-of-class asset-manager Texture))]
    [(ui/image-button (image file) (fn []))]
    #_[(ui/text-button file (fn []))]))

(defmethod schema/widget :s/image [_ image]
  (ui/image-button (edn->image image)
                   (fn on-clicked [])
                   {:scale 2})
  #_(ui/image-button image
                     #(stage/add! (scrollable-choose-window (texture-rows)))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here
