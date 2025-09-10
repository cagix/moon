(ns cdq.render-pipeline)

(defn do!
  [ctx]
  (reduce (fn [ctx sym]
            (let [f (requiring-resolve sym)]
              (assert f (str "cannot find var with sym " sym))
              (if-let [new-ctx (f ctx)]
                new-ctx
                ctx)))
          ctx
          (:cdq.render.pipeline (:ctx/config ctx))))
