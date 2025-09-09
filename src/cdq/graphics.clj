(ns cdq.graphics
  (:require [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.g2d.batch :as batch]))

(defn draw-on-world-viewport!
  [{:keys [ctx/batch
           ctx/shape-drawer
           ctx/unit-scale
           ctx/world-unit-scale
           ctx/world-viewport]}
   draw!]
  ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
  ; -> also Widgets, etc. ? check.
  (batch/set-color! batch color/white)
  (batch/set-projection-matrix! batch (:camera/combined (:viewport/camera world-viewport)))
  (batch/begin! batch)
  (sd/with-line-width shape-drawer world-unit-scale
    (fn []
      (reset! unit-scale world-unit-scale)
      (draw!)
      (reset! unit-scale 1)))
  (batch/end! batch))

(declare handle-draws!)

(require 'cdq.effects)

(def ^:private draw-fns
  (cdq.effects/walk-method-map
   '{:draw/with-line-width cdq.draw.with-line-width/do!
     :draw/grid cdq.draw.grid/do!
     :draw/texture-region cdq.draw.texture-region/do!
     :draw/text cdq.draw.text/do!
     :draw/ellipse cdq.draw.ellipse/do!
     :draw/filled-ellipse cdq.draw.filled-ellipse/do!
     :draw/circle cdq.draw.circle/do!
     :draw/filled-circle cdq.draw.filled-circle/do!
     :draw/rectangle cdq.draw.rectangle/do!
     :draw/filled-rectangle cdq.draw.filled-rectangle/do!
     :draw/arc cdq.draw.arc/do!
     :draw/sector cdq.draw.sector/do!
     :draw/line cdq.draw.line/do!}))

(defn- draw!
  [{k 0 :as component} ctx]
  ((draw-fns k) component ctx))

(defn handle-draws! [ctx draws]
  (doseq [component draws
          :when component]
    (draw! component ctx)))

(def zoom-level (comp :camera/zoom :viewport/camera :ctx/world-viewport))
