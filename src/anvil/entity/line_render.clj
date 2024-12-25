(ns ^:no-doc anvil.entity.line-render
  (:require [anvil.component :as component]
            [gdl.context :as c]))

(defmethods :entity/line-render
  (component/render-default [[_ {:keys [thick? end color]}] entity]
    (let [position (:position entity)]
      (if thick?
        (c/with-line-width 4
          #(c/line position end color))
        (c/line position end color)))))
