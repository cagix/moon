(ns cdq.start.load-draw-fns
  (:require [cdq.walk :as walk]))

(defn do! [ctx]
  (update ctx :ctx/draw-fns walk/require-resolve-symbols))
