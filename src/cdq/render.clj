(ns cdq.render
  (:require [cdq.g :as g]))

(def render-fns
  '[
    cdq.render.assoc-active-entities/do!
    cdq.render.set-camera-on-player/do!
    cdq.render.clear-screen/do!
    cdq.render.draw-world-map/do!
    cdq.render.draw-on-world-viewport/do!
    cdq.g/render-stage!
    cdq.render.player-state-handle-click/do!
    cdq.render.update-mouseover-entity/do!
    cdq.render.assoc-paused/do!
    cdq.render.update-time/do!
    cdq.render.update-potential-fields/do!
    cdq.render.tick-entities/do!
    cdq.render.remove-destroyed-entities/do!
    cdq.render.camera-controls/do!
    ])

(defn do! [ctx]
  (g/validate-humanize ctx)
  (let [render-fns (map requiring-resolve render-fns)
        ctx (reduce (fn [ctx render!]
                      (render! ctx))
                    ctx
                    render-fns)]
    (g/validate-humanize ctx)
    ctx))
