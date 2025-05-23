(ns cdq.render.camera-controls
  (:require [cdq.g :as g]))

(defn do! [{:keys [ctx/config] :as ctx}]
  (when (g/key-pressed? ctx (get (:controls config) :zoom-in))
    (g/inc-zoom! ctx (:zoom-speed config)))
  (when (g/key-pressed? ctx (get (:controls config) :zoom-out))
    (g/inc-zoom! ctx (- (:zoom-speed config))))
  nil)
