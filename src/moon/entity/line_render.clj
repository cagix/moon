(ns moon.entity.line-render
  (:require [gdl.graphics.shape-drawer :as sd]))

(defn render [{:keys [thick? end color]} entity]
  (let [position (:position entity)]
    (if thick?
      (sd/with-line-width 4 #(sd/line position end color))
      (sd/line position end color))))
