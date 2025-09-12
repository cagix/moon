(ns cdq.start.entity-states
  (:require [cdq.walk :as walk]))

(defn do! [ctx]
  (update ctx :ctx/entity-states walk/require-resolve-symbols))
