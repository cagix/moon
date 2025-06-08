(ns cdq.ui.editor.widget.image
  (:require [cdq.ui.editor.widget :as widget]
            [gdl.graphics :as graphics]
            [gdl.ui :as ui]))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
#_(defn- texture-rows [ctx]
    (for [file (sort (assets/all-of-type assets :texture))]
      [(ui/image-button (texture/region (assets file))
                        (fn [_actor _ctx]))]
      #_[(ui/text-button file
                         (fn [_actor _ctx]))]))

(defmethod widget/create :widget/image [schema  _attribute image {:keys [ctx/graphics]}]
  (ui/image-button (graphics/image->texture-region graphics image)
                   (fn [_actor _ctx])
                   {:scale 2})
  #_(ui/image-button image
                     (fn [_actor ctx]
                       (c/add-actor! ctx (scroll-pane/choose-window (texture-rows ctx))))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here
