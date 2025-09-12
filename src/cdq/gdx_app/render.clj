(ns cdq.gdx-app.render)

(defn do!
  [ctx]
  (reduce (fn [ctx f]
            (if-let [new-ctx (f ctx)]
              new-ctx
              ctx))
          ctx
          (:cdq.gdx-app.render (:ctx/config ctx))))
