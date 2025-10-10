(ns cdq.ui.editor.schema.animation
  (:require [cdq.graphics.textures :as textures]
            [clojure.scene2d.vis-ui.image-button :as image-button]
            [clojure.scene2d.build.table :as table]))

(defn create [_ animation {:keys [ctx/graphics]}]
  (table/create
   {:rows [(for [image (:animation/frames animation)]
             {:actor (image-button/create
                      {:drawable/texture-region (textures/texture-region graphics image)
                       :drawable/scale 2})})]
    :cell-defaults {:pad 1}}))
