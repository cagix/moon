(ns cdq.start
  (:require [gdl.application :as application]
            [gdl.utils :as utils]))

(defn -main [config-path]
  (-> config-path
      utils/create-config
      application/start!))
