(ns cdq.game.create)

(defn pipeline [ctx steps]
  (reduce (fn [ctx [f & params]]
            (apply (requiring-resolve f) ctx params))
          ctx
          steps))

(defn do! [steps]
  (pipeline {}
            steps))
