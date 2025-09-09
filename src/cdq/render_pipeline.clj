(ns cdq.render-pipeline)

(defn dissoc-mouseover-keys
  [ctx]
  (dissoc ctx
          :ctx/mouseover-actor
          :ctx/ui-mouse-position
          :ctx/world-mouse-position))

(defn do!
  [ctx]
  (reduce (fn [ctx sym]
            (let [f (requiring-resolve sym)]
              (assert f (str "cannot find var with sym " sym))
              (if-let [new-ctx (f ctx)]
                new-ctx
                ctx)))
          ctx
          '[cdq.render.validate/do!
            cdq.render.update-mouse/do!
            cdq.render.update-mouseover-eid/do!
            cdq.render.check-open-debug/do!
            cdq.render.assoc-active-entities/do!
            cdq.render.set-camera-on-player/do!
            cdq.render.clear-screen/do!
            cdq.render.draw-world-map/do!
            cdq.render.draw-on-world-viewport/do!
            cdq.render.stage/do!
            cdq.render.assoc-interaction-state/do!
            cdq.render.set-cursor/do!
            cdq.render.player-state-handle-input/do!
            cdq.render.assoc-paused/do!
            cdq.render.update-time/do!
            cdq.render.update-potential-fields/do!
            cdq.render.tick-entities/do!
            cdq.render.remove-destroyed-entities/do! ; do not pause as pickup item should be destroyed
            cdq.render.handle-key-input/do!
            cdq.render-pipeline/dissoc-mouseover-keys
            cdq.render.validate/do!]))
