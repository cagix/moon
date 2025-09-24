(ns cdq.application.render.check-open-debug
  (:require [cdq.input :as input]
            [cdq.ui.widget :as widget]
            [clojure.scene2d.stage :as stage]))

; TODO also items/skills/mouseover-actors
; -> can separate function get-mouseover-item-for-debug (@ ctx)
(defn do!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (input/open-debug-button-pressed? input)
    (let [data (or (and (:world/mouseover-eid world) @(:world/mouseover-eid world))
                   @((:world/grid world) (mapv int (:graphics/world-mouse-position graphics))))]
      (stage/add! stage (widget/data-viewer
                         {:title "Data View"
                          :data data
                          :width 500
                          :height 500}))))
  ctx)
