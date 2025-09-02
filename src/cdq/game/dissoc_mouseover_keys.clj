(ns cdq.game.dissoc-mouseover-keys)

(defn do!
  [ctx]
  (dissoc ctx
          :ctx/mouseover-actor
          :ctx/ui-mouse-position
          :ctx/world-mouse-position))
