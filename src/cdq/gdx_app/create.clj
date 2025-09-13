(ns cdq.gdx-app.create)

(defn do! [ctx gdx]
  (reduce (fn [ctx f]
            (f ctx))
          (assoc ctx :ctx/gdx gdx)
          (:cdq.gdx-app.create (:ctx/config ctx))))
