(ns cdq.tx.set-cursor
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]))

(defn do! [cursor]
  (graphics/set-cursor! ctx/graphics cursor))
