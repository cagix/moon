(ns cdq.context.level
  (:require [gdl.context :as c]
            [anvil.level :refer [generate-level]]))

(defn create [[_ world-id] c]
  (generate-level c (c/build c world-id)))
