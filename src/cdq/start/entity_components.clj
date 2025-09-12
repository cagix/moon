(ns cdq.start.entity-components
  (:require [cdq.walk :as walk]))

(defn do! [ctx]
  (update ctx :ctx/entity-components walk/require-resolve-symbols))
