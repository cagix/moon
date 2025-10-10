(ns cdq.game.render)

(defn do! [ctx steps]
  (reduce (fn [ctx f]
            ((requiring-resolve f) ctx))
          ctx
          steps))
