(ns cdq.render.camera-controls
  (:require [cdq.g :as g]
            [gdl.input :as input]))

(defn do! [{:keys [ctx/config] :as ctx}]
  (when (input/key-pressed? (get (:controls config) :zoom-in))
    (g/inc-zoom! ctx (:zoom-speed config)))
  (when (input/key-pressed? (get (:controls config) :zoom-out))
    (g/inc-zoom! ctx (- (:zoom-speed config))))
  nil)
