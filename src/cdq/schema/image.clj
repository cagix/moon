(ns cdq.schema.image
  (:require [cdq.gdx.graphics :as graphics]
            [cdq.schema :as schema]))

(defmethod schema/malli-form :s/image [_ schemas]
  (schema/malli-form [:s/map [:image/file
                              [:image/bounds {:optional true}]]]
                     schemas))

(defn create [schema  image {:keys [ctx/graphics]}]
  {:actor/type :actor.type/image-button
   :drawable/texture-region (graphics/texture-region graphics image)
   :drawable/scale 2}
  #_(ui/image-button image
                     (fn [_actor ctx]
                       (c/add-actor! ctx (scroll-pane/choose-window (texture-rows ctx))))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here


; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
#_(defn- texture-rows [ctx]
    (for [file (sort (assets/all-of-type assets :texture))]
      [(image-button/create {:texture-region (texture/region (assets file))})]
      #_[(text-button/create file
                             (fn [_actor _ctx]))]))
