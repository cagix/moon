(ns ^:no-doc anvil.entity.line-render
  (:require [anvil.component :as component]
            [gdl.context :as c]))

(defmethods :entity/line-render
  (component/render-default [[_ {:keys [thick? end color]}] entity]
    (let [c (c/get-ctx)
          position (:position entity)]
      (if thick?
        (c/with-line-width c 4
          #(c/line c position end color))
        (c/line c position end color)))))
