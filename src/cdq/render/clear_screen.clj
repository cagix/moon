(ns cdq.render.clear-screen
  (:require [cdq.graphics :as graphics]))

(defn do! [ctx]
  (graphics/clear-screen! ctx)
  ctx)
