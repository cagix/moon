(ns cdq.entity.line-render
  (:require [gdl.context :as c]))

(defn render-default [[_ {:keys [thick? end color]}] entity c]
  (let [position (:position entity)]
    (if thick?
      (c/with-line-width c 4
        #(c/line c position end color))
      (c/line c position end color))))
