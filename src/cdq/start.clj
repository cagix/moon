(ns cdq.start
  (:require [cdq.utils]))

(defn invoc [[f params]]
  (println "invoc ")
  (println f)
  (println params)
  ; TODO PASSED A NAMESPACE 'gdl.start' instead of 'gdl.start/start!'
  ; and just nothing happened no complaints
  ; => for this implicit complicated shit write tests...
  (f params))

(defn exec! [exec]
  (println "exec! " exec)
  (run! invoc exec))

(defn -main [config-path]
  (println "config-path: " config-path)
  (-> config-path
      cdq.utils/io-slurp-edn-req ; <- require-resolved in _this_ namespace ?? can I use 'invoc' ??
      exec!))

(defn when* [[ev cond* exec]]
  (println "when*")
  (println "(ev): " (ev))
  (println "condition true? " (= (ev) cond*))
  (when (= (ev) cond*)
    (println "INVOCE EXECS: " exec)
    (exec! exec)))
