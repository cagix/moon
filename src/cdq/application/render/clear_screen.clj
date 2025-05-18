(ns cdq.application.render.clear-screen
  (:require [gdl.graphics :as graphics]))

(defn do! []
  (graphics/clear-screen!))
