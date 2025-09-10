(ns cdq.gdx-app.create)

(defn do! [ctx]
  (reduce (fn [ctx f]
            (f ctx))
          ctx
          (map (fn [sym]
                 (let [var (requiring-resolve sym)]
                   (assert var sym)
                   var))
               (:cdq.gdx-app.create (:ctx/config ctx)))))
