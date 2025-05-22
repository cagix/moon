(ns cdq.application.render)

(def render-fns '[cdq.render.bind-active-entities/do!
                  cdq.render.set-camera-on-player/do!
                  cdq.render.clear-screen/do!
                  cdq.render.draw-tiled-map/do!
                  cdq.render.draw-on-world-viewport/do!
                  cdq.render.draw-ui/do!
                  cdq.render.update-ui/do!
                  cdq.render.player-state-handle-click/do!
                  cdq.render.update-mouseover-entity/do!
                  cdq.render.bind-paused/do!
                  cdq.render.when-not-paused/do!
                  cdq.render.remove-destroyed-entities/do! ; do not pause as pickup item should be destroyed
                  cdq.render.camera-controls/do!])

(defn do! [ctx]
  (reduce (fn [ctx render-fn]
            (if-let [result ((requiring-resolve render-fn) ctx)]
              result
              ctx))
          ctx
          render-fns))
