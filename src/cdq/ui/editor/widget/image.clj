(ns cdq.ui.editor.widget.image
  (:require [cdq.schema :as schema]
            [cdq.ui.editor.widget :as widget]
            #_[gdl.assets :as assets]
            [gdl.ui :as ui]))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
#_(defn- texture-rows [assets]
    (for [file (sort (assets/all-of-type assets :texture))]
      [(ui/image-button (image file)
                        (fn [_actor _ctx]))]
      #_[(ui/text-button file
                         (fn [_actor _ctx]))]))

(defmethod widget/create :s/image [schema image ctx]
  (ui/image-button (schema/edn->value schema image ctx)
                   (fn [_actor _ctx])
                   {:scale 2})
  #_(ui/image-button image
                     (fn [_actor _ctx]
                       (ui/add! ctx/stage (scroll-pane/choose-window (texture-rows ctx/assets))))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here
