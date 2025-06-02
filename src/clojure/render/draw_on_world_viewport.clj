(ns clojure.render.draw-on-world-viewport
  (:require [clojure.graphics.batch :as batch]
            [clojure.graphics.color :as color]
            [clojure.graphics.camera :as camera]
            [clojure.graphics.shape-drawer :as sd]))

(def draw-fns
  '[
    clojure.render.draw-on-world-viewport.draw-tile-grid/do!
    clojure.render.draw-on-world-viewport.draw-cell-debug/do!
    clojure.render.draw-on-world-viewport.render-entities/do!
    ; clojure.render.draw-on-world-viewport.geom-test/do!
    clojure.render.draw-on-world-viewport.highlight-mouseover-tile/do!
    ])

(defn do! [{:keys [ctx/batch
                   ctx/world-viewport
                   ctx/shape-drawer
                   ctx/world-unit-scale
                   ctx/unit-scale]
            :as ctx}]
  (let [draw-fns (map requiring-resolve draw-fns)]
    (batch/set-color! batch color/white) ; fix scene2d.ui.tooltip flickering
    (batch/set-projection-matrix! batch (camera/combined (:camera world-viewport)))
    (batch/begin! batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (fn []
        (reset! unit-scale world-unit-scale)
        (doseq [f draw-fns]
          (f ctx))
        (reset! unit-scale 1)))
    (batch/end! batch))
  ctx)
