(ns cdq.graphics
  (:require [cdq.effects]))

(declare handle-draws!)

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

(defn handle-draws! [ctx draws]
  (doseq [{k 0 :as component} draws
          :when component
          :let [[k] component]]
    ((draw-fns k) component ctx)))
