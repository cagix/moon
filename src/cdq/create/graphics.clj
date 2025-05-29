(ns cdq.create.graphics
  (:require [cdq.graphics :as graphics]))

(defn do! [ctx]
  (assoc ctx :ctx/graphics (graphics/create ctx)))
