(ns ^:no-doc forge.schema.image
  (:require [forge.assets :as assets]
            [forge.db :as db]
            [forge.editor.widget :as widget]
            [forge.ui :as ui]
            [forge.graphics :refer [image sprite sprite-sheet]]))

(defn edn->image [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (sprite (sprite-sheet file tilew tileh)
              [(int (/ sprite-x tilew))
               (int (/ sprite-y tileh))]))
    (image file)))

(defmethod db/edn->value :s/image [_ edn]
  (edn->image edn))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
(defn- texture-rows []
  (for [file (sort (assets/all-textures))]
    [(ui/image-button (image file) (fn []))]
    #_[(ui/text-button file (fn []))]))

(defmethod widget/create :s/image [_ image]
  (ui/image-button (edn->image image)
                   (fn on-clicked [])
                   {:scale 2})
  #_(ui/image-button image
                     #(stage/add! (scrollable-choose-window (texture-rows)))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here
