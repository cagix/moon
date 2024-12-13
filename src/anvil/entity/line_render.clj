(ns anvil.entity.line-render
  (:require [anvil.component :as component]
            [gdl.graphics :as g]
            [gdl.utils :refer [defmethods]]))

(defmethods :entity/line-render
  (component/render-default [[_ {:keys [thick? end color]}] entity]
    (let [position (:position entity)]
      (if thick?
        (g/with-line-width 4
          #(g/line position end color))
        (g/line position end color)))))
