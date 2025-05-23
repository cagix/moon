(ns cdq.render.camera-controls
  (:require [cdq.g :as g]))

(defn do! [ctx]
  (let [controls (g/config ctx :controls)
        zoom-speed (g/config ctx :zoom-speed)]
    (when (g/key-pressed? ctx (:zoom-in controls))  (g/inc-zoom! ctx    zoom-speed))
    (when (g/key-pressed? ctx (:zoom-out controls)) (g/inc-zoom! ctx (- zoom-speed))))
  nil)
