(ns cdq.render.dissoc-frame-keys)

(defn do!
  [ctx]
  (dissoc ctx
          :ctx/mouseover-actor
          :ctx/ui-mouse-position
          :ctx/world-mouse-position
          :ctx/interaction-state))
