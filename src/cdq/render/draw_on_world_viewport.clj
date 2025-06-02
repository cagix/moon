(ns cdq.render.draw-on-world-viewport
  (:require [clojure.gdx.graphics.camera :as camera]
            [clojure.graphics.shape-drawer :as sd])
  (:import (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.graphics.g2d Batch)))

(def draw-fns
  '[
    cdq.render.draw-on-world-viewport.draw-tile-grid/do!
    cdq.render.draw-on-world-viewport.draw-cell-debug/do!
    cdq.render.draw-on-world-viewport.render-entities/do!
    ; cdq.render.draw-on-world-viewport.geom-test/do!
    cdq.render.draw-on-world-viewport.highlight-mouseover-tile/do!
    ])

(defn do! [{:keys [^Batch ctx/batch
                   ctx/world-viewport
                   ctx/shape-drawer
                   ctx/world-unit-scale
                   ctx/unit-scale]
            :as ctx}]
  (let [draw-fns (map requiring-resolve draw-fns)]
    (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix batch (camera/combined (:camera world-viewport)))
    (.begin batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (fn []
        (reset! unit-scale world-unit-scale)
        (doseq [f draw-fns]
          (f ctx))
        (reset! unit-scale 1)))
    (.end batch))
  ctx)
