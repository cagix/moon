(ns cdq.application.dispose
  (:require cdq.utils))

(defn context [context]
  (doseq [[k value] context
          :when (cdq.utils/disposable? value)]
    ;(println "Disposing " k " - " value)
    (cdq.utils/dispose value)))
