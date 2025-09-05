(ns cdq.render.check-open-debug-data
  (:require [cdq.dev.data-view :as data-view]
            [cdq.input :as input]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [cdq.grid :as grid]))

; TODO also items/skills/mouseover-actors
; -> can separate function get-mouseover-item-for-debug (@ ctx)

(defn- open-debug-data-window!
  [{:keys [ctx/stage
           ctx/mouseover-eid
           ctx/grid
           ctx/world-mouse-position]}]
  (let [data (or (and mouseover-eid @mouseover-eid)
                 @(grid/cell grid
                             (mapv int world-mouse-position)))]
    (stage/add! stage
                (data-view/table-view-window {:title "Data View"
                                              :data data
                                              :width 500
                                              :height 500}))))

(defn do!
  [{:keys [ctx/input] :as ctx}]
  (when (input/button-just-pressed? input :right)
    (open-debug-data-window! ctx)))
