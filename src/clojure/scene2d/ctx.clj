(ns clojure.scene2d.ctx)

(defprotocol Graphics
  (draw! [_ draws]))
