(ns cdq.render.when-not-paused
  (:require cdq.render.when-not-paused.update-time
            cdq.render.when-not-paused.update-potential-fields
            cdq.render.when-not-paused.tick-entities))

(defn render [context]
  (if (:cdq.context/paused? context)
    context
    (reduce (fn [context f] (f context))
            context
            [cdq.render.when-not-paused.update-time/render
             cdq.render.when-not-paused.update-potential-fields/render
             cdq.render.when-not-paused.tick-entities/render])))
