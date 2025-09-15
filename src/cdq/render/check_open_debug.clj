(ns cdq.render.check-open-debug
  (:require [cdq.ui.widget :as widget]
            [clojure.gdx.input :as input]
            [clojure.scene2d.stage :as stage]))

; TODO also items/skills/mouseover-actors
; -> can separate function get-mouseover-item-for-debug (@ ctx)

(defn- open-debug-data-window!
  [{:keys [ctx/stage
           ctx/mouseover-eid
           ctx/world
           ctx/world-mouse-position]}]
  (let [data (or (and mouseover-eid @mouseover-eid)
                 @((:world/grid world) (mapv int world-mouse-position)))]
    (stage/add! stage (widget/data-viewer
                       {:title "Data View"
                        :data data
                        :width 500
                        :height 500}))))

(defn do!
  [{:keys [ctx/input] :as ctx}]
  (when (input/button-just-pressed? input :right)
    (open-debug-data-window! ctx))
  ctx)
