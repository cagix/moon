(ns cdq.ui.editor.widget.image
  (:require [cdq.schema :as schema]
            [cdq.ui.editor.widget :as widget]
            [gdl.ui :as ui]))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
#_(defn- texture-rows [ctx]
    (for [file (sort (c/all-textures ctx))]
      [(ui/image-button (image file)
                        (fn [_actor _ctx]))]
      #_[(ui/text-button file
                         (fn [_actor _ctx]))]))

(defmethod widget/create :s/image [schema image ctx]
  (ui/image-button (schema/edn->value schema image ctx)
                   (fn [_actor _ctx])
                   {:scale 2})
  #_(ui/image-button image
                     (fn [_actor ctx]
                       (c/add-actor! ctx (scroll-pane/choose-window (texture-rows ctx))))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here
