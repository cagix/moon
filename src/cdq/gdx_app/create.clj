(ns cdq.gdx-app.create)

(defn do! [ctx]
  (reduce (fn [ctx f]
            (f ctx))
          ctx
          (:cdq.gdx-app.create (:ctx/config ctx))))
