(ns clojure.scenes.scenes2d.stage)

(defprotocol Stage
  (act! [stage])
  (draw! [stage])
  (add! [stage actor])
  (clear! [stage])
  (root [stage])
  (hit [stage [x y]])
  (viewport [stage]))
