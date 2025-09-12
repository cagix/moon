(ns cdq.start.info
  (:require [cdq.walk :as walk]))

(defn do! [ctx]
  (update ctx :ctx/info walk/require-resolve-symbols))
