(ns cdq.gdx-app.create)

(defn do! [ctx create-fns]
  (reduce (fn [ctx f]
            (f ctx))
          ctx
          create-fns))
