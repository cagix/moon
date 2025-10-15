(ns cdq.ui.editor.schema.animation
  (:require [cdq.graphics :as graphics]
            [clojure.scene2d.vis-ui.image-button :as image-button]
            [cdq.ui.build.table :as table]))

(defn create [_ animation {:keys [ctx/graphics]}]
  (table/create
   {:rows [(for [image (:animation/frames animation)]
             {:actor (image-button/create
                      {:drawable/texture-region (graphics/texture-region graphics image)
                       :drawable/scale 2})})]
    :cell-defaults {:pad 1}}))
