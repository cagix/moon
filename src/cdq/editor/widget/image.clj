(ns cdq.editor.widget.image
  (:require [cdq.textures :as textures]
            [cdq.ui.image-button :as image-button]))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
#_(defn- texture-rows [ctx]
    (for [file (sort (assets/all-of-type assets :texture))]
      [(image-button/create {:texture-region (texture/region (assets file))})]
      #_[(text-button/create file
                             (fn [_actor _ctx]))]))

(defn create [schema  _attribute image {:keys [ctx/textures]}]
  (image-button/create {:texture-region (textures/image->texture-region textures image)
                        :scale 2})
  #_(ui/image-button image
                     (fn [_actor ctx]
                       (c/add-actor! ctx (scroll-pane/choose-window (texture-rows ctx))))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here
