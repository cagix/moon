(ns cdq.start
  (:require [cdq.utils]
            [gdl.start]))

(defn -main [config-path]
  (-> config-path
      cdq.utils/load-edn-config
      gdl.start/start!))
