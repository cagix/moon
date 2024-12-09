(ns forge.entity.line-render
  (:require [anvil.graphics :as g]))

(defn render-default [[_ {:keys [thick? end color]}] entity]
  (let [position (:position entity)]
    (if thick?
      (g/with-line-width 4
        #(g/line position end color))
      (g/line position end color))))
