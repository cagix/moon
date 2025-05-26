(ns cdq.start
  (:require [cdq.application :as application]
            [gdl.utils :as utils]))

(defn -main [config-path]
  (-> config-path
      utils/create-config
      application/start!))
