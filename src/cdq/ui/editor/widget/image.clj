(ns cdq.ui.editor.widget.image
  (:require [cdq.textures :as textures]
            [cdq.gdx.ui :as ui]))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
#_(defn- texture-rows [ctx]
    (for [file (sort (assets/all-of-type assets :texture))]
      [(ui/image-button {:texture-region (texture/region (assets file))})]
      #_[(ui/text-button file
                         (fn [_actor _ctx]))]))

(defn create [schema  _attribute image {:keys [ctx/graphics]}]
  (ui/image-button {:texture-region (textures/image->texture-region (:textures graphics) image)
                    :scale 2})
  #_(ui/image-button image
                     (fn [_actor ctx]
                       (c/add-actor! ctx (scroll-pane/choose-window (texture-rows ctx))))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here
