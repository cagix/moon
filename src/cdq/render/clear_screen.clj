(ns cdq.render.clear-screen
  (:require [cdq.g :as g]))

(defn do! [c]
  (g/clear-screen! c)
  c)
