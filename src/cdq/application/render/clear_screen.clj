(ns cdq.application.render.clear-screen
  (:require [gdl.graphics :as graphics]))

(defn do! [_ctx]
  (graphics/clear-screen!))
