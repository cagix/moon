(ns ^:no-doc anvil.entity.line-render
  (:require [anvil.component :as component]
            [clojure.utils :refer [defmethods]]
            [gdl.context :as c]))

(defmethods :entity/line-render
  (component/render-default [[_ {:keys [thick? end color]}] entity c]
    (let [position (:position entity)]
      (if thick?
        (c/with-line-width c 4
          #(c/line c position end color))
        (c/line c position end color)))))
