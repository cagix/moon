(ns ^:no-doc anvil.entity.line-render
  (:require [anvil.entity :as entity]
            [clojure.component :refer [defcomponent]]
            [gdl.context :as c]))

(defcomponent :entity/line-render
  (entity/render-default [[_ {:keys [thick? end color]}] entity c]
    (let [position (:position entity)]
      (if thick?
        (c/with-line-width c 4
          #(c/line c position end color))
        (c/line c position end color)))))
