(ns cdq.render.draw-on-world-viewport
  (:require [cdq.graphics :as graphics]
            [cdq.graphics]))

(defn do!
  [{:keys [ctx/graphics]
    :as ctx}
   draw-fns]
  (cdq.graphics/draw-on-world-viewport!
   graphics
   (fn []
     (doseq [[f & params] draw-fns]
       (graphics/handle-draws! graphics (apply f ctx params)))))
  ctx)
