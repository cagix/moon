(ns cdq.render.when-not-paused)

(defn render [context]
  (if (:cdq.context/paused? context)
    context
    (reduce (fn [context f]
              (clojure.utils/req-resolve-call f context))
            context
            '[(cdq.render.when-not-paused.update-time/render)
              (cdq.render.when-not-paused.update-potential-fields/render)
              (cdq.render.when-not-paused.tick-entities/render)])))
